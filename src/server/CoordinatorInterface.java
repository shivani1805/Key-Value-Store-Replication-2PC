package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.List;

/**
 * The CoordinatorInterface defines methods that a coordinator should implement
 * to manage participants and handle distributed transactions using the two-phase commit protocol.
 */
public interface CoordinatorInterface extends Remote {
  /**
   * Establishes connections to the participant servers.
   *
   * @param participantHosts the list of participant hostnames or IPs
   * @param participantPorts the list of corresponding participant port numbers
   * @throws RemoteException if an RMI error occurs
   */
  void connectToParticipants(List<String> participantHosts, List<Integer> participantPorts, String serverName) throws RemoteException;

  /**
   * Initiates the prepare phase of a two-phase commit protocol for operations with a value.
   *
   * @param operation the type of operation (e.g., "PUT", "DELETE")
   * @param key       the key involved in the transaction
   * @param value     the value involved in the transaction
   * @return the decision outcome of the prepare phase (true if successful, false otherwise)
   * @throws RemoteException if an RMI error occurs
   */
  boolean initiatePreparePhase(String operation, String key, String value) throws RemoteException;

  /**
   * Initiates the prepare phase of a two-phase commit protocol for operations without a value.
   *
   * @param operation the type of operation (e.g., "GET")
   * @param key       the key involved in the transaction
   * @return the decision outcome of the prepare phase (true if successful, false otherwise)
   * @throws RemoteException if an RMI error occurs
   */
  boolean initiatePreparePhase(String operation, String key) throws RemoteException;

}
