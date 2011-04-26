package geoip;

import java.io.*;

/**
 * Simple TreeSet wrapper to maintain geoip information
 */
public class DB implements java.io.Serializable {
  static final long serialVersionUID = 0;  
  String baseFileName;

  java.util.TreeSet<RIRStatemachine.Record> treeSet;

  /**
   *  `baseFileName` is where the db is persisted to
   *  when you call `save()`
   */
  public DB (String baseFileName) {
    this.baseFileName = baseFileName;
    this.treeSet      = new java.util.TreeSet<RIRStatemachine.Record>();
  }
  
  /**
   * deletes the database in preparation to be reloaded.
   * you probably don't want to do this, instead, start
   * with a fresh database and swap it around
   */
  void clear () {
    this.treeSet.clear();
  }

  /**
   * more or less internal method to add record while loading the
   * database from the source.
   */
  void addRecord (RIRStatemachine.Record record) {
    assert("ipv4".equals(record.type));
    this.treeSet.add(record);
  }
  
  /**
   * lookup an ip address, which should be provided in
   * standard dotted decimal notation, i.e. '214.12.34.5'
   *
   * returns null if no record matches.
   */
  public RIRStatemachine.Record lookup (String ip) {
    return lookup(Util.ip4_2_long(ip));
  }

  /**
   * lookup an ip address converted to long from dotted
   * decimal notation.
   * returns null if no record matches
   */
  public RIRStatemachine.Record lookup (long ip) {
    RIRStatemachine.Record r = this.treeSet.floor(new RIRStatemachine.Record(ip));
    if (null != r && r.to >= ip) {
      return r;
    }
    return null;
  }
  
  /**
   * writes the database to the filesystem using java serialization
   */
  void save() throws Throwable {
    // if exists baseFileName
      // serialize baseFileName.<ts>
      // mv baseFileName -> baseFileName.bak 
      // mv baseFileName.<ts> -> baseFileName
      // rm baseFileName.bak
    // else
      // serialize baseFileName

    // above is how it's supposed to be, THIS is what it is:

    ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(this.baseFileName));
    os.writeObject(this);
    os.close();
  }

  /**
   *  Tries to load a databse from the given filename. 
   *
   *  Returns null if it fails.
   */
  public static DB load(String fn) {
    // if baseFileName exists?
       // load
       // return true
    // return false
    DB db = null;
    ObjectInputStream is = null;
    try {
      is = new ObjectInputStream(new FileInputStream(fn));
      db = (DB)is.readObject();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } catch (ClassNotFoundException cnfe) {
      cnfe.printStackTrace();
    } finally {
      if (null != is) {
        try { is.close(); } catch (Throwable t) { t.printStackTrace(); }
      }
    }
    return db;

  }
  static void p (Object o) {
    System.out.println(o);
  }
}
