package reservation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class FlightSeatReservation {

	//public static CopyOnWriteArrayList<Boolean> customers = new CopyOnWriteArrayList<>();
	//public static CopyOnWriteArrayList<Long> bookingTimesPerCustomer = new CopyOnWriteArrayList<>();
	public static final int nrOfCustomers = 200;
	public static boolean[] customers = new boolean[nrOfCustomers];
	public static long[] bookingTimesPerCustomer = new long[nrOfCustomers];
	
	public static enum IsolationLevels{
		READ_UNCOMMITED("READ_UNCOMMITED",Connection.TRANSACTION_READ_UNCOMMITTED,"UR"),
		READ_COMMITED("READ_COMMITED",Connection.TRANSACTION_READ_COMMITTED,"CS"),
		REPEATABLE_READ("REPEATABLE_READ",Connection.TRANSACTION_REPEATABLE_READ,"RS"),
		SERIALIZABLE("SERIALIZABLE",Connection.TRANSACTION_SERIALIZABLE,"RR");
		String name;
		int jdbcValue;
		String db2Name;
		IsolationLevels(String name, int val, String db2Name) {
			this.name = name;
			this.jdbcValue = val;
			this.db2Name = db2Name;
		}
		String getName() {
			return this.name;
		}
		String getDB2Name() {
			return this.db2Name;
		}
		int jdbcValue() {
			return this.jdbcValue;
		}
	}
	
	//String url = "jdbc:db2://url:port/database:"	//for server
	public static String url = "jdbc:db2:xicht";
	
	public static void main(String[] args) {
		File resultsDir = new File("results");
		if(!resultsDir.exists())
			resultsDir.mkdirs();
		try {
			// Load the IBM Data Server Driver for JDBC and SQLJ with DriverManager
			Class.forName("com.ibm.db2.jcc.DB2Driver");			
			try {
				//For local installation
				Connection con = DriverManager.getConnection(url);
				//For server/service installation
				//Connection con = DriverManager.getConnection(url,info);
				//Check for existing lines, if not present, load data
				//Now start the threads
				int[] nrOfAgents = {1,2,4,6,8,10};
				IsolationLevels[] levels = {IsolationLevels.READ_COMMITED, IsolationLevels.SERIALIZABLE};
				try {
					DataLoader.statistic = new PrintWriter("results" + File.separator + "statistic");
					for(int agentCount: nrOfAgents) {
						System.out.println("+++ Running test with " + agentCount + " travel agents +++");
						for(IsolationLevels isolationLevel: levels) {
							System.out.println("Isolation level: " + isolationLevel.getName());
							System.out.println("One transaction");
							FlightSeatReservation.test(false, agentCount, con, isolationLevel);
							System.out.println("Splitted transaction");
							FlightSeatReservation.test(true, agentCount, con, isolationLevel);
						}
					}
					DataLoader.statistic.flush();
					DataLoader.statistic.close();
				} catch (IOException e1) {
					System.err.println("Cannot create/open statistics file! Aborting!");
					e1.printStackTrace();
				}
				try {
					con.close();
				} catch (Throwable ignore) {}
			} catch (SQLException e) {
				System.err.println("Cannot connect to database: " + e.getMessage());
				e.printStackTrace();
			}
		} catch (ClassNotFoundException e) {
			System.err.println("Cannot load db2 driver: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void setArrays(){
		//customers.clear();
		for (int i = 0; i<nrOfCustomers; i++) {
			//customers.add(false);
			customers[i] = false;
			bookingTimesPerCustomer[i] = 0;
		}
	}
	
	private static void test(boolean splittedBooking, int agentCount, Connection con, 
			IsolationLevels isolationLevel) {
		try {
			DataLoader.setupTable(con);
			//bookingTimesPerCustomer.clear();
			//Create customers
			setArrays();
			TravelAgent[] agents = new TravelAgent[agentCount];
			int customersPerAgent = nrOfCustomers/agentCount;
			for(int i = 0; i<agentCount; i++)
				agents[i] = new TravelAgent(i,i*customersPerAgent,(i+1)*customersPerAgent
						+((i==(agentCount-1))?(nrOfCustomers%agentCount):0),	//add remaining customers to last thread
						isolationLevel, splittedBooking);
			long startTime = System.currentTimeMillis();
			for (int i = 0; i < agentCount; i++) {
				agents[i].start();
			}
			for(int i=0; i < agentCount; i++) {
				try {
					agents[i].join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			DataLoader.printStatistic(System.currentTimeMillis()-startTime, false, agentCount, isolationLevel, con, splittedBooking);
			//Print booking times
			if(bookingTimesPerCustomer.length > 0) {
				long minTime = Long.MAX_VALUE, maxTime = Long.MIN_VALUE;
				long avgSum = 0;
				for(Long i: bookingTimesPerCustomer) {
					avgSum += i;
					if(i > maxTime)
						maxTime = i;
					if(i < minTime)
						minTime = i;
				}
				//DataLoader.statistic.println("\tBooking times: " + avgSum/(bookingTimesPerCustomer.size()*1.0) +
				DataLoader.statistic.println("\tBooking times: " + avgSum/(bookingTimesPerCustomer.length*1.0) +
						" avg, " + minTime + " min, " + maxTime + " max");
			} else
				DataLoader.statistic.println("------------ WTF, no booking time???? ------------");
		} catch (SQLException e) {
			System.err.println("Problem with data setup: " + e.getLocalizedMessage());
			e.printStackTrace();
			DataLoader.statistic.println("\tERROR!!!!!!!!!!!!!!!!!");
		}
	}
}
