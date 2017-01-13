package serialmodemconsole;

public interface SerialModemInterface
{
  public void onDataReceived(byte[] data);
  public void onBreakReceived();
  public void onCarrierDetectReceived();
  public void onClearToSendReceived();
  public void onDataSetReadyReceived();
  public void onFramingErrorReceived();
  public void onOverrunErrorReceived();
  public void onOutputBufferEmptyReceived();
  public void onParityErrorReceived();
  public void onRingIndicatorReceived();
  public void setMessage(String mess);
}
