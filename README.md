This program is a file transfer program built on top of UDP, using TFTP headers (with some modifications for Sliding windows)
TFTP info can be found at : https://tools.ietf.org/html/rfc1350

For the purpose of this program, the client only sends a file, and the Server receives a file.

can execute the program by running: 

* mvn package

* mvn exec:java -Dexec.mainClass=Main -Dexec.args="ARGS GO HERE"

The program supports flags to modify the way data is sent
One flag is used to prefer IPv6 addresses over IPv4, and the other is used to use a TCP style sliding window protocol,
in place of "Stop-and-Wait" acks as used in TFTP. 

Available flags:

One of the IPv flags (both server and client can activate this flag)

* 4 : enables IPv4 Datagram packets (default)

* 6 : enables IPv6 Datagram packets


One of the ack types (client only needs to activate this flag)

* -s : activates sequential acks used in TFTP (default)

* -w : activates TCP-style sliding windows (much faster than -s option)
