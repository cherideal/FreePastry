/*************************************************************************

"FreePastry" Peer-to-Peer Application Development Substrate 

Copyright 2002, Rice University. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

- Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

- Neither  the name  of Rice  University (RICE) nor  the names  of its
contributors may be  used to endorse or promote  products derived from
this software without specific prior written permission.

This software is provided by RICE and the contributors on an "as is"
basis, without any representations or warranties of any kind, express
or implied including, but not limited to, representations or
warranties of non-infringement, merchantability or fitness for a
particular purpose. In no event shall RICE or contributors be liable
for any direct, indirect, incidental, special, exemplary, or
consequential damages (including, but not limited to, procurement of
substitute goods or services; loss of use, data, or profits; or
business interruption) however caused and on any theory of liability,
whether in contract, strict liability, or tort (including negligence
or otherwise) arising in any way out of the use of this software, even
if advised of the possibility of such damage.

********************************************************************************/

package rice.pastry.testing;

import rice.pastry.*;
import rice.pastry.wire.*;
import rice.pastry.wire.exception.NodeIsDeadException;
import rice.pastry.standard.*;
import rice.pastry.dist.*;

import java.util.*;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.*;

/**
 * A hello world example for pastry. This is the distributed driver.
 *
 * For example, run with default arguments on two machines, both with args
 * -bootstrap firstmachine. Read the messages and follow the protocols.
 *
 * Either separately or with the above, try -nodes 3 and -nodes 20.
 * Try -msgs 100. Try the two machine configuration, kill it, restart, and
 * watch it join as a different node. Do that a few times, watch LeafSet
 * entries accumulate, then in 30 seconds, watch the maintenance take over.
 *
 * @version $Id$
 *
 * @author Sitaram Iyer
 * @author Peter Druschel
 */

public class DistHelloWorldMultiThread {
  private PastryNodeFactory factory;
  private Vector pastryNodes;
  private Vector helloClients;
  public Random rng;

  private static int port = 5009;
  private static String bshost = null;
  private static int bsport = 5009;
  private static int numnodes = 5;
  private static int nummsgs = 100;
  public static int protocol = DistPastryNodeFactory.PROTOCOL_WIRE;
  private static boolean toFile = false;

  /**
   * Constructor
   */
  public DistHelloWorldMultiThread() {
    factory =
      DistPastryNodeFactory.getFactory(
        new RandomNodeIdFactory(),
        protocol,
        port);
    pastryNodes = new Vector();
    helloClients = new Vector();
    rng = new Random(PastrySeed.getSeed());
  }

  /**
   * Gets a handle to a bootstrap node. First we try localhost, to see
   * whether a previous virtual node has already bound itself there.
   * Then we try nattempts times on bshost:bsport. Then we fail.
   *
   * @param firstNode true of the first virtual node is being bootstrapped on this host
   * @return handle to bootstrap node, or null.
   */
  protected NodeHandle getBootstrap(boolean firstNode) {
    InetSocketAddress addr = null;
    if (firstNode && bshost != null)
      addr = new InetSocketAddress(bshost, bsport);
    else {
      try {
        addr =
          new InetSocketAddress(
            InetAddress.getLocalHost().getHostName(),
            bsport);
      } catch (UnknownHostException e) {
        System.out.println(e);
      }
    }

    NodeHandle bshandle = ((DistPastryNodeFactory) factory).getNodeHandle(addr);
    return bshandle;
  }

  /**
   * process command line args,
   */
  private static void doIinitstuff(String args[]) {

    // process command line arguments

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-help")) {
        System.out.println(
          "Usage: DistHelloWorld [-msgs m] [-nodes n] [-port p] [-bootstrap bshost[:bsport]]");
        System.out.println(
          "                     [-protocol (rmi|wire)] [-verbose|-silent|-verbosity v] [-help]");
        System.out.println("");
        System.out.println(
          "  Ports p and bsport refer to RMI registry  or Socket port numbers (default = 5009).");
        System.out.println(
          "  Without -bootstrap bshost[:bsport], only localhost:p is used for bootstrap.");
        System.out.println(
          "  Default verbosity is 5, -verbose is 10, and -silent is -1 (error msgs only).");
        System.exit(1);
      }
    }

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-msgs") && i + 1 < args.length)
        nummsgs = Integer.parseInt(args[i + 1]);
    }

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-port") && i + 1 < args.length) {
        int p = Integer.parseInt(args[i + 1]);
        if (p > 0)
          port = p;
        break;
      }
    }

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-bootstrap") && i + 1 < args.length) {
        String str = args[i + 1];
        int index = str.indexOf(':');
        if (index == -1) {
          bshost = str;
          bsport = port;
        } else {
          bshost = str.substring(0, index);
          bsport = Integer.parseInt(str.substring(index + 1));
          if (bsport <= 0)
            bsport = port;
        }
        break;
      }
    }

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-nodes") && i + 1 < args.length) {
        int n = Integer.parseInt(args[i + 1]);
        if (n > 0)
          numnodes = n;
        break;
      }
    }

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-protocol") && i + 1 < args.length) {
        String s = args[i + 1];

        if (s.equalsIgnoreCase("wire"))
          protocol = DistPastryNodeFactory.PROTOCOL_WIRE;
        else if (s.equalsIgnoreCase("rmi"))
          protocol = DistPastryNodeFactory.PROTOCOL_RMI;
        else
          System.out.println("ERROR: Unsupported protocol: " + s);

        break;
      }
    }

  }

  /**
   * Create a Pastry node and add it to pastryNodes. Also create a client
   * application for this node, so that when this node comes up ( when 
   * pn.isReady() is true) , this application's notifyReady() method
   * is called, and it can do any interesting stuff it wants.
   */
  public PastryNode makePastryNode(boolean firstNode) {
    NodeHandle bootstrap = getBootstrap(firstNode);
    PastryNode pn = factory.newNode(bootstrap); // internally initiateJoins
    pastryNodes.addElement(pn);

    HelloWorldAppMultiThread app = new HelloWorldAppMultiThread(pn, this);
    helloClients.addElement(app);
    //if (Log.ifp(5)) System.out.println("created " + pastryNodes.size()+ "th pastry node " + pn);
    return pn;
  }

  static void waitUntilReady(PastryNode pn) {
    synchronized (pn) {
      while (!pn.isReady()) {
        try {
          pn.wait();
        } catch (InterruptedException e) {
          System.out.println(e);
        }
      }
    }
  }

  /**
   * Usage: DistHelloWorld [-msgs m] [-nodes n] [-port p] [-bootstrap bshost[:bsport]]
   *                      [-verbose|-silent|-verbosity v] [-help].
   *
   * Ports p and bsport refer to RMI registry/ Socket port numbers (default = 5009).
   * Without -bootstrap bshost[:bsport], only localhost:p is used for bootstrap.
   * Default verbosity is 5, -verbose is 10, and -silent is -1 (error msgs only).
   */
  public static void main(String args[]) {
    if (toFile) {
      try {
        PrintStream ps =
          new PrintStream(new FileOutputStream("/home/jeffh/moo.txt"));
        System.setOut(ps);
        System.setErr(ps);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    Log.init(args);
    if (protocol == DistPastryNodeFactory.PROTOCOL_WIRE) {      
      Wire.initialize();
    }
    doIinitstuff(args);
    final DistHelloWorldMultiThread driver = new DistHelloWorldMultiThread();

    // create first node
    PastryNode pn = driver.makePastryNode(true);

    // We wait till the first PastryNode on this host is ready so that the 
    // rest of the nodes find a bootstrap node on the local host
    waitUntilReady(pn);

    try {
      for (int i = 1; i < numnodes; i++) {
        System.out.println(
          "* Creating node " + i + "               **********");
        pn = driver.makePastryNode(false);
        Thread.currentThread().sleep(500);
        waitUntilReady(pn);
      }
    } catch (Exception e) {
      //System.out.println(e);
      e.printStackTrace();
    }

    while (!driver.allNodesCreated()) {
      try {
        Thread.sleep(1000);
      } catch (Exception e) {
        //System.out.println(e);
        e.printStackTrace();
      }
    }

    System.out.println(numnodes + " nodes constructed");

    try {
      Thread.currentThread().sleep(2000);
    } catch (Exception e) {
      //System.out.println(e);
      e.printStackTrace();
    }

    for (int i = 0; i < numnodes; i++)
      System.out.println(
        ((PastryNode) driver.pastryNodes.elementAt(i)).getLeafSet() + "");

    for (int i = 0; i < nummsgs; i++) {
      driver.sendRandomMessage();
    }

    //Now sit back and enjoy the fun, add some churn..

    int kill_counter = 0;
    while (true) {
      for (int i = 0; i < 50; i++) {
        driver.sendRandomMessage();
      }
      try {
        System.out.println("Messages Sent so far :" + driver.sentMessages());
        System.out.println(
          "Messages Delivered so far :" + driver.recievedMessages());
        ArrayList list = driver.getMissingMessages();
        System.out.println("Missing Messages so far :" + list.size());
        /*
                        Iterator i = list.iterator();
                        while (i.hasNext()) {
                            HelloMsg m = (HelloMsg)i.next();
                            System.out.println("  Missing msg: "+m);
                            
                        }*/
        if (kill_counter < 10) {
          driver.killPastryNode();
          kill_counter++;
          System.out.println("KillCounter:" + kill_counter);
        }

        Thread.currentThread().sleep(5000);

      } catch (Exception e) {
        e.printStackTrace();
        //                System.out.println(e);
      }
    }
  }

  private void sendRandomMessage() {
    int n = helloClients.size();
    int client = rng.nextInt(n);
    HelloWorldAppMultiThread tempApp =
      (HelloWorldAppMultiThread) helloClients.get(client);

    // get next alive client
    int origClient = client;
    while (!tempApp.getNodeHandle().isAlive()) {
      client++;
      if (client >= n) {
        client = 0;
      }
      if (client == origClient) {
        return; // we looped around, and all of the clients are dead
      }
      tempApp = (HelloWorldAppMultiThread) helloClients.get(client);
    }

    final HelloWorldAppMultiThread app = tempApp;
    final int msgId = getNextMsgId();
    Thread t = new Thread("Hello Client # " + app.getNodeId() + "," + msgId) {
      public void run() {
        try {
          Thread.currentThread().sleep(rng.nextInt(5000));
          app.sendRndMsg(rng, msgId);
        } catch (Exception e) {
          //System.out.println("Error Creating a new client thread for "+app.getNodeId()+" " + e);
          if (!(e instanceof NodeIsDeadException)) {
            e.printStackTrace();
          }
        }
      }
    };

    t.start();

  }

  //Global BookKeeping
  //Precious Global variables - need to have synchronized access to them

  private HashMap SentMessageLog = new HashMap();
  private HashMap RecievedMessageLog = new HashMap();
  private static int id = 0;
  private int ready_nodes = 0;

  public synchronized void MessageSent(HelloMsg helloMsg) { // int messageId, NodeId node){
    SentMessageLog.put(new Integer(helloMsg.getId()), helloMsg);
  }

  public synchronized void MessageRecieved(HelloMsg helloMsg) {
    //        Thread.dumpStack();
    RecievedMessageLog.put(new Integer(helloMsg.getId()), helloMsg);
  }

  private synchronized int sentMessages() {
    return SentMessageLog.size();
  }

  private synchronized int recievedMessages() {
    return RecievedMessageLog.size();
  }

  private synchronized ArrayList getMissingMessages() {
    ArrayList list = new ArrayList();
    Set s = SentMessageLog.keySet();
    Iterator i = s.iterator();
    while (i.hasNext()) {
      Integer messageId = (Integer) i.next();
      HelloMsg from = (HelloMsg) SentMessageLog.get(messageId);
      HelloMsg to = (HelloMsg) RecievedMessageLog.get(messageId);
      if (to == null) {
        list.add(from);
      }
    }
    return list;
  }

  public synchronized boolean allNodesCreated() {
    if (ready_nodes == numnodes)
      return true;
    return false;
  }

  public synchronized void node_created() {
    //System.out.println("Node Ready");
    ready_nodes++;
  }

  private synchronized int getNextMsgId() {
    return ++id;
  }

  /**
  * Create a Pastry node and add it to pastryNodes. Also create a client
  * application for this node, so that when this node comes up ( when 
  * pn.isReady() is true) , this application's notifyReady() method
  * is called, and it can do any interesting stuff it wants.
  */
  public void killPastryNode() {

    DistPastryNode pn =
      (DistPastryNode) pastryNodes.get(rng.nextInt(pastryNodes.size()));
    System.out.println("***********************   killing pastry node:" + pn);
    pn.kill();
  }

}
