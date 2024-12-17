package server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.io.FileWriter;
import java.io.IOException;

public class Coordinator extends UnicastRemoteObject implements CoordinatorInterface {

  private final List<ParticipantInterface> participants = new ArrayList<>();
  private final List<ParticipantInfo> participantInfoList = new ArrayList<>();
  private static final String LOG_FILE = "coordinator_log.txt";

  protected Coordinator() throws RemoteException {
    super();
  }


  private static class ParticipantInfo {
    String host;
    int port;

    ParticipantInfo(String host, int port) {
      this.host = host;
      this.port = port;
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

  @Override
  public void connectToParticipants(List<String> participantHosts, List<Integer> participantPorts, String serverName)
          throws RemoteException {
    for (int i = 0; i < participantHosts.size(); i++) {
      try {
        Registry registry = LocateRegistry.getRegistry(participantHosts.get(i), participantPorts.get(i));
        ParticipantInterface participant = (ParticipantInterface) registry.lookup(serverName);
        participants.add(participant);
        participantInfoList.add(new ParticipantInfo(participantHosts.get(i), participantPorts.get(i)));
        log("Connected to participant at " + participantHosts.get(i) + ":" + participantPorts.get(i));
      } catch (Exception e) {
        log("Failed to connect to participant at " + participantHosts.get(i) + ":" + participantPorts.get(i));
        e.printStackTrace();
      }
    }
  }

  @Override
  public boolean initiatePreparePhase(String operation, String key, String value) throws RemoteException {
    log("Initiating prepare phase for operation: " + operation);
    for (ParticipantInterface participant : participants) {
      boolean isReady = participant.prepare(operation, key, value,participant.getPort());
      if (!isReady) {
        log("Prepare phase failed. Aborting transaction.");
        sendAbortToAll();
        return false;
      }
    }
    log("Prepare phase successful. Committing transaction.");
    sendCommitToAll(operation, key, value);
    return true;
  }

  @Override
  public boolean initiatePreparePhase(String operation, String key) throws RemoteException {
    log("Initiating prepare phase for operation: " + operation);
    for (ParticipantInterface participant : participants) {
      boolean isReady = participant.prepare(operation, key, participant.getPort());
      if (!isReady) {
        log("Prepare phase failed. Aborting transaction.");
        sendAbortToAll();
        return false;
      }
    }
    log("Prepare phase successful. Committing transaction.");
    sendCommitToAll(operation, key);
    return true;
  }

  private void sendAbortToAll() {
    for (ParticipantInterface participant : participants) {
      try {
        participant.abort();
        log("Abort message sent successfully.");
      } catch (RemoteException e) {
        log("Failed to send abort to participant.");
        e.printStackTrace();
      }
    }
  }

  private void sendCommitToAll(String operation, String key, String value) {
    for (ParticipantInterface participant : participants) {
      try {
        participant.commit(operation, key, value,participant.getPort());
        log("Commit message sent successfully.");
      } catch (RemoteException e) {
        log("Failed to send commit to participant.");
        e.printStackTrace();
      }
    }
  }

  private void sendCommitToAll(String operation, String key) {
    for (ParticipantInterface participant : participants) {
      try {
        participant.commit(operation, key,participant.getPort());
        log("Commit message sent successfully.");
      } catch (RemoteException e) {
        log("Failed to send commit to participant.");
        e.printStackTrace();
      }
    }
  }
}
