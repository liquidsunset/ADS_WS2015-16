package main;

import database.*;

import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

public class Assignment3 {

	public static final int MAXFLIGHTIDS = 15;
	public static final int MAXPASSENGERIDS = 1500;
	public static final int MINSEATCOUNT = 50;
	public static final int MAXSEATCOUNT = 251;
	public static final int MAXTRANSACTIONS = 100;

	private static int transactionsFailed = 0;
	private static int transactionsAmount = 0;

	public static int agentCount = 20;
	
	public static void main(String[] args) {

		ConcurrencyControlManager.serial = true;
		
		initDatabase();

		DBTransaction allFlightsTransaction = TransactionCreator.getAllFlightsTransaction(0);
		DBTransaction allPassengersTransaction = TransactionCreator.getAllPassengersTransaction(0);

		DBConnection.exec(allFlightsTransaction, false);
		DBConnection.exec(allPassengersTransaction, false);

		if(allFlightsTransaction.isSuccess() && allPassengersTransaction.isSuccess()){
			System.out.println("Serial test ================");
			testAgentCount(allFlightsTransaction, allPassengersTransaction);
			ConcurrencyControlManager.serial = false;
			System.out.println("2PL test ===================");
			testAgentCount(allFlightsTransaction, allPassengersTransaction);
			System.out.println("We had a total of " + DBConnection.exceptionCount() + " exception(s)!");
		} else
			System.err.println("Cannot continue since main could not fetch passengers and/or flights");
		
	}
	
	private static void testAgentCount(DBTransaction allFlightsTransaction, DBTransaction allPassengersTransaction){
		System.out.println("Varying agent count");
		initDatabase();
		for(int agentCount = 2; agentCount<=10; agentCount+=2) {
			Agent[] agents = new Agent[agentCount];
			int transForAllAgents = 0;
			for (int i = 0; i < agentCount; i++) {
				int transactionsForAgent = MAXTRANSACTIONS / agentCount;

				if(transForAllAgents!= MAXTRANSACTIONS && i == agentCount - 1){
					transactionsForAgent = MAXTRANSACTIONS - transForAllAgents;
				}
				transForAllAgents += transactionsForAgent;

				agents[i] = new Agent(i, TransactionCreator.randomTransSequence(transactionsForAgent, i,
						allPassengersTransaction.getAllPassengers(), allFlightsTransaction.getAllFlights()));
			}
			long startTime = System.currentTimeMillis();
			for(int i = 0; i < agentCount; i++) {
				agents[i].start();
			}
			for (int i = 0; i < agentCount; i++) {
				try {
					agents[i].join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			long endTime = System.currentTimeMillis();
			System.out.println("agentCount " + agentCount + " runtime[ms] " + (endTime-startTime) + " throughput[1/s] " +
			(MAXTRANSACTIONS *agentCount*1000)/(endTime-startTime) + " Failed-Transactions: " + transactionsFailed +
					" Transactions/Amount: " + transactionsAmount);
			transactionsAmount = 0;
			transactionsFailed = 0;
		}
	}

	private static void initDatabase(){

		DBStructure.clearDataStructures();
		
		//init the flight ids
		for(int i = 1; i <= MAXFLIGHTIDS; i++){
			DBStructure.getFlights().add(i);
		}

		//init the passenger ids
		for(int i = 1; i <= MAXPASSENGERIDS; i++){
			DBStructure.getPassengers().add(i);
		}

		HashSet<Integer> seatIds = new HashSet<>();
		Integer seatCount;

		//init the max Seats per flight
		for(Integer flightID : DBStructure.getFlights()){
			seatIds.clear();
			seatCount = ThreadLocalRandom.current().nextInt(MINSEATCOUNT, MAXSEATCOUNT);
			for(int i = 1; i <= seatCount; i++){
				seatIds.add(i);
			}

			DBStructure.getSeatsPerFlightId().put(flightID, seatIds);
		}
	}

	public static synchronized void setTransCount(int transFailed, int transAmount){
		transactionsFailed += transFailed;
		transactionsAmount += transAmount;
	}



}
