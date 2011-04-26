
import java.util.*;
import java.io.*;

import ioutils.*;
public class Test {

  static List<List<String>> parse (String fn) {
    p("parsing :"+fn);
    IOUtils.Res result = IOUtils.readFile(fn);
    if (result.error) {
      throw new RuntimeException(result.exception);
    }
    List<List<String>> l = do_parse(result.contents);
    return l;
  }

  static List<List<String>> do_parse(String str) {
    char [] chars = str.toCharArray();

    final int INITIAL = 0;
    final int COMMENT = INITIAL+1;
    int STATE = INITIAL;

    List <String>           rec = new LinkedList<String>();
    StringBuilder         field = new StringBuilder();
    List <List<String>> results = new LinkedList<List<String>>();

    for (int i = 0; i!= chars.length; ++i){
      char c = chars[i];
      switch (STATE) {
        case INITIAL:
          switch(c) {
            case '#':
              STATE = COMMENT;
              break;
            case '|':
              rec.add(field.toString());
              field = new StringBuilder();
              break;
            case '\n':
              rec.add(field.toString());
              field = new StringBuilder();
              results.add(rec);
              rec = new LinkedList<String>();
            case '\r':
              break;
            default:
              field.append(c);
          }
        break;
        case COMMENT:
          switch(c){
            case '\n':
              STATE = INITIAL;
              break;
            default:
              break;
          }
          break;
        default:
          throw new RuntimeException("wtf");
      }// switch STATE
    } // for
    return results;
  } // doParse

  static void handle (TreeSet db, List<List<String>> entries){
    for (List<String> entry : entries) {
      if (entry.size() < 7) {
        continue;
      }

      if (entry.get(0).length() == 1) {
        continue;
      }
      if ("ipv4".equals(entry.get(2))){
        db.add(new Entry(entry));
      } else {
        //p(entry);
      
      }
    }
  }

  static long ip4_2_long (String ip4) {
    String [] spl = ip4.split("\\.");
    long l = 0;
         l += (Long.parseLong(spl[0]) << 24);
         l += (Long.parseLong(spl[1]) << 16);
         l += (Long.parseLong(spl[2]) << 8);
         l += (Long.parseLong(spl[3]));

    return l;
  }

  static Entry lookup(TreeSet<Entry> db, long ip) {
    Entry e = db.floor(new Entry(ip));
    //if (null != e) {
    //  p(e);
    //}
    if (null != e && e.to >= ip) {
      return e;
    }
    return null;
  }

  static final String FN = "GEOIP.ser";
  
  static TreeSet<Entry> loadDB() {
    TreeSet<Entry> db = null;
    try {
      ObjectInputStream is = new ObjectInputStream(new FileInputStream(FN));
      db = (TreeSet<Entry>)is.readObject();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return db;
  }
  static void save(TreeSet<Entry> db) throws Throwable {
    ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(FN));
    os.writeObject(db);
    os.close();
  }
  public static void main (String [] args) throws Throwable{
    long start = System.currentTimeMillis();
    TreeSet<Entry> db = loadDB();
    if (null == db) {
      db = new TreeSet<Entry>();
      start = System.currentTimeMillis();

      for (String fn : args) {
        List<List<String>> entries =  parse(fn);
        handle(db, entries);
      }
      p("finished parsing:"+(System.currentTimeMillis()-start)/1000);
      save(db);

    } else {
      p("loaded "+FN+":"+(System.currentTimeMillis()-start));
    }
    long [] tests = {
    3321127546L,
      2867985990L,
      180271324L,
      4181803568L,
      2036388460L,
      193448059L,
      3716705518L,
      2907947483L,
      4051000725L,
      2053612644L,
      };
    start = System.nanoTime();
    for (long l : tests) {
      Entry e = lookup(db, l);
      p(e);
    }
    p(System.nanoTime() - start);

  }

  static void p (Object o) { System.out.println(o);}
  
  static class Entry implements Comparable<Entry>, java.io.Serializable {
    String registry;
    String cc;
    String type;
    String start;

    long from;
    long to;
    
    Entry (long from) {
      this.from = from;
    }
    Entry (List<String> raw_entry) {
      if (raw_entry.size() >=7){
        if (raw_entry.get(0).length() == 1) {
          throw new RuntimeException("wrong:"+raw_entry);
        }
        this.registry = raw_entry.get(0);
        this.cc       = raw_entry.get(1);
        this.type     = raw_entry.get(2);
        this.start    = raw_entry.get(3);

        this.from     = ip4_2_long(this.start);
        this.to       = (this.from + Long.parseLong(raw_entry.get(4)));
      } else {
        throw new RuntimeException("wrong:"+raw_entry);
      } 
    }

    public int compareTo(Entry other) {
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
  }

}
