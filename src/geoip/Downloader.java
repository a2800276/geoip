package geoip;

public class Downloader {
  
  static final String [] DEFAULT_LOCATIONS = {
    "ftp://ftp.ripe.net/pub/stats/apnic/delegated-apnic-latest",
    "ftp://ftp.ripe.net/pub/stats/arin/delegated-arin-latest",
  /*  "ftp://ftp.ripe.net/pub/stats/iana/delegated-iana-latest", */
    "ftp://ftp.ripe.net/pub/stats/lacnic/delegated-lacnic-latest",
    "ftp://ftp.ripe.net/pub/stats/ripencc/delegated-ripencc-latest",
  };

  static void p(Object o) {
    System.out.println(o);
  }

  /**
   * Download RIR record from the _default_ location 
   * (pretty arbitrarily chosen to be ftp.ripe.net)
   * and load them into the provided database, which gets
   * clobbered.
   *
   * In case you don't like ripe for some reason, use the
   * `download` method and provide your own list of 
   * urls to download from.
   */
  static boolean defaultDownload(final DB db) {
    return download(DEFAULT_LOCATIONS, db);
  }

  /**
   * see `downloadDefault`
   */
  static boolean download (String [] urls, final DB db) {
    assert(null != db);
    db.clear();
    try {
      RIRStatemachine.RecordCB cb = new RIRStatemachine.RecordCB() {
        public void onRecord(RIRStatemachine.Record r) {
          if ("ipv4".equals(r.type)) {
            db.addRecord(r);
          } else {
            //p(r);
          }
        }
      };
      RIRStatemachine sm = new RIRStatemachine(null, null, cb);
      byte [] bytes = new byte[4092];
      for (String url : urls) {
        p(url);
        java.net.URLConnection con = new java.net.URL(url).openConnection();
        java.io.InputStream     is = con.getInputStream();
        int read = 0;
        while ( -1 != (read = is.read(bytes)) ) {
          sm.parse(bytes, 0, read);  
        }
      }
    } catch (java.io.IOException ioe) {
      ioe.printStackTrace();
      return false;
    }
    return true;
  }

  public static void main (String [] args) throws Throwable {
    DB db = null; // DB.load(DEFAULT_DB_FN);
    if (null == db) {
      db = new DB(Main.DEFAULT_DB_FN);
      boolean success = defaultDownload(db);
      if (!success) {
        System.exit(1);
      }
    }
    db.save();

  }
}
