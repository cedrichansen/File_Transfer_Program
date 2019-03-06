This program is a file transfer program built on top of UDP

can execute by running 

* mvn package

* mvn exec:java -Dexec.mainClass=Main -Dexec.args="ARGS GO HERE"

the available flags are:

one of the IPv flags

* 4 : enables IPv4 Datagram packets

* 6 : enables IPv6 Datagram packets


one of the ack types

* -w : activates TCP-style sliding windows 

* -s : activates sequential acks used in TFTP

pretending to drop 1% of packets

* -d: pretend to drop packets


NOTE: Flags have not yet been configured
