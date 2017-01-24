package database;

import database.ConcurrencyControlManager.*;
import database.DBStructure.Table;

import java.util.HashSet;
import java.util.Random;
import java.util.TreeMap;

public class DBConnection {
	/*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++GLOBAL+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

	private static int exceptions = 0;
	public static final long DELAY = 1000;
	
	/**
	 * Function for executing the transaction
	 * @param transaction
     */
    public static void exec(DBTransaction transaction, boolean timeout) {
    	Random r = new Random();

		if(timeout) {
			//Delay for database connection
			try {
				Thread.sleep(DELAY + (transaction.getAgentId() * r.nextInt(10)));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

    	//For serial schedule only, any other lock management has to be done somewhere else
    	ConcurrencyControlManager.lockManagementGrowing(transaction, null, null, null, null);
    	try {
	    	switch (transaction.getType()){
				case allReservations:
					totalReservations(transaction);
					break;
				case bookFlight:
					book(transaction);
					break;
				case cancelFlight:
					cancel(transaction);
					break;
				case myFlights:
					myFlights(transaction);
					break;
				case allFLights:
					getAllFLightsTransaction(transaction);
					break;
				case allPassengers:
					getAllPassengersTransaction(transaction);
					break;
				default:
					break;
	    	}
    	} catch(Exception ex){
    		incrementExceptionCount();
    	}
    	ConcurrencyControlManager.lockManagementShrinking(transaction);
    }
    
    private static synchronized void incrementExceptionCount(){
    	exceptions++;
    }
    
    public static int exceptionCount() {
    	return exceptions;
    }
    
/*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++TRANSACTIONS+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

    /**
	 * Executing the transaction for getting all reservations
	 * @param transaction
     */
	private static void totalReservations(DBTransaction transaction){
		Lock l = ConcurrencyControlManager.lockManagementGrowing(transaction, Table.RESERVATIONS, null, null, LockType.S);
		if(l != null) {
			TreeMap<Integer, TreeMap<Integer, Integer>> reservations =
					DBAccess.getAllReservations();
	
			TreeMap<Integer, TreeMap<Integer, Integer>> reservationsCopy =
					new TreeMap<>(reservations);
	
			int size = 0;
			for(Integer key : reservationsCopy.keySet()){
				size += reservationsCopy.get(key).size();
			}
	
			transaction.setSuccess(true);
			transaction.setTotalResults(size);
		} else
			transaction.setSuccess(false);
	}

	/**
	 * Executing the transaction for booking a flight
	 * @param transaction
     */
    private static void book(DBTransaction transaction){
		Integer passengerId = transaction.getPassengerId();
		Integer flightId = transaction.getFlightId();
		if(passengerId != null && flightId != null){
			Lock l = ConcurrencyControlManager.lockManagementGrowing(transaction, Table.RESERVATIONS, passengerId, null, LockType.IX);
			if(l != null) {
				if(DBAccess.existReservationByPassengerFlightId(flightId, passengerId)){
					transaction.setSuccess(false);
				}
				else {
					Lock l1 = ConcurrencyControlManager.lockManagementGrowing(transaction, Table.SEATS, null, null, LockType.S);
					if(l1 != null) {
						HashSet<Integer> freeSeatSet = DBAccess.getFreeSeatSet(flightId);
						if(freeSeatSet == null || freeSeatSet.size() == 0){
							transaction.setSuccess(false);
						}
						else {
							Lock l2 = ConcurrencyControlManager.lockManagementGrowing(transaction, Table.RESERVATIONS, passengerId, flightId, LockType.X);
							if(l2 != null) {
								transaction.setSuccess(DBAccess.insertReservationByPassengerId(passengerId, flightId,
										DBAccess.chooseFreeSeat(freeSeatSet)));
							} else {
								transaction.setSuccess(false);
							}
						}
					} else {
						transaction.setSuccess(false);
					}
				}
			} else {
				transaction.setSuccess(false);
			}
		}
		else {
			transaction.setSuccess(false);
		}
    }

    /**
	 * Executing the transaction for deleting a flight
	 * @param transaction
     */
    private static void cancel(DBTransaction transaction){
    	Integer passengerId = transaction.getPassengerId();
    	Integer flightId = transaction.getFlightId();
		if(passengerId != null && flightId != null){
			Lock l = ConcurrencyControlManager.lockManagementGrowing(transaction, Table.RESERVATIONS, passengerId, flightId, LockType.X);
			if(l != null) {
				DBAccess.deleteReservationByFlightAndPassengerId(transaction);
			} else
				transaction.setSuccess(false);
		}
		else{
			transaction.setSuccess(false);
		}
    }

    /**
	 * Executing the transaction for getting all flights booked by a passenger
	 * @param transaction
     */
	private static void myFlights(DBTransaction transaction){
		if(transaction.getPassengerId() != null){
			Lock l = ConcurrencyControlManager.lockManagementGrowing(transaction, Table.RESERVATIONS, null, null, LockType.S);
			if(l != null) {
				TreeMap<Integer, Integer> reservationsByPassengerId =
						DBAccess.getReservationsByPassengerId(transaction.getPassengerId());
	
	
				if(reservationsByPassengerId != null && reservationsByPassengerId.keySet().size() > 0){
					TreeMap<Integer, Integer> reservationsByPassengerIdCopy = new TreeMap<>(reservationsByPassengerId);
	
					transaction.setMyFlights(reservationsByPassengerIdCopy.keySet());
				}
	
				transaction.setSuccess(true);
			} else
				transaction.setSuccess(false);
		}
		else{
			transaction.setSuccess(false);
		}
	}

	/**
	 * Executing the transaction for getting all possible flights
	 * @param transaction
     */
	private static void getAllFLightsTransaction(DBTransaction transaction){
		Lock l = ConcurrencyControlManager.lockManagementGrowing(transaction, Table.FLIGHTS, null, null, LockType.S);
		if(l != null) {
			HashSet<Integer> allFlights = DBAccess.getAllFlights(transaction);
	
			if(allFlights != null){
				transaction.setAllFlights(allFlights);
				transaction.setSuccess(true);
			}
			else{
				transaction.setSuccess(false);
			}
		} else
			transaction.setSuccess(false);
	}

	/**
	 * Executing the transaction for getting all possible passengers
	 * @param transaction
     */
	private static void getAllPassengersTransaction(DBTransaction transaction){
		Lock l = ConcurrencyControlManager.lockManagementGrowing(transaction, Table.PASSENGERS, null, null, LockType.S);
		if(l != null) {
			HashSet<Integer> allPassengers = DBAccess.getAllPassengers(transaction);
	
			if(allPassengers != null){
				transaction.setAllPassengers(allPassengers);
				transaction.setSuccess(true);
			}
			else{
				transaction.setSuccess(false);
			}
		} else
			transaction.setSuccess(false);
	}
}
