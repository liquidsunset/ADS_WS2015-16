package database;

import java.util.HashSet;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class DBAccess {
    

/*---------------------------------------------------------------LOCAL----------------------------------------------------------------------------*/

	//SELECT * FROM (
	//SELECT seat_id FROM seats WHERE flight_id=flightId as seats_per_flight
	//					MINUS/EXCEPT
	//SELECT seat_id FROM reservations WHERE flight_id=flightId as reservations_per_flight;
	//											);

	/**
	 * Function for getting the available seats
	 * @param flightId
	 * @return HashSet with the IDs of the free seats, null on error or when the flight
	 * is booked up
     */
	protected static HashSet<Integer> getFreeSeatSet(Integer flightId){
		HashSet<Integer> seats = getSeatsPerFlightId(flightId);

		if(seats == null)
			return null;

		HashSet<Integer> reservedSeats = new HashSet<>();
		TreeMap<Integer, TreeMap<Integer, Integer>> reservations =
				getAllReservations();

		if(reservations == null)
			return null;

		TreeMap<Integer, TreeMap<Integer, Integer>> reservationCopy =
				new TreeMap<>(reservations);

		for(Integer key : reservationCopy.keySet()){
			TreeMap<Integer, Integer> entry = reservationCopy.get(key);

			if(entry.containsKey(flightId)){
				reservedSeats.add(entry.get(flightId));
			}
		}

		//flight is booked up
		if(seats.size() == reservedSeats.size()){
			return null;
		}

		return seats.stream().filter(
				seat -> !reservedSeats.contains(seat)).collect(Collectors.toCollection(HashSet::new));
	}

	//Nested sql statement:
	//SELECT * FROM passengers WHERE passgenger_id=passengerId as reservations_per_customer;
	//SELECT COUNT(*) FROM  reservations_per_customer WHERE flight_id=flightId;
	//1==true, 0==false

	/**
	 * Function to check if passenger has already booked a seat for a certain flight
	 * @param flightId
	 * @param passengerId
     * @return true if passenger has already booked the flight, otherwise false
     */
	protected static boolean existReservationByPassengerFlightId(Integer flightId, Integer passengerId) {
		TreeMap<Integer, Integer> reservationsByPassengerId =
				getReservationsByPassengerId(passengerId);

		if(reservationsByPassengerId == null)
			return false;

		TreeMap<Integer, Integer> reservationsByPassengerIdCopy =
				new TreeMap<>(reservationsByPassengerId);

		return reservationsByPassengerIdCopy.containsKey(flightId);
	}
	
	//SELECT * FROM passengers WHERE passgenger_id=passengerId;

	/**
	 * Function for getting all reservations from a passenger
	 * @param passengerId
	 * @return TreeMap with all the reservations from a certain passenger
     */
	protected static TreeMap<Integer, Integer> getReservationsByPassengerId(Integer passengerId){
    	TreeMap<Integer, Integer> reservationsByPassengerId = DBStructure.getReservations().get(passengerId);

		return reservationsByPassengerId;
    }

    //INSERT INTO reservations VALUES(passengerId, entry.value, entry.key);

	/**
	 * Adds a booking to the reservations
	 * @param passengerId
	 * @param flightId
	 * @param seatId
     * @return true
     */
	protected static boolean insertReservationByPassengerId(Integer passengerId, Integer flightId, Integer seatId){
	    if(DBStructure.getReservations().containsKey(passengerId)){
	    	DBStructure.getReservations().get(passengerId).put(flightId, seatId);
	    }else{
	    	TreeMap<Integer, Integer> flightReservations = new TreeMap<>();
	        flightReservations.put(flightId, seatId);
	        DBStructure.getReservations().put(passengerId,flightReservations);
	    }
	    return true;	
    }

	/**
	 * Function for getting a random seat
	 * @param freeSeats
	 * @return random chosen seat
     */
	protected static Integer chooseFreeSeat(HashSet<Integer> freeSeats){
		return HelperFunctions.getRandomValueFromHashSet(freeSeats);
	}

	//DELETE FROM reservations WHERE passenger_id=passengerId AND seat_id=reservation.value AND flight_id=reservation.key

	/**
	 * Deletes a booking
	 * @param transaction
	 * @return true
     */
	protected static boolean deleteReservationByFlightAndPassengerId(DBTransaction transaction){
		Integer passengerId = transaction.getPassengerId();
		Integer flightId = transaction.getFlightId();
    	
		TreeMap<Integer, Integer> reservationsByPassengerId =
				getReservationsByPassengerId(transaction.getPassengerId());

		if(reservationsByPassengerId == null || reservationsByPassengerId.size() == 0) {
			transaction.setSuccess(true);
		}
		else{

			TreeMap<Integer, Integer> reservationsByPassengerIdCopy =
					new TreeMap<>(reservationsByPassengerId);

			if(reservationsByPassengerIdCopy.remove(flightId) != null){
				//Todo locking hier setzenund überprüfen ob eines existiert
				DBStructure.getReservations().get(passengerId).remove(flightId);
			}

			transaction.setSuccess(true);
		}

    	return true;
    }

    //SELECT * FROM reservations;
	protected static TreeMap<Integer, TreeMap<Integer, Integer>> getAllReservations(){
    	TreeMap<Integer, TreeMap<Integer, Integer>> reservations = DBStructure.getReservations();
        return reservations;
    }

    //SELECT * FROM seats WHERE flight_id = flightId;
	protected static HashSet<Integer> getSeatsPerFlightId(Integer flightId){
    	HashSet<Integer> seats = DBStructure.getSeatsPerFlightId().get(flightId);
        return seats;
    }

    //SELECT * FROM  flights;
	protected static HashSet<Integer> getAllFlights(DBTransaction transaction){
    	HashSet<Integer> flights = DBStructure.getFlights();
        return flights;
    }

    //SELECT * FROM passengers;
	protected static HashSet<Integer> getAllPassengers(DBTransaction transaction){
        HashSet<Integer> passengers = DBStructure.getPassengers();
    	return passengers;
    }
}
