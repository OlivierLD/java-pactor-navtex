package serialmodemconsole;

public interface CablePilotClientInterface
  extends ModemClientInterface
{
  public void pilotCommand(String cmd);
}
