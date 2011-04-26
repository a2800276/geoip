package geoip;

import java.net.*;

import event.*;

public class Main {

  static final String DEFAULT_DB_FN = "geoip.db";
  static DB db;
  
  /**
   *  for the purposes of "Main" only, lookup and return the
   *  bytes to send back to the client.
   */
  static byte[] lookup (String ip) {
    if (null == db) {
      return "error\n".getBytes();
    }  
    RIRStatemachine.Record rec = db.lookup(ip);
    if (null == rec) {
      return "not found\n".getBytes();
    }
    return rec.json;
  }

  public static void main (String [] args) throws Throwable {
    db = DB.load(DEFAULT_DB_FN);
    if (null == db) {
      db = new DB(DEFAULT_DB_FN);
      boolean success = Downloader.defaultDownload(db);
      if (!success) {
        p("Couldn't download db and none serialized, giving up");
        System.exit(1);
      }
      p("download successful!");
      db.save();
      p("saved");
    } else {
      p("loaded: "+DEFAULT_DB_FN);
    }
    
    ServerCallback      cb = new ServerCallback();
    InetSocketAddress   sa = new InetSocketAddress(8080);

    TCPServerLoop loop = new TCPServerLoop();
                  loop.createTCPServer(cb, sa);
                  loop.run();
  }

  static class ServerCallback extends Callback.TCPServerCB {
    public void onAccept(event.TCPServerLoop loop,
                         java.nio.channels.ServerSocketChannel ssc,
                         java.nio.channels.SocketChannel sc)
    {
      p("new connection: "+ssc+":"+sc);
      loop.createTCPClient(new ClientCallback() ,sc); 
    }
  }
  static final byte NL = (byte)'\n';
  static final byte CR = (byte)'\r';
  static class ClientCallback extends Callback.TCPClientCB {
    StringBuilder builder = new StringBuilder();
    public void onData(event.TCPClientLoop             loop,
                       java.nio.channels.SocketChannel sc,
                       java.nio.ByteBuffer             buf) 
    { 
      while (buf.position() != buf.limit()) {
        byte b = buf.get();
        if (NL == b) {
          byte [] resp = "eRrOr".getBytes();
          try {
            resp = lookup(builder.toString());
          } catch (Throwable t) {
            t.printStackTrace();
            p(sc);
            // close ...?
          }
          loop.write(sc, this, resp); 
          builder = new StringBuilder();
        } else {
          if (CR != b) {
            builder.append((char)b);
          }
        }
      }
    } 
  }
  
  static void p (Object o) {
    System.out.println(o);
  }
}
