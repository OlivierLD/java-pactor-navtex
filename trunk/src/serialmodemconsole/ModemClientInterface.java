package serialmodemconsole;

public interface ModemClientInterface
{
  public void talkToPort(String userInput);
  public void setMessage(String mess);
  public void setOutputType(int i);
  public int getOutputType();
  public void setEnabled(boolean b);
  public boolean isEnabled();
}
