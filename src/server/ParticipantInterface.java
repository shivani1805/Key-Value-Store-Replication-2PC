package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The ParticipantInterface defines methods that a participant should implement
 * to handle requests from the coordinator in a distributed transaction system.
 */
public interface ParticipantInterface extends Remote {

  /**
   * Prepares the participant for a transaction.
   *
   * @param operation the type of operation (e.g., "PUT", "DELETE")
   * @param key       the key involved in the transaction
   * @param value     the value involved in the transaction (optional for some operations)
   * @return true if the participant is ready to proceed, false otherwise
   * @throws RemoteException if an RMI error occurs
   */
  boolean prepare(String operation, String key, String value, int serverPort) throws RemoteException;

  /**
   * Prepares the participant for a transaction without a value.
   *
   * @param operation the type of operation (e.g., "GET")
   * @param key       the key involved in the transaction
   * @return true if the participant is ready to proceed, false otherwise
   * @throws RemoteException if an RMI error occurs
   */
  boolean prepare(String operation, String key, int serverPort) throws RemoteException;

  /**
   * Commits the transaction for the participant.
   *
   * @param operation the type of operation
   * @param key       the key involved in the transaction
   * @param value     the value involved in the transaction
   * @throws RemoteException if an RMI error occurs
   */
  void commit(String operation, String key, String value, int serverPort) throws RemoteException;

  /**
   * Commits the transaction for the participant without a value.
   *
   * @param operation the type of operation
   * @param key       the key involved in the transaction
   * @throws RemoteException if an RMI error occurs
   */
  void commit(String operation, String key, int serverPort) throws RemoteException;

  /**
   * Aborts the transaction for the participant.
   *
   * @throws RemoteException if an RMI error occurs
   */
  void abort() throws RemoteException;

  int getPort() throws RemoteException;
}

