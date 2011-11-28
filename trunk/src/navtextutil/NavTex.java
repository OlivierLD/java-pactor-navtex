package navtextutil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.ArrayList;

public class NavTex
{
  public final static char NUL = 0x00;
  public final static char SOH = 0x01; // Start of Heading
  public final static char STX = 0x02; // Start of Text
  public final static char ETX = 0x03; // End of Text
  public final static char EOT = 0x04; // End of Transmission
  public final static char ENQ = 0x05; // Enquiry
  public final static char ACK = 0x06; // Acknowledge 
  public final static char BEL = 0x07; // Bell
  public final static char BS  = 0x08; // Backspace
  public final static char HT  = 0x09; // Horizontal Tab
  public final static char NL  = 0x0A; // New Line
  public final static char VT  = 0x0B; // Vertical Tab
  public final static char NP  = 0x0C; // New Page
  public final static char CR  = 0x0D; // Carriage Return 
  public final static char SO  = 0x0E; // Shift Out
  public final static char SI  = 0x0F; // Shift In
  public final static char DLE = 0x10; // Data Link Escape
  public final static char DC1 = 0x11; // Device Control 1
  public final static char DC2 = 0x12; // Device Control 2
  public final static char DC3 = 0x13; // Device Control 3
  public final static char DC4 = 0x14; // Device Control 4
  public final static char NAK = 0x15; // Negative Acknowledge
  public final static char SYN = 0x16; // Synchronous Idle
  public final static char ETB = 0x17; // End of Transmission Block
  public final static char CAN = 0x18; // Cancel
  public final static char EM  = 0x19; // End of Medium
  public final static char SUB = 0x1A; // Subsitute
  public final static char ESC = 0x1B; // Escape
  public final static char FS  = 0x1C; // File Separator
  public final static char GS  = 0x1D; // Group Separator
  public final static char RS  = 0x1E; // Record Separator
  public final static char US  = 0x1F; // Unit Separator
  public final static char SP  = 0x20; // Space
  public final static char EXCLAM           = 0x21; // (!)
  public final static char DQUOTE           = 0x22; // (")
  public final static char HASH             = 0x23; // (#)
  public final static char DOLLAR           = 0x24; // ($)
  public final static char PERCENT          = 0x25; // (%)
  public final static char AMP              = 0x26; // (&)
  public final static char SQUOTE           = 0x27; // (')
  public final static char OPEN_BRACKET     = 0x28; // (()
  public final static char CLOSE_BRACKET    = 0x29; // ())
  public final static char ASTER            = 0x2A; // (*)
  public final static char PLUS             = 0x2B; // (+)
  public final static char COMMA            = 0x2C; // (,)
  public final static char MINUS            = 0x2D; // (-)
  public final static char DOT              = 0x2E; // (.)
  public final static char SLASH            = 0x2F; // (/)
  public final static char ZERO             = 0x30; // (0)
  public final static char ONE              = 0x31; // (1)
  public final static char TWO              = 0x32; // (2)
  public final static char THREE            = 0x33; // (3)
  public final static char FOUR             = 0x34; // (4)
  public final static char FIVE             = 0x35; // (5)
  public final static char SIX              = 0x36; // (6)
  public final static char SEVEN            = 0x37; // (7)
  public final static char EIGHT            = 0x38; // (8)
  public final static char NINE             = 0x39; // (9)
  public final static char COLON            = 0x3A; // (:)
  public final static char SEMI_COLON       = 0x3B; // (;)
  public final static char LT               = 0x3C; // (<)
  public final static char EQU              = 0x3D; // (=)
  public final static char GT               = 0x3E; // (>)
  public final static char QUESTION         = 0x3F; // (?)
  public final static char AT               = 0x40; // (@)
  public final static char A = 0x41; // (A)
  public final static char B = 0x42; // (B)
  public final static char C = 0x43; // (C)
  public final static char D = 0x44; // (D)
  public final static char E = 0x45; // (E)
  public final static char F = 0x46; // (F)
  public final static char G = 0x47; // (G)
  public final static char H = 0x48; // (H)
  public final static char I = 0x49; // (I)
  public final static char J = 0x4A; // (J)
  public final static char K = 0x4B; // (K)
  public final static char L = 0x4C; // (L)
  public final static char M = 0x4D; // (M)
  public final static char N = 0x4E; // (N)
  public final static char O = 0x4F; // (O)
  public final static char P = 0x50; // (P)
  public final static char Q = 0x51; // (Q)
  public final static char R = 0x52; // (R)
  public final static char S = 0x53; // (S)
  public final static char T = 0x54; // (T)
  public final static char U = 0x55; // (U)
  public final static char V = 0x56; // (V)
  public final static char W = 0x57; // (W)
  public final static char X = 0x58; // (X)
  public final static char Y = 0x59; // (Y)
  public final static char Z = 0x5A; // (Z)
  public final static char OPEN_SQ_BRACK    = 0x5B; // ([)
  public final static char BACK_SLASH       = 0x5C; // (\)
  public final static char CLOSE_SQ_BRACK   = 0x5D; // (])
  public final static char CARET            = 0x5E; // (^)
  public final static char UNDERSCR         = 0x5F; // (_)
  public final static char BACK_QUOTE       = 0x60; // (`)
  public final static char a = 0x61; // (a)
  public final static char b = 0x62; // (b)
  public final static char c = 0x63; // (c)
  public final static char d = 0x64; // (d)
  public final static char e = 0x65; // (e)
  public final static char f = 0x66; // (f)
  public final static char g = 0x67; // (g)
  public final static char h = 0x68; // (h)
  public final static char i = 0x69; // (i)
  public final static char j = 0x6A; // (j)
  public final static char k = 0x6B; // (k)
  public final static char l = 0x6C; // (l)
  public final static char m = 0x6D; // (m)
  public final static char n = 0x6E; // (n)
  public final static char o = 0x6F; // (o)
  public final static char p = 0x70; // (p)
  public final static char q = 0x71; // (q)
  public final static char r = 0x72; // (r)
  public final static char s = 0x73; // (s)
  public final static char t = 0x74; // (t)
  public final static char u = 0x75; // (u)
  public final static char v = 0x76; // (v)
  public final static char w = 0x77; // (w)
  public final static char x = 0x78; // (x)
  public final static char y = 0x79; // (y)
  public final static char z = 0x7A; // (z)
  public final static char OPEN_CURL_BRACE  = 0x7B; // ({)
  public final static char PIPE             = 0x7C; // (|)
  public final static char CLOSE_CURL_BRACE = 0x7D; // (})
  public final static char TILDA            = 0x7E; // (~)
  public final static char DEL              = 0x7F; // Delete
  
  public final static String MESSAGE_HEADER = "ZCZC";
  public final static String MESSAGE_FOOTER = "NNNN";
    
  public static byte[] decodeNavtex(byte[] ba)
  {
    byte[] decoded = null;
    ArrayList<Byte> alb = new ArrayList<Byte>();
    for (int i=0; i<ba.length; i++)
    {
      if (i<(ba.length - 3) &&  ba[i] == STX && ba[i+2] == SOH)
      {
        byte b = ba[i+1];
//      System.out.println("Adding :" + b);
        alb.add(b);
        i+=2;        
        continue;
      }
//      else if (i<(ba.length - 3) &&  ba[i] == EOT && ba[i+2] == SOH)
//      {
//        byte b = ba[i+1];
//        alb.add(b);
//        alb.add((byte)'\n');
//        i+=2;        
//        continue;
//      }
      else if (i<(ba.length - 2) &&  ba[i] == STX && ba[i+1] == SOH)
      {
        i+=1;        
        continue;
      }
      else if (ba[i] == DC1 || 
               ba[i] == STX)
//      alb.add((byte)'\n');
        continue;
      else if (ba[i] == RS)
        continue;
      else if (ba[i] == EOT || // QRTC
               ba[i] == CR ||
//             ba[i] == SOH || 
               ba[i] == NL )
      {
        alb.add((byte)'\n');
        continue;
      }
      else if (ba[i] > DEL || ba[i] < SP)
        continue;
      else      
        alb.add(ba[i]);
    }    
    decoded = new byte[alb.size()];
    int idx = 0;
    for (Byte b : alb)
      decoded[idx++] = b.byteValue();
    
    return decoded;
  }
  
  /*
   * For tests
   */
  public static void main(String[] args) throws Exception
  {
//  String fName = "log.txt";
//  String fName = "log2.txt";
//  String fName = "log3.txt";
//  String fName = "2011-11-24.4.txt";
//  String fName = "2011-11-25.1.txt";
    String fName = "2011-11-26.1.save.txt";
    
    if (args.length > 0)
      fName = args[0];

    File file = new File(fName);

    BufferedReader br = new BufferedReader(new FileReader(file));
    String line = "";
    boolean keepReading = true;
    while (keepReading)
    {
      line = br.readLine();
      if (line != null)
      {
        byte[] dn = decodeNavtex((line).getBytes());
        String mess = new String(dn);
        if (mess.contains(NavTex.MESSAGE_HEADER))
          System.out.println("---------- New Message >> ------------");
        System.out.print(mess);
        if (mess.contains(NavTex.MESSAGE_FOOTER))
          System.out.println("\n---------- << End Message ------------");
//      try { Thread.sleep(100L); } catch (Exception ignore) { ignore.printStackTrace(); }
      }
      else
        keepReading = false;
    }
    br.close();
  }
  
  public static void main1(String[] args)
  {
    for (int i=0; i<128; i++)
    {
      String n = Integer.toHexString(i);
      while (n.length() < 2)
        n = "0" + n;
      System.out.println("public final static char SP = 0x" + n.toUpperCase() + "; // (" + (char)i + ")") ;
    }
  }
}
