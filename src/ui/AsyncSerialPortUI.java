package ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import navtextutil.NavTex;

import serialmodemconsole.AsyncCommunication;
import serialmodemconsole.ModemClientInterface;
import serialmodemconsole.SerialModemInterface;

public class AsyncSerialPortUI implements SerialModemInterface, ModemClientInterface
{
  private final static String _PORT        = "-port";
  private final static String _BR          = "-br";
  private final static String _DATA_BITS   = "-data-bits";
  private final static String _PARITY      = "-parity";
  private final static String _STOPBIT     = "-stopbit";
  private final static String _HALF_DUPLEX = "-half-duplex";  

  private final static String _VERBOSE     = "-verbose";  
  private final static String _FILE        = "-file";
  private final static String _MODEM_COMM  = "-modem-comm";
  private final static String _SYNC        = "-sync";
  private final static String _MODEM_EOS   = "-modem-eos";
  private final static String _ECHO_CMD    = "-echo-cmd";
  
  private final static String _LOG_OUTPUT  = "-log-output";
  private final static String _LOG_NAVTEX  = "-log-navtex";
  private final static String _USER_PROMPT = "-user-prompt";
  
  private final static String _CLONING_CABLE_PORT = "-cloning-cable-port";
  private final static String _FREQ        = "-freq";

  private final static String _HELP        = "-help";

  // Default values for Pactor (except the port name)
  private static String port         = "COM1";
  private static int baudRate        = 57600;
  private static int databits        = 8;
  private static int stopbit         = 1;
  private static int parity          = 0;
  private static boolean half_duplex = true;
  private static String fileName     = null;
  private static boolean behaveSynchronously = false;
  private static String modemEos     = "MODEM";
  private static String logFileName  = null;
  private static String navtexFileName = null;
  private static String userPrompt   = "What? > ";
  private static boolean echoCmd     = true;
  
  private static String cc_port      = "COM2";
  private static String frequence    = null; //"8416.5";

  private static boolean verbose     = false;
  private static boolean modemComm   = true;
  
  private static boolean justHelp    = false;
  
  private static AsyncCommunication comm = null;
  private static CloningCablePilot ccPilot = null;

  public final static int NONE        = 0; // 000
  public final static int ASCII_TYPE  = 1; // 001
  public final static int BIN_TYPE    = 2; // 010
  public final static int NAVTEX_TYPE = 4; // 100
    
  private static int outputType = NONE | NAVTEX_TYPE; // | BIN_TYPE;
  
  private SerialWriterThread swt = null;
  private boolean keepWorking = true;

  private final static BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
  private static BufferedReader input = null;
  private static BufferedWriter logfile = null;
  private static BufferedWriter navtexfile = null;

  public static void main(String[] args) throws Exception
  {
    System.out.println("Usage (default values between square brackets):");
    AsyncSerialPortUI that = new AsyncSerialPortUI();
    System.out.println("java " + that.getClass().getName() + "\n" + 
                       "  " + _PORT + " [COM1]\n" + 
                       "  " + _BR + " [57600]\n" + 
                       "  " + _DATA_BITS + " [8]\n" + 
                       "  " + _PARITY + " [0]\n" + 
                       "  " + _STOPBIT + " [1]\n" + 
                       "  " + _HALF_DUPLEX + " [true]|false|yes|no|on|off\n" + 
                       "  " + _VERBOSE + " true|[false]|yes|no|on|off\n" + 
                       "  " + _FILE + " <script, commands in a text file>\n" + 
                       "  " + _MODEM_COMM + " [true]|false|yes|no|on|off\n" + 
                       "  " + _ECHO_CMD + " [true]|false|yes|no|on|off\n" + 
                       "  " + _SYNC + " true|[false]|yes|no|on|off\n" + 
                       "  " + _MODEM_EOS + " [MODEM]|STD|SPECIAL\n" +
                       "  " + _LOG_OUTPUT + " <log-file>\n" +
                       "  " + _LOG_NAVTEX + " <navtex-file>\n" +
                       "  " + _CLONING_CABLE_PORT + " [COM2]\n" +
                       "  " + _FREQ + " 8416.5\n" +
                       "  " + _USER_PROMPT + " [\"What? > \"]\n");
    System.out.println(" or\n");
    System.out.println("java " + that.getClass().getName() + " " + _HELP + "\n");
    System.out.println("--------------------------------------------------");    
    System.out.println("Current output is set to " + 
                       (((outputType & ASCII_TYPE) == ASCII_TYPE)?"ACSII ":"") + 
                       (((outputType & NAVTEX_TYPE) == NAVTEX_TYPE)?"NAVTEX ":"") + 
                       (((outputType & BIN_TYPE) == BIN_TYPE)?"BIN ":""));    
    System.out.println("-------------------------------------------------");    
    that.getParameters(args);
    if (justHelp)
      System.exit(0);
    
    if (frequence != null)
    {
      Thread pilotThread = new Thread()
        {
          public void run()
          {
            try
            {
              ccPilot = new CloningCablePilot();
              ccPilot.initPilot(new String[] { "-port", "COM11", 
                                               "-br", "4800",
                                               "-data-bits", "8",
                                               "-stopbit", "1",
                                               "-parity", "0",
                                               "-half-duplex", "off",
                                               "-sync", "off", 
                                               "-verbose", (verbose?"on":"off"),
                                               "-headless", "true" });
            }
            catch (Exception ex)
            {
              ex.printStackTrace();
            }
          }
        };
      pilotThread.start();
    }

    comm = new AsyncCommunication(that, port, baudRate, databits, stopbit, parity, half_duplex, fileName, verbose, modemComm);
    
    if (modemEos.equalsIgnoreCase("MODEM"))
      comm.setEos(AsyncCommunication.MODEM_EOS);
    else if (modemEos.equalsIgnoreCase("STD"))
      comm.setEos(AsyncCommunication.STD_EOS);
    else if (modemEos.equalsIgnoreCase("SPECIAL")) // For Cloning Cable
      comm.setEos(AsyncCommunication.SPECIAL_EOS);
    else
    {
      System.out.println("Unknown EOS type [" + modemEos + "], reseting to MODEM");
      comm.setEos(AsyncCommunication.MODEM_EOS);
    }
    
    Runtime.getRuntime().addShutdownHook(new Thread() 
      {
        public void run() 
        {
          System.out.println ("Shutting down nicely (shutdown hook).");
          comm.setKeepWorking(false);
          comm.closeAll();
          if (ccPilot != null)
          {
            ccPilot.pilotCommand("RELEASE");
            ccPilot.exit();
          }
          if (logfile != null)
          {
            try
            {
              logfile.flush();
              logfile.close();
            }
            catch (IOException ioe)
            {
              ioe.printStackTrace();
            }
          }
          if (navtexfile != null)
          {
            try
            {
              navtexfile.flush();
              navtexfile.close();
            }
            catch (IOException ioe)
            {
              ioe.printStackTrace();
            }
          }
        }
      });    
    that.setSerialWriterThread(new AsyncSerialPortUI.SerialWriterThread(that));
    that.getSerialWriterThread().start();
    
    if (frequence != null)
    {
      int nbTry = 0;
      while (ccPilot == null || (ccPilot != null && !ccPilot.getComm().isPortReady()))
      {
        if (++nbTry % 100 == 0)
          System.out.println("... Tried cloning cable " + nbTry + " time(s)");
        try { Thread.sleep(100L); } catch (InterruptedException ie) { ie.printStackTrace(); }
      }
      System.out.println("*****************************************************");
      System.out.println("** Connecting cloning cable (" + (nbTry + 1) + " tests)");
      System.out.println("*****************************************************");
      ccPilot.pilotCommand("CONNECT");
      ccPilot.pilotCommand("FREQ " + frequence);
    }
    else
    {
      System.out.println("** frequence:" + (frequence==null?"null":frequence) + ", ccPilot is " + (ccPilot==null?"":"not ") + "null");
    }
      
    
    if (logFileName != null)
    {
      try { logfile = new BufferedWriter(new FileWriter(logFileName)); }
      catch (Exception ex) { ex.printStackTrace(); }
    }
    if (navtexFileName != null)
    {
      try { navtexfile = new BufferedWriter(new FileWriter(navtexFileName)); }
      catch (Exception ex) { ex.printStackTrace(); }
    }

    comm.startConsole();
    System.out.println("Done with main");
  }

  private void getParameters(String[] args) throws Exception
  {
    for (int i=0; i<args.length; i++)
    {
      if (args[i].trim().equals(_PORT))
        port = args[i+1];
      else if (args[i].trim().equals(_CLONING_CABLE_PORT))
        cc_port = args[i+1];
      else if (args[i].trim().equals(_FREQ))
        frequence = args[i+1];
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
      else if (args[i].trim().equals(_VERBOSE))
        verbose = (args[i+1].equalsIgnoreCase("TRUE") || 
                   args[i+1].equalsIgnoreCase("YES") || 
                   args[i+1].equalsIgnoreCase("ON"));
      else if (args[i].trim().equals(_FILE))
        fileName = args[i+1];
      else if (args[i].trim().equals(_MODEM_COMM))
        modemComm = (args[i+1].equalsIgnoreCase("TRUE") || 
                     args[i+1].equalsIgnoreCase("YES") || 
                     args[i+1].equalsIgnoreCase("ON"));
      else if (args[i].trim().equals(_SYNC))
        behaveSynchronously = (args[i+1].equalsIgnoreCase("TRUE") || 
                               args[i+1].equalsIgnoreCase("YES") || 
                               args[i+1].equalsIgnoreCase("ON"));
      else if (args[i].trim().equals(_MODEM_EOS))
        modemEos = args[i+1];
      else if (args[i].trim().equals(_LOG_OUTPUT))
        logFileName = args[i+1];
      else if (args[i].trim().equals(_LOG_NAVTEX))
        navtexFileName = args[i+1];
      else if (args[i].trim().equals(_USER_PROMPT))
        userPrompt = args[i+1];
      else if (args[i].trim().equals(_HELP))
        justHelp = true;
      else if (args[i].trim().equals(_ECHO_CMD))
        echoCmd = (args[i+1].equalsIgnoreCase("TRUE") || 
                   args[i+1].equalsIgnoreCase("YES") || 
                   args[i+1].equalsIgnoreCase("ON"));
    }
  }
  
  public void talkToPort(String userInput)
  {
    if (modemComm && comm.isPortReady())
    {
      try 
      { 
        comm.getSerialPort().setDTR(true); // Data Terminal Ready
        while (false && (!comm.getSerialPort().isCTS() || !comm.getSerialPort().isDTR()))
        {
          this.setMessage("<Port not ready>");
          try { Thread.sleep(10); } catch (Exception ex) { ex.printStackTrace(); }
        }
        comm.getSerialOutputStream().write((userInput + comm.getEos()).getBytes()); 
        comm.getSerialOutputStream().flush();
      }
      catch (Exception ex)
      {
        System.out.println("User Input [" + userInput + "]");
        System.err.println("... Comm is " + (comm==null?"":"not ") + "null");
        if (comm != null)
          System.err.println("... Serial Port is " + (comm.getSerialPort()==null?"":"not ") + "null");
        ex.printStackTrace();
      }
    }
    else if (!modemComm)  // Echo
    {
      onDataReceived( ("NO_COMM: [" + userInput + "]\n").getBytes());
    }
  }
  
  public void onDataReceived(byte[] data)
  {
    // Output here
    if ((outputType & ASCII_TYPE) == ASCII_TYPE && comm.isPortReady())
    {
      String mess = new String(data);
      if (!mess.endsWith("\n") && !mess.endsWith("\r"))
        mess += "\n";
      System.out.print(mess);
    }
    if ((outputType & NAVTEX_TYPE) == NAVTEX_TYPE && comm.isPortReady())
    {
      String mess = new String(NavTex.decodeNavtex(data));
      if (mess.contains(NavTex.MESSAGE_HEADER))
        System.out.println("---------- New Message >> ------------");
      System.out.print(mess);
      if (mess.contains(NavTex.MESSAGE_FOOTER))
        System.out.println("---------- << End Message ------------");
      if (navtexfile != null)
      {
        try
        {
          navtexfile.write(mess);
          navtexfile.flush();
        }
        catch (IOException ioe)
        {
          ioe.printStackTrace();
        }
      }
    }
    if ((outputType & BIN_TYPE) == BIN_TYPE) // Display even if modem not ready yet
    {
      for (int i=0; i<data.length; i++)
      {
        String s = Integer.toHexString((int)data[i] & 0xFF).toUpperCase();
        while (s.length() < 2)
          s = "0" + s;
        System.out.print(s + " ");
        if ((i + 1) % 20 == 0)
          System.out.println(); // New Line
      }
      System.out.println();
    }
    if (logfile != null)
    {
      try
      {
        for (int i=0; i<data.length; i++)
          logfile.write(data[i]);
        logfile.write('\n');
        logfile.flush();
      }
      catch (IOException ioe)
      {
        ioe.printStackTrace();
      }
    }
    if (behaveSynchronously)
    {
      Thread synchWait = new Thread()
        {
          public void run()
          {
            if (!modemComm)
              try { Thread.sleep(500L); } catch (InterruptedException ie) { ie.printStackTrace(); } // Simulate wait

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
    System.out.println("\n*****************");
    System.out.println("** Carrier Detect");
    System.out.println("*****************");
  }
  
  public void onClearToSendReceived()
  {
    System.out.println("\n*****************");
    System.out.println("** Clear to Send");
    System.out.println("*****************");
  }
  
  public void onDataSetReadyReceived()
  {
    System.out.println("\n*****************");
    System.out.println("** Data Set Ready");
    System.out.println("*****************");
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

  public void setSerialWriterThread(AsyncSerialPortUI.SerialWriterThread swt)
  {
    this.swt = swt;
  }

  public AsyncSerialPortUI.SerialWriterThread getSerialWriterThread()
  {
    return swt;
  }

  public void setOutputType(int outputType)
  {
    this.outputType = outputType;
  }

  public int getOutputType()
  {
    return outputType;
  }

  private static void setVerbose(boolean verbose)
  {
    AsyncSerialPortUI.verbose = verbose;
  }

  public static class SerialWriterThread extends Thread
  {
    private ModemClientInterface parent = null;
    
    public SerialWriterThread(ModemClientInterface parent)
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
    
    private final static SimpleDateFormat SDF_DATE = new SimpleDateFormat("dd.MM.yy");
    private final static SimpleDateFormat SDF_TIME = new SimpleDateFormat("HH:mm:ss");
    
    /**
     * Patches ${date}, ${time}, ${utc.date}, ${utc.time}
     * @param str String containing the above
     * @return the patched string
     */
    private static String patch(String str)
    {
      String before = str.substring(0, str.indexOf("${"));
      String variable = str.substring(str.indexOf("${") + "${".length(), str.indexOf("}"));
      String after  =  str.substring(str.indexOf("}") + 1);
      String patched = "-unknown-";
      
      Calendar cal = GregorianCalendar.getInstance();
      
      if (variable.equals("date"))
      {
        SDF_DATE.setTimeZone(TimeZone.getDefault());
        patched = SDF_DATE.format(cal.getTime());
      }
      else if (variable.equals("time"))
      {
        SDF_TIME.setTimeZone(TimeZone.getDefault());
        patched = SDF_TIME.format(cal.getTime());
      }
      else if (variable.equals("utc.date"))
      {
        SDF_DATE.setTimeZone(TimeZone.getTimeZone("UTC"));
        patched = SDF_DATE.format(cal.getTime());
      }
      else if (variable.equals("utc.time"))
      {
        SDF_TIME.setTimeZone(TimeZone.getTimeZone("UTC"));
        patched = SDF_TIME.format(cal.getTime());
      }
      String newString = before + patched + after;
      
      return newString;
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
          if (userInput.contains("${"))
            userInput = patch(userInput);
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
        else if (userInput != null && userInput.toUpperCase().startsWith(".OUTPUT"))
        {
          String[] parts = userInput.split(" ");
          if (parts.length == 1)
            System.out.println("... Incorrect syntax. .OUTPUT NONE | [ASCII] [BIN]");
          else
          {
            for (int i=1; i<parts.length; i++)
            {
              if (parts[i].equalsIgnoreCase("NONE"))
                parent.setOutputType(NONE);
              else if (parts[i].equalsIgnoreCase("ASCII"))
                parent.setOutputType(parent.getOutputType() | ASCII_TYPE);
              else if (parts[i].equalsIgnoreCase("BIN"))
                parent.setOutputType(parent.getOutputType() | BIN_TYPE);
              else if (parts[i].equalsIgnoreCase("NAVTEX"))
                parent.setOutputType(parent.getOutputType() | NAVTEX_TYPE);
              else
                System.out.println("... Unknown output type [" + parts[i] + "]");
            }
          }
        }
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
        else if (userInput != null && userInput.toUpperCase().startsWith(".FREQ "))
        {
          String frequence = userInput.substring(".FREQ ".length());
          if (ccPilot != null)
            ccPilot.pilotCommand("FREQ " + frequence);
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
          parent.talkToPort(userInput);
          if (behaveSynchronously)
          {
            synchronized (this) 
            { 
              parent.setMessage("Waiting for the Modem response");
              try { wait(); }
              catch (InterruptedException ie) { ie.printStackTrace(); }
              parent.setMessage("Wait completed");
            }
          }
        }
      }
      parent.setMessage("Bye (from " + this.getClass().getName() + ").");
    }
    
    private void displayHelp()
    {
      System.out.println("Commands (case IN-sensitive) are:");
      System.out.println(".exit, .bye, .quit or <Ctrl+C> to exit");
      System.out.println(".output none, bin, ascii or navtex for the output (can be combined. none has to be first to reset)");
      System.out.println(".input [fileName] to execute a script of commands");
      System.out.println(".status to display the Serial Port status");
      System.out.println(".freq 8416.5 if there is a cloning cable, to set the frequence of the receiver");
      System.out.println(".verbose on|off ");
      System.out.println(".help to display this message");
      System.out.println("Other commands will be sent to the modem.");
    }
  }

  public void setEnabled(boolean b)
  {
    keepWorking = b;
    comm.setKeepWorking(b);
    if (ccPilot != null)
      ccPilot.exit();
  }
  
  public boolean isEnabled()
  {
    return keepWorking;
  }  
}
