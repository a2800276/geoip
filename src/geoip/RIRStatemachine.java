package geoip;

import java.nio.*;
import java.util.*;

/**
 * sort of the brains behind the whole machinery,
 * is able to parse RIR records.
 */

public class RIRStatemachine {
  /* hah! There's only two states! I'm just showing off
   * calling the thing Statemachine. */
  enum State {
    INITIAL,
    COMMENT
  }

  public interface HeaderCB {
    public void onHeader (Header h);
  }

  public interface SummaryCB {
    public void onSummary (Summary s);
  }

  public interface RecordCB {
    public void onRecord(Record r);
  }
  
  HeaderCB  headerCB;
  SummaryCB summaryCB;
  RecordCB  recordCB;
  
  /**
   * Construct a new parser,
   * the callbacks provided to the constructor will be called
   * for each Header, Summary and Record encountered during
   * parsing, respectively.
   * In case you are not interested in one of the three 
   * record types, you may set the approriate parameter to
   * null.
   */

  public RIRStatemachine(HeaderCB h, SummaryCB s, RecordCB r) {
    this.headerCB = null != h ? h : new HeaderCB() {
      public void onHeader(Header h){}
    };
    this.summaryCB = null != s ? s : new SummaryCB() {
      public void onSummary(Summary s) {}
    };
    this.recordCB = null != r ? r : new RecordCB () {
      public void onRecord(Record r) {}
    };
  }
  
  /**
   * Just interested in the meat
   */
  public RIRStatemachine (RecordCB r) {
    this(null, null, r);
  }

  State state         = State.INITIAL;

  /* just temporary storage for fields and records
   * while the are being parsed. You should not access
   * them.*/
  List<String> rec    = new LinkedList<String>();
  StringBuilder field = new StringBuilder();
  
  final static byte HASH = (byte)'#';
  final static byte PIPE = (byte)'|';
  final static byte NL   = (byte)'\n';
  final static byte CR   = (byte)'\r';

  /**
   * Parse the bytes currently available to you.
   **/
  public void parse (byte [] bytes) {
    parse(ByteBuffer.wrap(bytes));
  }
  /**
   * Parse the bytes currently available to you, with 
   * standard `offset` and `len` params.
   **/
  public void parse (byte [] bytes, int offset, int len) {
    parse(ByteBuffer.wrap(bytes, offset, len));
  }

  /**
   *  parse a `ByteBuffer` if you are nio inclined
   */
  public void parse (ByteBuffer buf) {
    while (buf.position() != buf.limit()) {
    
      byte b = buf.get();
      switch (this.state) {
        case INITIAL:
          switch (b) {
            case HASH:
              this.state = State.COMMENT;
              break;
            case PIPE:
              handleField();
              break;
            case NL:
              handleField();
              handleRecord();
              break;
            case CR:
              break;
            default:
              this.field.append((char)b);
          }
          break;
        case COMMENT:
          switch(b) {
            case NL:
              this.state = State.INITIAL;
              break;
            default:
              break;
          }
          break;
        default:
          throw new RuntimeException("can't happen");
      }
    }  
  }
  /* internal: just passed a PIPE or NL and need to
   * stash the current field content and reset the
   * storage. */
  void handleField(){
    this.rec.add(this.field.toString());
    this.field = new StringBuilder();
  }
  /**
   *  internal, just finished a line, need to determine
   *  what type (Header, Summary, Record) it is and
   *  prepare the callback call.
   **/
  void handleRecord(){
    if (6 == this.rec.size()) {
      Summary s = new Summary();
              s.registry = this.rec.get(0);
              s.type     = this.rec.get(2);
              s.count    = this.rec.get(4);
              s.summary  = this.rec.get(5);
       this.summaryCB.onSummary(s);
    } else if ( 7 <= this.rec.size()) {
      if (1 == this.rec.get(0).length()) {
        Header h = new Header();
               h.version   = this.rec.get(0);
               h.registry  = this.rec.get(1);
               h.serial    = this.rec.get(2);
               h.records   = this.rec.get(3);
               h.startdate = this.rec.get(4);
               h.enddate   = this.rec.get(5);
               h.UTCoffset = this.rec.get(6);
        this.headerCB.onHeader(h);
      } else {
        this.recordCB.onRecord(new Record(this.rec));
      }
    }
    
    this.rec = new LinkedList<String>();
  }

  /**
   *  I'm not interested in headers, so this is just a dumb
   *  holder class.
   **/
  static class Header{
    String version;
    String registry;
    String serial;
    String records;
    String startdate;
    String enddate;
    String UTCoffset;
  }
  /**
   *  I'm not interested in summary lines, so this is just a dumb
   *  holder class.
   **/
  static class Summary {
    String registry;
    String type;
    String count;
    String summary;
  }

  /**
   * Class to represent record lines, maybe will be moved out of
   * this file to be a proper class when it grows up.
   *
   * Implements all those interfaces because it gets stuffed into
   * a TreeSet and is then written to disk.
   */

  static class Record implements Comparable<Record>, java.io.Serializable {
    String registry;
    String cc;
    String type;
    String start;
    String value;
    String date;
    String status;

    long from;
    long to;

    byte [] json;
    
    /**
     * Use this constructor when you are doing a lookup on 
     * the TreeSet, you would be better off just using the
     * `lookup` methods in `DB` though.
     */
    Record (long from) {
      this.from = from;
    }
    /**
     * Construct a Record from a parsed line
     */
    Record (List<String> raw_entry) {
      if (raw_entry.size() >=7){
        if (raw_entry.get(0).length() == 1) {
          throw new RuntimeException("wrong:"+raw_entry);
        }
        this.registry = raw_entry.get(0);
        this.cc       = raw_entry.get(1);
        this.type     = raw_entry.get(2);
        this.start    = raw_entry.get(3);
        this.value    = raw_entry.get(4);
        this.date     = raw_entry.get(5);
        this.status   = raw_entry.get(6);
        
        if ("ipv4".equals(this.type)) {
          this.from     = Util.ip4_2_long(this.start);
          this.to       = (this.from + Long.parseLong(raw_entry.get(4)));
        }

        this.json = toJSON().getBytes();
      } else {
        throw new RuntimeException("wrong:"+raw_entry);
      } 
    }

    public int compareTo(Record other) {
      if (this.from == other.from) {
        return 0;
      } else if (this.from < other.from) {
        return -1;
      } 
      return 1;
    }

    public String toString () {
      StringBuilder b = new StringBuilder();
      b.append("registry: " +this.registry+"\n");
      b.append("cc:       " +this.cc+"\n");
      b.append("type:     " +this.type+"\n");
      b.append("start:    " +this.start+"\n");
      b.append("from:     " +this.from+"\n");
      b.append("to:       " +this.to+"\n");

      return b.toString();
    }

    String toJSON () {
      return String.format("{\"registry\":%s,\"cc\":%s,\"type\":%s,\"start\":%s,\"value\":%s,\"date\":%s,\"status\":%s}\n",
                            q(this.registry),
                            q(this.cc),
                            q(this.type),
                            q(this.start),
                            q(this.value),
                            q(this.date),
                            q(this.status));
    }

    String q(String str) {
      return "\"" + str + "\"";
    }
  }


  static void p(Object o) {
    System.out.println(o);
  }
  
}
