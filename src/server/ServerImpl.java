package server;

import java.io.FileWriter;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;


public class ServerImpl {

  private static final int numParticipants = 5;

  private static final String LOG_FILE = "coordinator_log.txt";


  public static void main(String[] args) {

    if (args.length < 2) {
      System.err.println("Correct Format: java server/ServerImpl <port-number> <server-name>");
      System.exit(1);
    }


    int coordinatorPort;
    String serverName = args[1];
    String coordinatorHost = "localhost";
    try {
      coordinatorPort = Integer.parseInt(args[0]);
    } catch (NumberFormatException e) {
      System.err.println("Invalid port number: " + args[0]);
      System.exit(1);
      return;
    }

    try {

      Registry coordinatorRegistry = LocateRegistry.createRegistry(coordinatorPort);
      Coordinator coordinator = new Coordinator();
      coordinatorRegistry.rebind("Coordinator", coordinator);


      for (int i = 0; i < numParticipants; i++) {
        int port = coordinatorPort + 1 + i;
        Participant participant = new Participant(coordinatorHost, coordinatorPort, port);
        Registry participantRegistry = LocateRegistry.createRegistry(port);
        participantRegistry.rebind(serverName, participant);
      }


      coordinator.connectToParticipants(
              Arrays.asList(coordinatorHost, coordinatorHost, coordinatorHost, coordinatorHost, coordinatorHost),
              Arrays.asList(coordinatorPort + 1, coordinatorPort + 2, coordinatorPort + 3, coordinatorPort + 4, coordinatorPort + 5)
              , serverName );
      log("Servers ready...");
    } catch (RemoteException e) {
      System.err.println("RMI failure while starting the servers: " + e.getMessage());
      System.exit(1);
    } catch (NotBoundException e) {
      throw new RuntimeException(e);
    }
  }
  private static void log(String message) {
    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    String logMessage = "[" + timestamp + "] " + message;
    System.out.println(logMessage);

    try (FileWriter logFile = new FileWriter(LOG_FILE, true)) {
      logFile.write(logMessage + "\n");
    } catch (IOException e) {
      System.err.println("Failed to log to file: " + e.getMessage());
    }
  }
}