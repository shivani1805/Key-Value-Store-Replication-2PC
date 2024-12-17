package server;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The Client class implements a remote client that connects to a
 * server using RMI. It allows users to perform operations like
 * storing, retrieving, and deleting key-value pairs through a
 * console interface or by executing commands from a script file.
 */
public class Client {
  private static final String LOG_FILE = "client-log.txt";
  private static final String OPERATIONS_SCRIPT = "operations-script.txt";
  private static final String DATA_POPULATE_SCRIPT = "data-population-script.txt";

  private ServerInterface server;
  private ExecutorService executor;

  public static void main(String[] args) {
    Client client = new Client();
    client.run(args);
  }

  private void run(String[] args) {
    if (args.length != 4) {
      log("Incorrect Arguments");
      throw new IllegalArgumentException("Please enter the following arguments - <IP/hostname> <Port-number> <server-name> <client-name>");
    }

    String hostname = args[0];
    int port = Integer.parseInt(args[1]);
    String serverName  = args[2];
    String clientName = args[3];

    log("Starting Client");
    log("Attempting connection to " + hostname + " on port " + port);

    try {
      Registry registry = LocateRegistry.getRegistry(hostname, port);
      server = (ServerInterface) registry.lookup(serverName);
      log("Connection established to the server : "+serverName);

      log("Starting Data Population");
      processScript(DATA_POPULATE_SCRIPT,clientName);
      log("Data Population Completed");

      executor = Executors.newFixedThreadPool(5);
      Scanner scanner = new Scanner(System.in);
      while (true) {
        log("Enter 'run' to execute command from script, 'console' to enter commands manually,'close' to exit, or 'concurrent' to run concurrent commands from script: ");
        String userInput = scanner.nextLine().trim().toLowerCase();

        if ("console".equals(userInput)) {
          log("Entering manual command mode. Type 'exit' to return to the main menu.");
          while (true) {
            System.out.print("Enter command: ");
            String command = scanner.nextLine();
            if ("exit".equalsIgnoreCase(command)) break;
            handleRequest(command,clientName);
          }
        } else if ("close".equals(userInput)) {
          log("Exiting client.");
          break;
        } else if ("run".equals(userInput)) {
          processScript(OPERATIONS_SCRIPT,clientName);
          log("All Operations Completed");
        } else if ("concurrent".equals(userInput)) {
          log("Running concurrent requests.");
          runConcurrentCommands(clientName);
        } else {
          log("Invalid input. Please enter 'console', 'close', 'run', or 'concurrent'.");
        }
      }
    } catch (Exception e) {
      log("Error connecting to server: " + e.getMessage());
    } finally {
      executor.shutdown();
    }
  }

  /**
   * Runs commands concurrently by reading them from the operations script.
   */
  private void runConcurrentCommands(String clientName) {
    try (BufferedReader scriptReader = new BufferedReader(new FileReader(OPERATIONS_SCRIPT))) {
      String line;
      while ((line = scriptReader.readLine()) != null) {
        String finalLine = line;
        executor.submit(() -> handleRequest(finalLine,clientName));
      }
    } catch (FileNotFoundException e) {
      log("Script file not found: " + OPERATIONS_SCRIPT);
    } catch (IOException e) {
      log("Error reading script file: " + e.getMessage());
    }
  }

  /**
   * Processes a given script by reading commands from the specified script file
   * for performing operations or pre-populating the data.
   *
   * @param scriptPath the path to the script file
   */
  private void processScript(String scriptPath, String clientName) {
    try (BufferedReader scriptReader = new BufferedReader(new FileReader(scriptPath))) {
      String line;
      while ((line = scriptReader.readLine()) != null) {
        handleRequest(line,clientName);
      }
    } catch (FileNotFoundException e) {
      log("Script file not found: " + scriptPath);
    } catch (IOException e) {
      log("Error reading script file: " + e.getMessage());
    }
  }

  /**
   * Handles a request based on the provided command string.
   *
   * @param command the command string to process
   */
  private void handleRequest(String command, String clientName) {
    String[] parts = command.split(" ", 3);
    if (parts.length < 2) {
      log("Invalid command format. Use: PUT key value, GET key, or DELETE key.");
      return;
    }

    String operation = parts[0].toUpperCase();
    String key = parts[1];
    String response = null;

    try {
      switch (operation) {
        case "PUT":
          if (parts.length != 3) {
            log("PUT command requires a key and a value.");
            return;
          }
          String value = parts[2];
          response = server.put(key, value,clientName);
          break;
        case "GET":
          response = server.get(key,clientName);
          break;
        case "DELETE":
          response = server.delete(key,clientName);
          break;
        default:
          log("Invalid operation. Supported operations: PUT, GET, DELETE.");
          return;
      }

      log("Response from server: " + response);
    } catch (RemoteException e) {
      log("Remote exception: " + e.getMessage());
    }
  }


  /**
   * Logs messages to both the console and a log file, including a timestamp.
   *
   * @param message the message to log
   */
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
