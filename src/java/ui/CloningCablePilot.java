package ui;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import serialmodemconsole.AsyncCommunication;
import serialmodemconsole.CablePilotClientInterface;
import serialmodemconsole.SerialModemInterface;

/**
 * Icom will not give away the commands that drive the cloning cable...
 * 
 * Using a serial port sniffer, the commands can be seen though.
 * They look like NMEA strings - the checksum is actually the NMEA one.
 * The device ID is "PI", the String ID "COA".
 * The End Of String (EOS) has to be "\r\n", which is the opposite of the usual EOS for a Modem ("\n\r"). 
 * 
 * The remote ID (REMOTE_ID variable in the code) can be tricky to identify.
 * This code ha been tested on the ICOM M700PRO.
 * The cloning cable is a Icom OPC-478U
 */
public class CloningCablePilot implements SerialModemInterface, CablePilotClientInterface
{
  private final static String _PORT        = "-port";
  private final static String _BR          = "-br";
  private final static String _DATA_BITS   = "-data-bits";
  private final static String _PARITY      = "-parity";
  private final static String _STOPBIT     = "-stopbit";
  private final static String _HALF_DUPLEX = "-half-duplex";  

  private final static String _VERBOSE     = "-verbose";  
  private final static String _FILE        = "-file";
  private final static String _SYNC        = "-sync";
  private final static String _ECHO_CMD    = "-echo-cmd";
  private final static String _HEADLESS    = "-headless";
  
  private final static String _USER_PROMPT = "-user-prompt";
  
  // Default values for Pactor (except the port name)
  private static String port         = "COM1";
  private static int baudRate        = 4800;
  private static int databits        = 8;
  private static int stopbit         = 1;
  private static int parity          = 0;
  private static boolean half_duplex = false;
  private static boolean headless    = false;
  private static String fileName     = null;
  private static boolean behaveSynchronously = false;
  private static String userPrompt   = "What? > ";
  private static boolean echoCmd     = false;
  
  private static boolean verbose     = false;
  
  private static AsyncCommunication comm = null;
    
  private SerialWriterThread swt = null;
  private boolean keepWorking = true;

  private final static BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
  private static BufferedReader input = null;
/*
  $PICOA,90,00,REMOTE,ON*58
  $PICOA,90,02,REMOTE,ON*5A
  $PICOA,90,02,MODE,USB*18
  $PICOA,90,02,MODE,J3E*60
  $PICOA,90,02,RXF,8.415000*05 ; Rx Freq
  $PICOA,90,02,TXF,8.415000*03 ; Tx Freq

  $PICOA,90,02,REMOTE,OFF*14
*/
  private final static String REMOTE_ID = "02";

  private final static NumberFormat PREFIX = new DecimalFormat("##0");
  private final static NumberFormat SUFFIX = new DecimalFormat("000000");
  
  public static void main(String[] args) throws Exception 
  {
    final CloningCablePilot that = new CloningCablePilot();

    System.out.println("Usage (default values between square brackets):");
    System.out.println("java " + that.getClass().getName() + "\n" + 
                       "  " + _PORT + " [COM1]\n" + 
                       "  " + _BR + " [4800]\n" + 
                       "  " + _DATA_BITS + " [8]\n" + 
                       "  " + _PARITY + " [0]\n" + 
                       "  " + _STOPBIT + " [1]\n" + 
                       "  " + _HALF_DUPLEX + " true|[false]|yes|no|on|off\n" + 
                       "  " + _HEADLESS + " true|[false]|yes|no|on|off\n" + 
                       "  " + _VERBOSE + " true|[false]|yes|no|on|off\n" + 
                       "  " + _FILE + " <script, commands in a text file>\n" + 
                       "  " + _ECHO_CMD + " [true]|false|yes|no|on|off\n" + 
                       "  " + _SYNC + " true|[false]|yes|no|on|off\n" + 
                       "  " + _USER_PROMPT + " [\"What? > \"]\n");
    System.out.println("-------------------------------------------------");    
    Runtime.getRuntime().addShutdownHook(new Thread() 
      {
        public void run() 
        {
          System.out.println ("Shutting down nicely (shutdown hook).");
          if (comm != null)
          {
//          if (that.getSerialWriterThread() != null && that.getSerialWriterThread().isAlive())
            {
//            that.pilotCommand("RELEASE");
            }
            comm.setKeepWorking(false);
            comm.closeAll();
            comm = null;
          }
        }
      });    
    that.initPilot(args);
    System.out.println("Done with main");
  }
  
  public void initPilot(String[] args) throws Exception
  {
    this.getParameters(args);
    
    comm = new AsyncCommunication(this, port, baudRate, databits, stopbit, parity, half_duplex, fileName, verbose, true);    
    comm.setEos(AsyncCommunication.SPECIAL_EOS);
    if (!headless)
    {
      this.setSerialWriterThread(new CloningCablePilot.SerialWriterThread(this));
      this.getSerialWriterThread().start();
    }
    comm.startConsole();
  }

  public void exit()
  {
    if (comm != null)
    {
//    if (this.getSerialWriterThread() != null && this.getSerialWriterThread().isAlive())
      {
        this.pilotCommand("RELEASE");
      }
      comm.setKeepWorking(false);
      comm.closeAll();
      setEnabled(false);
      comm = null;
    }
  }
  
  private static String generateCheckSum(String str)
  {
    int cs = calculateCheckSum(str);
    String gcs = Integer.toString(cs, 16).toUpperCase();
    while (gcs.length() < 2)
      gcs = "0" + gcs;
    return gcs;
  }

  public static int calculateCheckSum(String str) {
    int cs = 0;
    char[] ca = str.toCharArray();
    cs = ca[0];
    for (int i = 1; i < ca.length; i++)
      cs = cs ^ ca[i]; // XOR
    return cs;
  }

  public static String userInput(String prompt)
  {
    String inputString = "";
    System.err.print(prompt);
    try
    {
      inputString = stdin.readLine();
    }
    catch(Exception e)
    {
      System.out.println(e);
      String s;
      try
      {
        s = userInput("<Oooch/>");
      }
      catch(Exception exception) 
      {
        exception.printStackTrace();
      }
    }
    return inputString;
  }  

  private void getParameters(String[] args) throws Exception
  {
    for (int i=0; i<args.length; i++)
    {
      if (args[i].trim().equals(_PORT))
        port = args[i+1];
      else if (args[i].trim().equals(_BR))
        baudRate = Integer.parseInt(args[i+1]);
      else if (args[i].trim().equals(_DATA_BITS))
        databits  = Integer.parseInt(args[i+1]);
      else if (args[i].trim().equals(_PARITY))
        parity =Integer.parseInt(args[i+1]);
      else if (args[i].trim().equals(_STOPBIT))
        stopbit = Integer.parseInt(args[i+1]);
      else if (args[i].trim().equals(_HALF_DUPLEX))
        half_duplex = (args[i+1].equalsIgnoreCase("TRUE") || 
                       args[i+1].equalsIgnoreCase("YES") || 
                       args[i+1].equalsIgnoreCase("ON"));
      else if (args[i].trim().equals(_HEADLESS))
        headless = (args[i+1].equalsIgnoreCase("TRUE") || 
                    args[i+1].equalsIgnoreCase("YES") || 
                    args[i+1].equalsIgnoreCase("ON"));
      else if (args[i].trim().equals(_VERBOSE))
        verbose = (args[i+1].equalsIgnoreCase("TRUE") || 
                   args[i+1].equalsIgnoreCase("YES") || 
                   args[i+1].equalsIgnoreCase("ON"));
      else if (args[i].trim().equals(_FILE))
        fileName = args[i+1];
      else if (args[i].trim().equals(_SYNC))
        behaveSynchronously = (args[i+1].equalsIgnoreCase("TRUE") || 
                               args[i+1].equalsIgnoreCase("YES") || 
                               args[i+1].equalsIgnoreCase("ON"));
      else if (args[i].trim().equals(_USER_PROMPT))
        userPrompt = args[i+1];
      else if (args[i].trim().equals(_ECHO_CMD))
        echoCmd = (args[i+1].equalsIgnoreCase("TRUE") || 
                   args[i+1].equalsIgnoreCase("YES") || 
                   args[i+1].equalsIgnoreCase("ON"));
    }
  }
  
  public void pilotCommand(String cmd)
  {
    String[] commands = null;
    String nmeaString = "";
    
    if (cmd.trim().equalsIgnoreCase("CONNECT"))
    {
      commands = new String[4];
      nmeaString = "PICOA,90,00,REMOTE,ON";
      nmeaString += ("*" + generateCheckSum(nmeaString));    
      commands[0] = ("$" + nmeaString);

      nmeaString = "PICOA,90," + REMOTE_ID + ",REMOTE,ON";
      nmeaString += ("*" + generateCheckSum(nmeaString));    
      commands[1] = ("$" + nmeaString);

      nmeaString = "PICOA,90," + REMOTE_ID + ",MODE,USB";
      nmeaString += ("*" + generateCheckSum(nmeaString));    
      commands[2] = ("$" + nmeaString);

      nmeaString = "PICOA,90," + REMOTE_ID + ",MODE,J3E";
      nmeaString += ("*" + generateCheckSum(nmeaString));    
      commands[3] = ("$" + nmeaString);
    }
    else if (cmd.trim().equalsIgnoreCase("RELEASE"))
    {
      commands = new String[1];
      nmeaString = "PICOA,90," + REMOTE_ID + ",REMOTE,OFF";
      nmeaString += ("*" + generateCheckSum(nmeaString));    
      commands[0] = ("$" + nmeaString);
    }
    else if (cmd.trim().toUpperCase().startsWith("FREQ "))
    {
      commands = new String[2];
      String freq = cmd.substring("FREQ ".length());
      double f = Double.parseDouble(freq);
      f -= 1.5d;
      f *= 1000;
      String frStr = PREFIX.format((int)(f / 1E6)) + "." + SUFFIX.format(f % 1E6);
      nmeaString = "PICOA,90," + REMOTE_ID + ",RXF," + frStr;
      nmeaString += ("*" + generateCheckSum(nmeaString));    
      commands[0] = ("$" + nmeaString);

      nmeaString = "PICOA,90," + REMOTE_ID + ",TXF," + frStr;
      nmeaString += ("*" + generateCheckSum(nmeaString));    
      commands[1] = ("$" + nmeaString);
    }
    else
      System.out.println("Unknown command [" + cmd + "]");
    
    for (int i=0; commands != null &&  i<commands.length; i++)
    {
      talkToPort(commands[i]);
      if (behaveSynchronously)
      {
        synchronized (this) 
        { 
          setMessage("Waiting for the port response");
          try { wait(); }
          catch (InterruptedException ie) { ie.printStackTrace(); }
          setMessage("Wait completed");
        }
      }
    }    
  }
  
  public void talkToPort(String userInput)
  {
    if (comm != null && comm.isPortReady())
    {
      try 
      { 
        comm.getSerialPort().setDTR(true); // Data Terminal Ready
        while (false && (!comm.getSerialPort().isCTS() || !comm.getSerialPort().isDTR()))
        {
          this.setMessage("<Port [" + port + "] not ready>");
          try { Thread.sleep(10); } catch (Exception ex) { ex.printStackTrace(); }
        }
        comm.getSerialOutputStream().write((userInput + comm.getEos()).getBytes()); 
        comm.getSerialOutputStream().flush();
      }
      catch (Exception ex)
      {
        System.out.println("User Input [" + userInput + "] on port [" + port + "]");
        System.err.println("... Comm is " + (comm==null?"":"not ") + "null");
        if (comm != null)
          System.err.println("... Serial Port is " + (comm.getSerialPort()==null?"":"not ") + "null");
        ex.printStackTrace();
      }
    }
    else if (verbose)
      System.out.println("Port [" + port +"] is not ready.");
      
  }
  
  public void onDataReceived(byte[] data)
  {
    // Output here
    String mess = new String(data);
    if (!mess.endsWith("\n") && !mess.endsWith("\r"))
      mess += "\n";
    System.out.print(mess);

    if (behaveSynchronously)
    {
      Thread synchWait = new Thread()
        {
          public void run()
          {
            synchronized (getSerialWriterThread())
            {
              setMessage("Synchronous Behavior: onDataReceived.");
              getSerialWriterThread().notify();
            }
          }
        };
      synchWait.start();
    }
  }
  
  public void onBreakReceived()
  {
    System.out.println("** Break");
  }
  
  public void onCarrierDetectReceived()
  {
    System.out.println("** Carrier Detect");
  }
  
  public void onClearToSendReceived()
  {
    System.out.println("** Clear to Send");
  }
  
  public void onDataSetReadyReceived()
  {
    System.out.println("** Data Set Ready");
  }
  
  public void onFramingErrorReceived()
  {
    System.out.println("** Framing Error");
  }
  
  public void onOverrunErrorReceived()
  {
    System.out.println("** Overrun Error");
  }
  
  public void onOutputBufferEmptyReceived()
  {
    System.out.println("** Output Buffer Empty");
  }
  
  public void onParityErrorReceived()
  {
    System.out.println("** Parity Error");
  }
  
  public void onRingIndicatorReceived()
  {
    System.out.println("** Ring Indicator");
  }
  
  public void setMessage(String mess)
  {
    if (verbose)
      System.out.println(mess);
  }

  public void setSerialWriterThread(CloningCablePilot.SerialWriterThread swt)
  {
    this.swt = swt;
  }

  public CloningCablePilot.SerialWriterThread getSerialWriterThread()
  {
    return swt;
  }

  private static void setVerbose(boolean verbose)
  {
    CloningCablePilot.verbose = verbose;
  }

  // Not Used
  public void setOutputType(int i) 
  {
  }
  
  // Not Used
  public int getOutputType()
  {
    return 0;
  }

  public static AsyncCommunication getComm()
  {
    return comm;
  }

  public static class SerialWriterThread extends Thread
  {
    private CablePilotClientInterface parent = null;
    
    public SerialWriterThread(CablePilotClientInterface parent)
    {
      super();
      this.parent = parent;
      if (fileName != null)
      {
        try { input = new BufferedReader(new FileReader(fileName)); }
        catch (FileNotFoundException fnfe)
        {
          System.out.println(fileName + " not found, exiting.");
          System.exit(1);
        }
      }
    }

    public static String userInput(String prompt)
    {
      String inputString = "";
      System.err.print(prompt);
      try
      {
        inputString = stdin.readLine();
      }
      catch(Exception e)
      {
        System.out.println(e);
        String s;
        try
        {
          s = userInput("<Oooch/>");
        }
        catch(Exception exception) 
        {
          exception.printStackTrace();
        }
      }
      return inputString;
    }
    
    public void run()
    {
      System.out.println("Type [.exit], [.quit] or [.bye] at the prompt to close the program.\n"+ 
                         "<Ctrl+C> would work too.\n" +
                         "Type [.help] for a list of available commands.\n" +
                         "-----------------------------------------------------------------");
      while (parent.isEnabled())
      {
        String userInput = null;
        if (input == null)
          try { userInput = userInput(userPrompt).trim(); } catch (NullPointerException npe) { System.err.println("On gere..."); }
        else
        {
          try 
          { 
            userInput = input.readLine(); 
          } 
          catch (IOException ioe)
          {
            ioe.printStackTrace();
          }      
        }
        if (userInput != null)
        {
          if (!userInput.startsWith("#") && userInput.contains(";"))
            userInput = userInput.substring(0, userInput.indexOf(";")).trim();
          if (!userInput.startsWith("#") && input != null && echoCmd)
            System.out.println("+ Cmd: [" + userInput + "]");
        }
        
        if (userInput == null && input != null) // EOF
        {
        //keepWorking = false;
          try
          {
            input.close();
            input = null; // Swicth to manual input
          }
          catch (Exception ex)
          {
            ex.printStackTrace();
          }
        }
        else if (userInput != null && (userInput.equalsIgnoreCase(".EXIT") || userInput.equalsIgnoreCase(".BYE") || userInput.equalsIgnoreCase(".QUIT"))) // Quit is a Pactor command
          parent.setEnabled(false);
        else if (userInput != null && userInput.equalsIgnoreCase(".HELP"))
        {
          displayHelp();
        }
        else if (userInput != null && userInput.equalsIgnoreCase(".STATUS"))
        {
          comm.displayPortStatus();
        }
        else if (userInput != null && userInput.toUpperCase().startsWith(".INPUT "))
        {
          String fName = userInput.substring(".INPUT ".length());
          try { input = new BufferedReader(new FileReader(fName)); }
          catch (FileNotFoundException fnfe)
          {
            System.out.println(fName + " not found.");
            input = null;
          }          
        }
        else if (userInput != null && userInput.toUpperCase().startsWith(".VERBOSE "))
        {
          String option = userInput.substring(".VERBOSE ".length());
          if (option.equalsIgnoreCase("ON"))
            setVerbose(true);            
          else if (option.equalsIgnoreCase("OFF"))
            setVerbose(false);
          else
            System.out.println("... Invalid option. Only ON or OFF are supported.");
        }
        else if (userInput != null && userInput.equalsIgnoreCase("FUCK"))
        {
          String str = userInput("- Who? > ").trim();
          if (str.equalsIgnoreCase("YOU"))
          {
            System.out.println("+------------------------------------------+");
            System.out.println("| Program aborted due to indecent request. |");
            System.out.println("+------------------------------------------+");
            parent.setEnabled(false);
          }
        }
        else if (userInput != null && !userInput.startsWith("#")) // Forward to the modem
        {
          parent.pilotCommand(userInput);
        }
      }
      parent.setMessage("Bye (from " + this.getClass().getName() + ").");
    }
    
    private void displayHelp()
    {
      System.out.println("Commands (case IN-sensitive) are:");
      System.out.println(".exit, .bye, .quit or <Ctrl+C> to exit");
      System.out.println(".input [fileName] to execute a script of commands");
      System.out.println(".status to display the Serial Port status");
      System.out.println(".verbose on|off ");
      System.out.println(".help to display this message");
      System.out.println("Other commands are:");
      System.out.println("  CONNECT");
      System.out.println("  FREQ [Frequence]");
      System.out.println("  RELEASE");
    }
  }

  public void setEnabled(boolean b)
  {
    keepWorking = b;
    comm.setKeepWorking(b);
  }
  
  public boolean isEnabled()
  {
    return keepWorking;
  }  
}
