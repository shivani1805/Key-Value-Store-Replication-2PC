package server;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class Participant extends UnicastRemoteObject implements ParticipantInterface, ServerInterface {
  private final Map<String, String> kvStore = new HashMap<>();
  private boolean prepared = false;
  private final String coordinatorHost;
  private final int coordinatorPort;

  private final CoordinatorInterface coordinator;
  private final int port;
  private final Registry registry;


  private static final String LOG_FILE = "participant_log.txt";

  /**
   * Constructor to initialize a participant server instance.
   *
   * @param coordinatorHost the hostname of the coordinator
   * @param coordinatorPort the port number of the coordinator
   * @param port            the port number of the participant
   * @throws RemoteException if an RMI error occurs
   */
  public Participant(String coordinatorHost, int coordinatorPort, int port) throws RemoteException, NotBoundException {
    super();
    this.coordinatorHost = coordinatorHost;
    this.coordinatorPort = coordinatorPort;
    this.port = port;
    this.registry = LocateRegistry.getRegistry(coordinatorHost, coordinatorPort);

    this.coordinator = (CoordinatorInterface) registry.lookup("Coordinator");
    log("Participant Server initialized on port " + port + " and connected to Coordinator at " + coordinatorHost + ":" + coordinatorPort);
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

  @Override
  public boolean prepare(String operation, String key, String value, int serverPort) throws RemoteException {
    log("Server "+serverPort+" - Preparing for operation: " + operation + " with key: " + key + " and value: " + value);
    prepared = true;
    return prepared;
  }

  @Override
  public boolean prepare(String operation, String key, int serverPort) throws RemoteException {
    log("Server "+serverPort+" - Preparing for operation: " + operation + " with key: " + key);
    prepared = true;
    return prepared;
  }

  @Override
  public void commit(String operation, String key, String value, int serverPort) throws RemoteException {
    if (!prepared) {
      log("Server "+serverPort+" - Transaction was not prepared. Cannot commit.");
      return;
    }

    log("Server "+serverPort+" - Committing operation: " + operation + " with key: " + key + " and value: " + value);

    switch (operation.toUpperCase()) {
      case "PUT":
        kvStore.put(key, value);
        break;
      default:
        log("Unknown operation: " + operation);
    }
    prepared = false;
  }

  @Override
  public void commit(String operation, String key,int serverPort) throws RemoteException {
    if (!prepared) {
      log("Server "+serverPort+" - Transaction was not prepared. Cannot commit.");
      return;
    }

    log("Server "+serverPort+" - Committing operation: " + operation + " with key: " + key);

    switch (operation.toUpperCase()) {
      case "GET":
        log("Retrieved value: " + kvStore.get(key));
        break;
      case "DELETE":
        kvStore.remove(key);
        break;
      default:
        log("Unknown operation: " + operation);
    }
    prepared = false;
  }

  @Override
  public void abort() throws RemoteException {
    log("Aborting transaction...");
    prepared = false;
  }

  @Override
  public int getPort() {
    return this.port;
  }

  @Override
  public String get(String key, String clientName) throws RemoteException {
    if (kvStore.containsKey(key)) {
      String result = kvStore.get(key);
      log("Client Name - "+clientName+" >"+" Success GET : Success: Key=" + key + ", Value=" + result);
      return result;
    } else {
      log("Client Name - "+clientName+" >"+" Error GET : Key="+key+" not found");
      return "Error: Key not found";
    }
  }

  @Override
  public String delete(String key, String clientName) throws RemoteException {
    try {
      if (!this.coordinator.initiatePreparePhase("DELETE", key)) {
        log("Client Name - "+clientName+" >"+" Error DELETE: Key="+key+" not found");
        return "Error: Key not found";
      } else {
        log("Client Name - "+clientName+" >"+" Success DELETE : Key=" + key + " deleted.");
        return "Success: Key=" + key + " deleted.";
      }
    }  catch (RemoteException re) {
      log("RMI failure occurred during the two-phase protocol (delete): " + re.getMessage());
      return "FAIL: RMI failure. Please try again";
    }
  }

  @Override
  public String put(String key, String value, String clientName) throws RemoteException {
    try {
      if (!this.coordinator.initiatePreparePhase("PUT", key, value)) {
        return "Error PUT: Transaction aborted";
      } else {
        log("Client Name - "+clientName+" >"+" Success PUT : Key=" + key + ", Value=" + value + " stored.");
        return "Success: Key=" + key + ", Value=" + value + " stored.";
      }
    }  catch (RemoteException re) {
      log("RMI failure occurred during the two-phase protocol (put): " + re.getMessage());
      return "FAIL: RMI failure. Please try again";
    }
  }

}
