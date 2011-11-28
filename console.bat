@setlocal
::
:: Can be used for any Serial Port.
:: start console -modem-eos modem -sync on -echo-cmd off -user-prompt "" -file input.txt -log-output 2011-11-26.2.txt -log-navtex navtex.2011-11-26.2.txt -freq 16806.5 -cloning-cable-port COM11
::
:: Freq : 8416.5 16806.5
::
@echo off
title Oliv's Serial Modem Console - Navtex
set CP=C:\_mywork\dev-corner\olivsoft\SerialModemConsole\classes
set CP=%CP%;D:\OlivSoft\all-3rd-party\rxtx.distrib\RXTXcomm.jar
set CP=%CP%;D:\OlivSoft\all-libs\nmeaparser.jar
set JAVA_OPTIONS=-Djava.library.path=D:\OlivSoft\all-3rd-party\rxtx.distrib\win64
set BR=57600
java -classpath %CP% %JAVA_OPTIONS% ui.AsyncSerialPortUI -port COM12 -br %BR% -data-bits 8 -stopbit 1 -parity 0 -half-duplex on -modem-eos modem -sync on -echo-cmd off -user-prompt "" -file input.txt -freq 16806.5 -cloning-cable-port COM11 %*
@endlocal