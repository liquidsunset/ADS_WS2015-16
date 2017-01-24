/*
 * Example code for Assignment 6 (concurrency tuning) of the course:
 * 
 * Database Tuning
 * Department of Computer Science
 * University of Salzburg, Austria
 * 
 * Lecturer: Nikolaus Augsten
 */
package reservation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Random;

import reservation.FlightSeatReservation.IsolationLevels;

/** 
 * Dummy transaction that prints a start message, waits for a random time 
 * (up to 100ms) and finally prints a status message at termination.
 */
public class TravelAgent extends Thread {

	// identifier of the transaction
	int id;
	int minCustomer;
	int maxCustomer;
	IsolationLevels isolationLevel;
	boolean splittedBooking;
	
	PreparedStatement selectAvailable;
	PreparedStatement updateSeats;
	
	TravelAgent(int id, int minCustomer, int maxCustomer, IsolationLevels isolationLevel, boolean splittedBooking) {
		this.id = id;
		this.minCustomer = minCustomer;
		this.maxCustomer = maxCustomer;
		this.isolationLevel = isolationLevel;
		this.splittedBooking = splittedBooking;
	}
	
	public void setIsolationLevel(IsolationLevels isolationLevel) {
		this.isolationLevel = isolationLevel;
	}
	
	private void book(Connection con, int customerID) {
		//1. Retrieve list of available seats (selection of available seat ids).
		try {
			try {
				ResultSet rs = selectAvailable.executeQuery();
				ArrayList<Integer> ids = new ArrayList<>();
				while(rs.next())
					ids.add(rs.getInt(1));
				if(ids.isEmpty())
					return;
				//2. Give the customer some time (decision time is 1 second) to decide on a seat (a random seat id from the list returned in point 1).
				//Thread.sleep(1000);		//not allowed
				long startTime = System.currentTimeMillis();
				while((System.currentTimeMillis()-startTime) < 1000);	//busy waiting
				Random r = new Random();
				int seat = ids.get(r.nextInt(ids.size()));
				//System.out.println(seat + "asdfasdfasdfasdfasdfadsf");
				//3. Secure a seat (update the availability of the chosen seat to false).
				try {
					updateSeats.setInt(1, seat);
					updateSeats.executeUpdate();
					//FlightSeatReservation.customers.set(customerID, true);
					FlightSeatReservation.customers[customerID] = true;
				} catch(SQLException e) { con.rollback(); }//System.out.println("3------------------------------------------------------");}
			} catch(SQLException e) { con.rollback(); }//System.out.println("2------------------------------------------------------");}
			con.commit();
		} catch (SQLException e) {}//System.out.println("1------------------------------------------------------");
	}
	
	private void bookSplitted(Connection con, int customerID) {
		//1. Retrieve list of available seats (selection of available seat ids).
		try {
			try {
				ResultSet rs = selectAvailable.executeQuery();
				ArrayList<Integer> ids = new ArrayList<>();
				while(rs.next())
					ids.add(rs.getInt(1));
				con.commit();
				//2. Give the customer some time (decision time is 1 second) to decide on a seat (a random seat id from the list returned in point 1).
				//Thread.sleep(1000);		//not allowed
				long startTime = System.currentTimeMillis();
				while((System.currentTimeMillis()-startTime) < 1000);	//busy waiting
				Random r = new Random();
				int seat = ids.get(r.nextInt(ids.size()));
				//System.out.println(seat + "asdfasdfasdfasdfasdfadsf");
				try {
					try {
						//3. Secure a seat (update the availability of the chosen seat to false).
						updateSeats.setInt(1, seat);
						updateSeats.executeUpdate();
						con.commit();
						//FlightSeatReservation.customers.set(customerID, true);
						FlightSeatReservation.customers[customerID] = true;
					} catch (SQLException e) { con.rollback(); }
				} catch (SQLException e) { con.rollback(); }//System.out.println("3------------------------------------------------------");}
			} catch(SQLException e) { con.rollback(); }//System.out.println("2------------------------------------------------------");}
		} catch (SQLException e) {}//System.out.println("1------------------------------------------------------");}
	}
	
	@Override
	public void run() {
		System.out.println("Flight seat booking agent " + id + " has started!");
		try {
			Connection con = DriverManager.getConnection(FlightSeatReservation.url);
			con.setAutoCommit(false);
			con.setTransactionIsolation(isolationLevel.jdbcValue());
			/*Statement st = con.createStatement();
			try {
				st.execute("SET CURRENT ISOLATION " + isolationLevel.db2Name);
			} catch(SQLException e) {
				st.close();
				con.close();
				System.err.println("Thread " + id + " cannot set isolation level! Aborting!");
				return;
			}
			st.close();*/
			this.selectAvailable = con.prepareStatement("SELECT id FROM flight_seats WHERE availability=1");
			this.updateSeats = con.prepareStatement("UPDATE flight_seats SET availability=0 WHERE id=?");
			for(int i = minCustomer; i<maxCustomer; i++) {
				long tries = 0;
				//retry as long as reservation did not work
				while(!FlightSeatReservation.customers[i]) {
					if(splittedBooking)
						bookSplitted(con, i);
					else
						book(con, i);
					tries++;
				}
				FlightSeatReservation.bookingTimesPerCustomer[i] = tries;
			}
			con.close();
		} catch (SQLException e) {
			System.err.println("Thread " + id + " could not connect to database!");
			e.printStackTrace();
		}

		System.out.println(id + " has finished!");
	}
}