package database;

import java.util.HashSet;
import java.util.TreeMap;

public class DBStructure{
	public enum Table {
		FLIGHTS, SEATS, PASSENGERS, RESERVATIONS
	}

	/**
	 * Holds all the flight-IDs
	 */
	private static final HashSet<Integer> flights = new HashSet<>();
	/**
	 * Holds the seat-IDs per flight-ID
	 * TreeMap<Flight-ID, HashSet<Seat-IDs>>
	 */
	private static final TreeMap<Integer, HashSet<Integer>> seatsPerFlightId = new TreeMap<>();
	/**
	 * Holds all the passenger-IDs
	 */
	private static final HashSet<Integer> passengers = new HashSet<>();
	/**
	 * Holds the reservations of a passenger
	 * TreeMap<Passenger-ID, TreeMap<Flight-ID, Seat-ID>>
	 */
	private static final TreeMap<Integer, TreeMap<Integer, Integer>>
		reservations = new TreeMap<>();

	public static HashSet<Integer> getFlights() {
		return flights;
	}

	public static TreeMap<Integer, HashSet<Integer>> getSeatsPerFlightId() {
		return seatsPerFlightId;
	}

	public static HashSet<Integer> getPassengers() {
		return passengers;
	}

	public static TreeMap<Integer, TreeMap<Integer, Integer>> getReservations() {
		return reservations;
	}
	
	public static void clearDataStructures() {
		flights.clear();
		seatsPerFlightId.clear();
		passengers.clear();
		reservations.clear();
	}
}
