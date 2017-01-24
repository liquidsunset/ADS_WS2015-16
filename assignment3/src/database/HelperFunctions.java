package database;

import java.util.Random;
import java.util.Set;

public class HelperFunctions {

    /**
     * Returns a random chosen value from a HashSet
     * @param setOfData
     * @return random Integer, -1 if there is no data available
     */
    public static Integer getRandomValueFromHashSet(Set<Integer> setOfData){

        if(setOfData == null || setOfData.size() == 0)
            return -1;

        int size = setOfData.size();
        int item = new Random().nextInt(size);
        int i = 0;
        for(Integer obj : setOfData)
        {
            if (i == item)
                return obj;
            i = i + 1;
        }

        return -1;
    }

    /**
     * Function for printing the transactions
     * @param transaction
     */
    public static synchronized void printTransaction(DBTransaction transaction){
        switch (transaction.getTypeValue()){
            case 0:
                if(transaction.isSuccess()){
                    System.out.println("AgentID: " + transaction.getAgentId() + " book-transaction succeeded");
                }
                else{
                    System.out.println("AgentID: " + transaction.getAgentId() + " book-transaction failed");
                }
                break;
            case 1:
                if(transaction.isSuccess()){
                    System.out.println("AgentID: " + transaction.getAgentId() + " cancel-transaction succeeded");
                }
                else{
                    System.out.println("AgentID: " + transaction.getAgentId() + " cancel-transaction failed");
                }
                break;
            case 2:
                if(transaction.isSuccess()){
                    System.out.println("AgentID: " + transaction.getAgentId() + " myFlights-transaction succeeded");
                    if(transaction.getMyFlights() == null){
                        System.out.println("transaction succeded but we have no flight-Ids found for this passenger");
                    }
                    else{
                        System.out.println("myFlights-IDs: " + transaction.getMyFlights().toString());
                    }
                }
                else{
                    System.out.println("AgentID: " + transaction.getAgentId() + " myFlights-transaction failed");
                }
                break;
            case 3:
                if(transaction.isSuccess()){
                    System.out.println("AgentID: " + transaction.getAgentId() + " allreservations-transaction succeeded");
                    System.out.println("allReservations count: " + transaction.getTotalResults());
                }
                else{
                    System.out.println("AgentID: " + transaction.getAgentId() + " allreservations-transaction failed");
                }
                break;
            case 4:
                if(transaction.isSuccess()){
                    System.out.println("AgentID: " + transaction.getAgentId() + " allFlights-transaction succeeded");
                }
                else{
                    System.out.println("AgentID: " + transaction.getAgentId() + " allFlights-transaction failed");
                }
                break;
            case 5:
                if(transaction.isSuccess()){
                    System.out.println("AgentID: " + transaction.getAgentId() + " allPassengers-transaction succeeded");
                }
                else{
                    System.out.println("AgentID: " + transaction.getAgentId() + " allPassengers-transaction failed");
                }
                break;
            default:
                System.out.println("AgentID: " + transaction.getAgentId() + " transaction failed");
        }

    }
}
