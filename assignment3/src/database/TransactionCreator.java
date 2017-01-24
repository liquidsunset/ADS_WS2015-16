package database;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class TransactionCreator {

    private static final int[] transTypes = {0,1,2,3,0,1,0,2,0};

    /**
     * Creation of the Transaction for getting all possible flight-IDs
     * @param agentId
     * @return Transaction Object for execution
     */
    public static DBTransaction getAllFlightsTransaction(Integer agentId){
        return new DBTransaction(DBTransaction.TransactionType.allFLights, agentId);
    }

    /**
     * Creation of the Transaction for getting all possible passenger-IDs
     * @param agentId
     * @return Transaction Object for execution
     */
    public static DBTransaction getAllPassengersTransaction(Integer agentId){
        return new DBTransaction(DBTransaction.TransactionType.allPassengers, agentId);
    }

    /**
     * Creation of a random transaction
     * @param agentId
     * @param passengerId
     * @param flightId
     * @return Transaction Object for execution
     */
    private static DBTransaction createTransaction(Integer agentId, Integer passengerId, Integer flightId, int type){

        switch (type){
            case 0:
                return new DBTransaction(DBTransaction.TransactionType.bookFlight, agentId, passengerId, flightId);
            case 1:
                DBTransaction myFlights = new DBTransaction(DBTransaction.TransactionType.myFlights, agentId);
                myFlights.setPassengerId(passengerId);
                DBConnection.exec(myFlights, false);
                if(myFlights.getMyFlights() != null && myFlights.getMyFlights().size() > 0){
                    return new DBTransaction(DBTransaction.TransactionType.cancelFlight, agentId,
                            passengerId, HelperFunctions.getRandomValueFromHashSet(myFlights.getMyFlights()));
                }
                else{
                    return new DBTransaction(DBTransaction.TransactionType.bookFlight, agentId, passengerId, flightId);
                }
            case 2:
                return new DBTransaction(DBTransaction.TransactionType.myFlights, agentId, passengerId, flightId);
            case 3:
                return new DBTransaction(DBTransaction.TransactionType.allReservations, agentId, passengerId, flightId);
            default:
                return new DBTransaction(DBTransaction.TransactionType.bookFlight, agentId, passengerId, flightId);
        }
    }

    public static ArrayList<DBTransaction> randomTransSequence(int amount, Integer agentID, HashSet<Integer> passengerIDs,
                                                               HashSet<Integer> flightIDs){
        ArrayList<DBTransaction> randomList = new ArrayList<>();
        Random r = new Random();

        for (int i = 0; i < amount; i++){
            Integer passengerId = HelperFunctions.getRandomValueFromHashSet(passengerIDs);
            Integer flightId = HelperFunctions.getRandomValueFromHashSet(flightIDs);
            int transType = transTypes[r.nextInt(transTypes.length)];
            randomList.add(createTransaction(agentID, passengerId, flightId, transType));
        }

        return randomList;
    }
}
