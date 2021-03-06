COMP 512: Distributed Systems
School of Computer Science
McGill University

A Distributed System in Java using Web Services.
Adapted from CSE 593, University of Washington.

==========

Project Structure:

 + build                          generated by ant
   + classes                        generated code and compiled classes
   + war                            deployable web service

 + etc                            configuration
   - server.policy                  server security policy
   - sun-jaxws.xml                  service endpoint mapping
   - web.xml                        service deployment settings

 + lib                            jar libraries

 + src                            source code
   + client                         client code
     - Client.java                    client CLI
     - WSClient.java                  web service proxy wrapper
   + server                         server code
     + ws                             web service
       - Main.java                      embedded Tomcat launcher
       - ResourceManager.java           resource manager API
     - ResourceManagerImpl.java       resource manager implementation
     - *.java

 + webapps                        generated by ant
   + rm                             deployed web service

 - build.xml                      ant build configuration
 - README.txt                     this file

==========

Ant Targets:

 - clean                          deletes directories: build and webapps
 - setup                          creates directories: build and webapps
 - build-server                   processes annotations and compiles server code
 - create-war                     creates deployable web service
 - deploy-war                     deploys war to webapps directory
 - start-tomcat                   launches embedded Tomcat
 - server                         does all of the above
 - generate-client                generates proxy classes from web service wsdl
 - build-client                   compiles client code
 - client                         does both of the above

Ant Properties:

 - ${service.name}                name of web service (default: rm)
 - ${service.host}                hostname/IP of server (default: localhost)
 - ${service.port}                web service port binding (default: 8080)

Note that you may either modify these properties directly within build.xml,
or specify any of them when launching ant from the command line (as below).

==========

Instructions:

1. Build and launch your service on the server machine:

   [userx][lab1-1][proj1]  ant server -Dservice.port=8081

   Make sure you use a unique port number to avoid port conflicts with other
   groups that may be running their services on the same machine in the lab.

   You may use your group number as a prefix. For example:
     Group 1: 1081; Group 8: 8081; Group 10: 10081; Group 18: 18081

2. Build and launch your client on the client machine:

   [userx][lab1-2][proj1]  ant client -Dservice.host=lab1-1 -Dservice.port=8081

   Make sure you specify the hostname/IP of the server and the port from step 1.

3. Run some test commands on the client:

     [java] Client Interface
     [java] Type "help" for list of supported commands

   newflight,1,1,1,1
     [java] Adding a new Flight using id: 1
     [java] Flight number: 1
     [java] Add Flight Seats: 1
     [java] Set Flight Price: 1
     [java] Flight added

   queryflight,1,1
     [java] Querying a flight using id: 1
     [java] Flight number: 1
     [java] Number of seats available: 1

   If it works, you will see corresponding log messages on the server:

     [java] http-nio-8080-exec-1 INFO: RM::addFlight(1, 1, $1, 1) called.
     [java] http-nio-8080-exec-1 INFO: RM::addFlight(1, 1, $1, 1) OK.
     [java] http-nio-8080-exec-1 INFO: RM::queryNum(1, flight-1) called.
     [java] http-nio-8080-exec-1 INFO: RM::queryNum(1, flight-1) OK: 1


