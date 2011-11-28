package serialmodemconsole;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.TooManyListenersException;

/**
 * Asynchrone
 * 2 Threads, one for input, one for output.
 */
public class AsyncCommunication
{
  public final static String MODEM_EOS   = "\n\r";
  public final static String STD_EOS     = "\n";
  public final static String SPECIAL_EOS = "\r\n"; // Cloning cable...
  
  private String eos = MODEM_EOS;

  private final static boolean REPLACE_OUTPUT_CR_WITH_NL = false;  
  
  // Default values for Pactor (except the port name)
  private String port         = "COM1";
  private int baudRate        = 57600;
  private int databits        = 8;
  private int stopbit         = 1;
  private int parity          = 0;
  private boolean half_duplex = true;
  
  private String fileName     = null;  
  private static boolean verbose = false;
  private boolean modemComm      = true; // set to false to simulate a dialog
  private boolean portReady     = false;
  private boolean pinging        = false;
  
  private boolean serialListenersAdded = false;
  
  private CommPortIdentifier portId = null;
  private SerialPort serialPort     = null;
  
  private OutputStream os = null;
  private InputStream  is = null;
  
  private boolean keepWorking = true;
  private SerialReaderThread srt = null;
  
  private SerialModemInterface invoker = null;
  
  public AsyncCommunication(SerialModemInterface smi, 
                            String port, 
                            int baudRate, 
                            int databits,
                            int stopbit, 
                            int parity, 
                            boolean half_duplex, 
                            String fileName, 
                            boolean verbose, 
                            boolean modemComm)
  {
    super();
    this.invoker = smi;
    this.port = port;
    this.baudRate = baudRate;
    this.databits = databits;
    this.stopbit = stopbit;
    this.parity = parity;
    this.half_duplex = half_duplex;
    this.fileName = fileName;
    this.verbose = verbose;
    this.modemComm = modemComm;
  }

  public void startConsole()
  {
    // Dump parameters
    if (verbose)
    {
      System.out.println("Port:" + this.getPort());
      System.out.println("Baud Rate:" + this.getBaudRate());
      System.out.println("Data bits:" + this.getDatabits());
      System.out.println("Stop Bit:" + this.getStopbit());
      System.out.println("Parity:" + this.getParity());
      System.out.println("Half duplex:" + this.isHalf_duplex());
    }

    try
    {
      this.setPortId(CommPortIdentifier.getPortIdentifier(this.getPort()));
      if (this.getPortId().isCurrentlyOwned())
      {
        System.err.println("Detected " + this.getPortId().getName() + " in use by " + this.getPortId().getCurrentOwner() + ", exiting.");
        System.exit(1);
      }
      this.setSerialPort((SerialPort)this.getPortId().open("ModemConsole", 2000));

      int oldBaudRate    = this.getSerialPort().getBaudRate();
      int oldDatabits    = this.getSerialPort().getDataBits();
      int oldStopbits    = this.getSerialPort().getStopBits();
      int oldParity      = this.getSerialPort().getParity();
      int oldFlowControl = this.getSerialPort().getFlowControlMode();
//    String uart        = this.getSerialPort().getUARTType();
      if (false && verbose) // Skip this
      {
        System.out.println("Default parameters were : baud rate:" + oldBaudRate + ", databits:" + oldDatabits + ", stopbits:" + oldStopbits + ", parity:" + oldParity + ", flow control:" + oldFlowControl); // + ", UART:" + uart);
        System.out.println("New ones for [" + port + "]    : baud rate:" + baudRate + ", databits:" + databits + ", stopbits:" + stopbit + ", parity:" + parity);
      }
      // Set connection parameters, if set fails return parameters object
      // to original state.
      try
      {
        this.getSerialPort().setSerialPortParams(this.getBaudRate(), 
                                                 this.getDatabits(), 
                                                 this.getStopbit(),
                                                 this.getParity());
      }
      catch (UnsupportedCommOperationException e)
      {
        throw new RuntimeException("Unsupported parameter");
      }
     
      this.getSerialPort().setFlowControlMode(SerialPort.FLOWCONTROL_NONE); // Init
      if (half_duplex)
      {
        // Set flow control, whatever happens.
        try
        {
//        this.getSerialPort().setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
          this.getSerialPort().setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_OUT);
        }
        catch (UnsupportedCommOperationException e)
        {
          throw new RuntimeException("Unsupported flow control");
        }
        finally
        {
          // tiens fume.
        }
      }
      this.getSerialPort().setRTS(true); // Request To Send
      this.getSerialPort().setDTR(true); // Data Terminal Ready
      
      // Status
      if (verbose) { displayPortStatus(); }
      
      try
      {
        this.setSerialOutputStream(this.getSerialPort().getOutputStream());
        this.setSerialInputStream(this.getSerialPort().getInputStream());
      }
      catch (IOException e)
      {
        this.getSerialPort().close();
        throw new RuntimeException("Error opening i/o streams");
      }
      // Now we're working!            
      this.setSerialReaderThread(new SerialReaderThread(this));
      this.getSerialReaderThread().start();
      
      if (modemComm)
      {
        // Establish communication. Ping until is responds.
        WakeUpCallThread wuct = new WakeUpCallThread(this);
        try
        {
          pinging = true;
          synchronized (this) //.getSerialReaderThread()) 
          { 
            wuct.start();
            wait(); 
            if (verbose)
              displayPortStatus();
          }
          pinging = false;
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
        }
      }
      invoker.setMessage("Port [" + port + "] is ready.");
      System.out.println("Port [" + port + "] is ready.");
      synchronized (this)
      {
        wait();
      }
      invoker.setMessage("Bottom of the loop");
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
    finally
    {
      closeAll();
      System.out.println("Bye-bye...");
    }
  }
  
  public void closeAll()
  {
    // Close what's opened
    if (this.getSerialInputStream() != null)
      try { this.getSerialInputStream().close(); } catch (Exception ex) { ex.printStackTrace(); }
    if (this.getSerialOutputStream() != null)
      try { this.getSerialOutputStream().close(); } catch (Exception ex) { ex.printStackTrace(); }
    this.getSerialPort().close();      
    System.out.println("Done.");
  }
  
  public void displayPortStatus()
  {
    String port     = this.getSerialPort().getName();
    int baudRate    = this.getSerialPort().getBaudRate();
    int databits    = this.getSerialPort().getDataBits();
    int stopbits    = this.getSerialPort().getStopBits();
    int parity      = this.getSerialPort().getParity();
    int flowControl = this.getSerialPort().getFlowControlMode();
    boolean rts = this.getSerialPort().isRTS(); // Request to Send
    boolean cts = this.getSerialPort().isCTS(); // Clear to Send
    boolean dsr = this.getSerialPort().isDSR(); // Data Set Ready
    boolean cdc = this.getSerialPort().isCD();  // Carrier Detect
    boolean dtr = this.getSerialPort().isDTR(); // Data Terminal Ready
    boolean ri  = this.getSerialPort().isRI();  // Ring Indicator
    
    System.out.println("------------------------------" + 
                       "\nPort:" + port + 
                       "\nBaud Rate:" + baudRate + " bps" +
                       "\nData bits:" + databits + 
                       "\nStop bits:" + stopbits + 
                       "\nparity   :" + displayParity(parity) + 
                       "\nRTS/CTS in   " + (((flowControl & SerialPort.FLOWCONTROL_RTSCTS_IN)   == SerialPort.FLOWCONTROL_RTSCTS_IN)?"[X]":"[ ]") + 
                       "\nRTS/CTS out  " + (((flowControl & SerialPort.FLOWCONTROL_RTSCTS_OUT)  == SerialPort.FLOWCONTROL_RTSCTS_OUT)?"[X]":"[ ]") + 
                       "\nXON/XOFF in  " + (((flowControl & SerialPort.FLOWCONTROL_XONXOFF_IN)  == SerialPort.FLOWCONTROL_XONXOFF_IN)?"[X]":"[ ]") + 
                       "\nXON/XOFF out " + (((flowControl & SerialPort.FLOWCONTROL_XONXOFF_OUT) == SerialPort.FLOWCONTROL_XONXOFF_OUT)?"[X]":"[ ]") + 
                       "\nRTS : " + (rts?"[X]":"[ ]") +
                       "\nCTS : " + (cts?"[X]":"[ ]") + 
                       "\nDSR : " + (dsr?"[X]":"[ ]") + 
                       "\nCD  : " + (cdc?"[X]":"[ ]") + 
                       "\nDTR : " + (dtr?"[X]":"[ ]") +
                       "\nRI  : " + (ri?"[X]":"[ ]") + 
                       "\n------------------------------");
  }
  
  private String displayParity(int p)
  {
    String parity = "NONE";
    switch(p)
    {
      case SerialPort.PARITY_EVEN: 
        parity = "EVEN";
        break;
      case SerialPort.PARITY_MARK:
        parity = "MARK";
        break;
      case SerialPort.PARITY_ODD: 
        parity = "ODD";
        break;
      case SerialPort.PARITY_SPACE:
        parity = "SPACE";
        break;
      case SerialPort.PARITY_NONE: 
      default:
        parity = "NONE";
        break;
    }
    return parity;
  }
  
  public void setBaudRate(int baudRate)
  {
    this.baudRate = baudRate;
  }

  public int getBaudRate()
  {
    return baudRate;
  }

  public void setDatabits(int databits)
  {
    this.databits = databits;
  }

  public int getDatabits()
  {
    return databits;
  }

  public void setStopbit(int stopbit)
  {
    this.stopbit = stopbit;
  }

  public int getStopbit()
  {
    return stopbit;
  }

  public void setParity(int parity)
  {
    this.parity = parity;
  }

  public int getParity()
  {
    return parity;
  }

  public void setHalf_duplex(boolean half_duplex)
  {
    this.half_duplex = half_duplex;
  }

  public boolean isHalf_duplex()
  {
    return half_duplex;
  }

  public void setPort(String port)
  {
    this.port = port;
  }

  public String getPort()
  {
    return port;
  }

  public void setPortId(CommPortIdentifier portId)
  {
    this.portId = portId;
  }

  public CommPortIdentifier getPortId()
  {
    return portId;
  }

  public void setSerialPort(SerialPort serialPort)
  {
    this.serialPort = serialPort;
  }

  public SerialPort getSerialPort()
  {
    return serialPort;
  }

  public void setSerialOutputStream(OutputStream os)
  {
    this.os = os;
  }

  public OutputStream getSerialOutputStream()
  {
    return os;
  }

  public void setSerialInputStream(InputStream is)
  {
    this.is = is;
  }

  public InputStream getSerialInputStream()
  {
    return is;
  }

  public void setKeepWorking(boolean keepWorking)
  {
    this.keepWorking = keepWorking;
    // Notify reader thread
    if (this.getSerialReaderThread() != null && this.getSerialReaderThread().isAlive() && !keepWorking)
    {
      synchronized (this.getSerialReaderThread()) 
      { 
        this.getSerialReaderThread().notify(); 
      }
    }
    synchronized (this)
    {
      this.notify();
    }
  }

  public boolean isKeepWorking()
  {
    return keepWorking;
  }

  public void setSerialReaderThread(AsyncCommunication.SerialReaderThread srt)
  {
    this.srt = srt;
  }

  public AsyncCommunication.SerialReaderThread getSerialReaderThread()
  {
    return srt;
  }

  public void setPortReady(boolean portReady)
  {
    this.portReady = portReady;
  }

  public boolean isPortReady()
  {
    return portReady;
  }

  public void setEos(String eos)
  {
    this.eos = eos;
  }

  public String getEos()
  {
    return eos;
  }

  public void requestNotifications(SerialPort sp)
  {
    sp.notifyOnDataAvailable(true);
    sp.notifyOnBreakInterrupt(true);
//  sp.notifyOnCTS(true);
//  sp.notifyOnCarrierDetect(true);
//  sp.notifyOnDSR(true);
//  sp.notifyOnFramingError(true);
//  sp.notifyOnOutputEmpty(true);
//  sp.notifyOnOverrunError(true);
//  sp.notifyOnParityError(true);
//  sp.notifyOnRingIndicator(true);    
  }
  
  public class SerialReaderThread extends Thread implements SerialPortEventListener
  {
    private AsyncCommunication parent = null;
    public SerialReaderThread(AsyncCommunication parent)
    {
      super();
      this.parent = parent;
      if (!serialListenersAdded)
      {
        try
        {
          invoker.setMessage("Adding listeners (" + this.getClass().getName() + ")");
          parent.getSerialPort().addEventListener(this);
          serialListenersAdded = true;
        }
        catch (TooManyListenersException tmle)
        {
          parent.getSerialPort().close();
//        throw new RuntimeException(this.getClass().getName() + ":too many listeners added");
          throw new RuntimeException(tmle);
        }
      }
      requestNotifications(parent.getSerialPort());
      // Set receive timeout to allow breaking out of polling loop during input handling.
      try
      {
        parent.getSerialPort().enableReceiveTimeout(30);
      }
      catch (UnsupportedCommOperationException e)
      {
        e.printStackTrace();
      }
    }
    
    public void run()
    {
      while (parent.isKeepWorking())
      {
        try
        {
          synchronized ( this) { wait(); }
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
        }
        parent.setKeepWorking(false);
      }
      invoker.setMessage("Bye (from " + this.getClass().getName() + ").");
    }

    public void serialEvent(SerialPortEvent serialPortEvent)
    {
      invoker.setMessage("SerialPortEvent received from port [" + port + "]");
      if (pinging)
      {
        parent.setPortReady(true);
  //    return;
      }
      List<Byte> inputlist = new ArrayList<Byte>();
      int newData = 0;

      switch (serialPortEvent.getEventType())
      {
        // Read data until -1 is returned. If \r is received substitute
        // \n for correct newline handling.
        case SerialPortEvent.DATA_AVAILABLE:
          while (newData != -1)
          {
            try
            {
              newData = parent.getSerialInputStream().read();
              if (newData == -1)
              {
                break;
              }
              else if ('\r' == (char) newData)
              {
                if (REPLACE_OUTPUT_CR_WITH_NL)
                  inputlist.add((byte)'\n'); 
              }
              else
              {
                inputlist.add((byte)newData); 
              }
            }
            catch (IOException ex)
            {
              System.err.println(ex);
              return;
            }
          }
          Object[] oa = inputlist.toArray();
          byte[] ba = new byte[oa.length];
          for (int i=0; i<oa.length; i++)
            ba[i] = ((Byte)oa[i]).byteValue();
          invoker.onDataReceived(ba);
          break;        
        case SerialPortEvent.BI: 
          invoker.onBreakReceived();          
          break;
        case SerialPortEvent.CD:
          invoker.onCarrierDetectReceived();
          break;
        case SerialPortEvent.CTS:
          invoker.onClearToSendReceived();
          break;
        case SerialPortEvent.DSR:
          invoker.onDataSetReadyReceived();
          break;
        case SerialPortEvent.FE:
          invoker.onFramingErrorReceived();
          break;
        case SerialPortEvent.OE:
          invoker.onOverrunErrorReceived();
          break;
        case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
          invoker.onOutputBufferEmptyReceived();
          break;
        case SerialPortEvent.PE:
          invoker.onParityErrorReceived();
          break;
        case SerialPortEvent.RI:
          invoker.onRingIndicatorReceived();
          break;
        default:
          invoker.setMessage("Unknown SerialEvent [" + serialPortEvent.getEventType() + "]");
          break;
      }
    }
  }
  
  private class WakeUpCallThread extends Thread
  {
    private AsyncCommunication parent = null;
    
    public WakeUpCallThread(AsyncCommunication parent)
    {
      super();
      this.parent = parent;
      if (!serialListenersAdded)
      {
        try
        {
          invoker.setMessage("Adding listeners (" + this.getClass().getName() + ")");
          parent.getSerialPort().addEventListener(parent.getSerialReaderThread());
          serialListenersAdded = true;
        }
        catch (TooManyListenersException tmle)
        {
          parent.getSerialPort().close();
//        throw new RuntimeException(this.getClass().getName() + ":too many listeners added");
          throw new RuntimeException(tmle);
        }
      }
      requestNotifications(parent.getSerialPort());
      // Set receive timeout to allow breaking out of polling loop during input handling.
      try
      {
        parent.getSerialPort().enableReceiveTimeout(30);
      }
      catch (UnsupportedCommOperationException e)
      {
        e.printStackTrace();
      }
    }
    
    public void run()
    {
      int nbTry = 0;
      while (!parent.isPortReady())
      {
        try
        {
          parent.getSerialOutputStream().write(("" + eos).getBytes()); // boom-boom-boom
          parent.getSerialOutputStream().flush();          
          if (verbose)
            System.out.print(".");
//        Thread.sleep(100L);
          if ((nbTry + 1) % 1000 == 0)
            System.out.println("Port [" + port + "], tried " + (nbTry + 1) + " times...");
          nbTry++;
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
        }
      }
      System.out.println("Port [" + port + "] successfully opened after " + nbTry + " try(ies)");
      synchronized (parent) { parent.notify(); }
//    parent.getSerialPort().notifyOnOutputEmpty(true);
      invoker.setMessage("End of WakeUpThread");
    }
  }
}
