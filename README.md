# SoftwareDefinedNetworkSimulation
Use Java threads to simulate central controller and switches in a Software Defined Network so that Switches route along a highest bottleneck bandwidth  path.

Demos:
1.Bootstrap of controller : reading configuration file  
$ java Controller topo_6nodes.txt localhost 30000  

2.Switches register on controller and receive alive neighbors information  

$ java Switch 1 localhost 30000  
$ java Switch 2 localhost 30000  
$ java Switch 3 localhost 30000  

3.Switches mutually communicate to know new alive neighbors (-v will verbosely log KKEP_ALIVE message)
$ java Switch 4 localhost 30000 -v  
$ java Switch 5 localhost 30000 -v  

4.Routing tables are sent to switches after all switches registered  
$ java Switch 6 localhost 30000  

4.One Switch fails, controller recompute route table  
- neighbor switches will report link fail by not including it in TOPOLOGY_UPDATE  
- controller will find switch fail by not receiving TOPOLOGY_UPDATE in time  
CTRL+C on one switch process

5.Switch recover, controller recompute route table  
Restart the switch process normally

6.One link of a Switch fails, controller recompute route table  
- neighbor switches will report link fail by not including it in TOPOLOGY_UPDATE  
$java Switch 3 localhost 30000 -f 2 4
