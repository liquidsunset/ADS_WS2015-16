package database;

import java.util.HashSet;
import java.util.Set;

public class DBTransaction extends Thread {

	public enum TransactionType{
		bookFlight(0,"BOOK"), cancelFlight(1,"CANCEL"),
		myFlights(2,"MYFLIGHTS"), allReservations(3,"RESERVATIONS"),
		allFLights(4,"ALLFLIGHTS"), allPassengers(5,"ALLPASSANGERS");
		Integer value;
		String name;

		TransactionType(Integer value, String name){
			this.value = value;
			this.name = name;
		}
		
	}

	//private Integer type;
	private TransactionType type;
	private Integer agentId;

	private Integer passengerId;
	private Integer flightId;

	private Integer totalResults;
	private boolean success;
	private Set<Integer> myFlights;
	private HashSet<Integer> allFlights;
	private HashSet<Integer> allPassengers;

	//DBTransaction(Integer type, Integer agenId){
	DBTransaction(TransactionType type, Integer agentId) {
		this.type = type;
		this.agentId = agentId;
		this.passengerId = null;
		this.flightId = null;
	}

	DBTransaction(TransactionType type, Integer agentId, Integer passengerId, Integer flightId){
		this.type = type;
		this.agentId = agentId;
		this.flightId = flightId;
		this.passengerId = passengerId;
	}

	public Integer getTypeValue() {
		return type.value;
	}
	
	public TransactionType getType() {
		return type;
	}

	public Integer getTotalResults() {
		return totalResults;
	}

	public void setTotalResults(Integer totalResults) {
		this.totalResults = totalResults;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public Set<Integer> getMyFlights() {
		return myFlights;
	}

	public void setMyFlights(Set<Integer> myFlights) {
		this.myFlights = myFlights;
	}

	public HashSet<Integer> getAllFlights() {
		return allFlights;
	}

	public void setAllFlights(HashSet<Integer> allFlights) {
		this.allFlights = allFlights;
	}

	public HashSet<Integer> getAllPassengers() {
		return allPassengers;
	}

	public void setAllPassengers(HashSet<Integer> allPassengers) {
		this.allPassengers = allPassengers;
	}

	public Integer getPassengerId() {
		return passengerId;
	}

	public void setPassengerId(Integer passengerId) {
		this.passengerId = passengerId;
	}

	public Integer getFlightId() {
		return flightId;
	}

	public void setFlightId(Integer flightId) {
		this.flightId = flightId;
	}

	public Integer getAgentId() {
		return agentId;
	}
	@Override
	public String toString() {
		String representation = "agent " + this.agentId + " transaction " + this.type.name;
		if(this.flightId != null && this.passengerId != null)
			representation += " values (" + this.flightId + "," + this.passengerId + ")";
		else if(this.flightId != null)
			representation += " flightId " + this.flightId;
		else if(this.passengerId != null)
			representation += " passengerId " + this.passengerId;
		return representation;
	}

}
