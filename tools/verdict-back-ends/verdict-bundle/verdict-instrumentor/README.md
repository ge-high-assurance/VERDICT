VDM Threat instrumentor:
-----------------
compile:
# mvn clean install
# cd target

usage: VERDICT-Instrumentor
 -i,--VDM Model <arg>                       Input Model File
 -o,--Instrumented Model <arg>              Instrumented Model File

 -HT,--Harware Trojan                       Harware Trojans
                                            Instrumentation
 -IT,--Insider Threat                       Insider Threat Instrumentation
 -LB,--Logic Bomb                           Logic Bomb Instrumentation
 -LS,--Location Spoofing                    Location Spoofing attack
                                            Instrumentation
 -NI,--Network Injection                    Network Injection
                                            Instrumentation
 -OT,--Outsider Threat                      Outsider Threat
                                            Instrumentation
 -RI,--Remotet Code Injection               Remotet Code Injection
                                            Instrumentation
 -SV,--Software Virus/malware/worm/trojan   Software
                                            Virus/malware/worm/trojan
                                            Instrumentation
 -BN,--Benign                               Benign (Default)

Example Run:
#java -jar verdict-instrumentor-1.0.0-SNAPSHOT.jar -i <input model(*.xml)> -o <instrumented model(.xml)> -<Choosen attack?>
