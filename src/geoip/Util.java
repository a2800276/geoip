package geoip;

public class Util {

  /** 
   * convert ip in dotted decimal to
   * long
   */
  public static long ip4_2_long (String ip4) {
    String [] spl = ip4.split("\\.");
    long l = 0;
         l += (Long.parseLong(spl[0]) << 24);
         l += (Long.parseLong(spl[1]) << 16);
         l += (Long.parseLong(spl[2]) << 8);
         l += (Long.parseLong(spl[3]));
    return l;
  }
  
  /** 
   * convert long to dotted decimal 
   */
  public static String long_2_ip4 (long ip) {
    StringBuilder b = new StringBuilder();
                  b.append ( (ip >> 24) & 0xff);
                  b.append('.');
                  b.append ( (ip >> 16) & 0xff);
                  b.append('.');
                  b.append ( (ip >>  8) & 0xff);
                  b.append('.');
                  b.append ( (ip      ) & 0xff);
    return b.toString();
  }

  public static void main (String [] args) {
    for (String arg : args) {
      long ip = ip4_2_long(arg);
      p(arg +" : "+ip+" : "+long_2_ip4(ip));
      
    }
  }

  static void p(Object o) {
    System.out.println(o);
  }
}
