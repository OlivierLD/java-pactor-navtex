Implements a Java communication between any device with a serial port and a Pactor Modem, to decode NAVTEX transmissions.
Uses the comm api provided by [RxTx](http://rxtx.qbang.org/wiki/index.php/Download).

![http://3.bp.blogspot.com/-InDzzHMO4Ww/TtPdw7ASgYI/AAAAAAAAANA/aiAIcPSXCuA/s400/pactor.png](http://3.bp.blogspot.com/-InDzzHMO4Ww/TtPdw7ASgYI/AAAAAAAAANA/aiAIcPSXCuA/s400/pactor.png)

As of now, the UI is minimal, console-like.
Commands are:
```
Usage (default values between square brackets):
java ui.AsyncSerialPortUI
  -port [COM1]
  -br [57600]
  -data-bits [8]
  -parity [0]
  -stopbit [1]
  -half-duplex [true]|false|yes|no|on|off
  -verbose true|[false]|yes|no|on|off
  -file <script, commands in a text file>
  -step-by-step true|[false]|yes|no|on|off
  -modem-comm [true]|false|yes|no|on|off
  -echo-cmd [true]|false|yes|no|on|off
  -sync true|[false]|yes|no|on|off
  -modem-eos [MODEM]|STD|SPECIAL
  -log-output <log-file>
  -log-navtex <navtex-file>
  -cloning-cable-port [COM2]
  -freq 8416.5
  -user-prompt ["What? > "]

 or

java ui.AsyncSerialPortUI -help
```