package reservation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import reservation.FlightSeatReservation.IsolationLevels;

public class DataLoader {
	
	public static PrintWriter statistic = null;
	
	public static void setupTable(Connection con) throws SQLException {
		System.out.print("Dropping and recreation of table flight_seats: ");
		Statement s = con.createStatement();
		try {
			s.execute("DROP TABLE flight_seats");
			System.out.print("Dropped table flight_seats");
		} catch (Throwable t) {}
		//flight_seats[id, availability]
		s.execute("CREATE TABLE flight_seats(id integer not null, availability integer, PRIMARY KEY(id))");
		con.setAutoCommit(false);
		PreparedStatement insertSeat = con.prepareStatement("INSERT INTO flight_seats(id,availability) VALUES (?,?)");
		for(int i = 1; i<=FlightSeatReservation.nrOfCustomers; i++) {
				insertSeat.setInt(1, i);
				insertSeat.setInt(2, 1);
				insertSeat.executeUpdate();
				con.commit();
		}
		con.setAutoCommit(true);
		s.execute("ALTER TABLE flight_seats LOCKSIZE ROW");
		s.close();
		System.out.println("   +++DONE+++");
	}
	
	public static void printStatistic(long runTime, boolean failure, int nrOfThreads, IsolationLevels isolationLevel, 
			Connection con, boolean splittedBooking) {
		if(statistic != null) {
			statistic.println((splittedBooking?"splitted":"one-transaction") + ":::nrOfThreads: " +nrOfThreads + ", runtime: " + runTime +
					", isolation level: " + isolationLevel.name + ", db2 name: " + isolationLevel.db2Name);
			statistic.flush();
			PrintWriter table;
			try {
				table = new PrintWriter("results"+File.separator+"resultTable-"+nrOfThreads+"-"+isolationLevel.db2Name+
						(splittedBooking?"-split":""));
				if(failure) {
					table.write("Error/failure");
				} else {
					try {
						Statement s = con.createStatement();
						ResultSet rs = s.executeQuery("SELECT * FROM flight_seats");
						while(rs.next())
							table.println("  (" + rs.getString(1) + "," + rs.getString(2).equals("1") + ")");
						s.close();
					} catch (SQLException e) {
						System.err.println("Problem with statistic reading: " + e.getLocalizedMessage());
						e.printStackTrace();
					}
				}
				table.flush();
				table.close();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}

		}
	}
}
