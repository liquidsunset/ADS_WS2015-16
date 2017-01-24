package main;

import database.DBConnection;
import database.DBTransaction;
import database.HelperFunctions;

import java.util.ArrayList;

public class Agent extends Thread {

	private int id;
	private ArrayList<DBTransaction> transactions;



	public Agent(int id, ArrayList<DBTransaction> transactions) {
		this.id = id;
		this.transactions = transactions;
	}
	
	@Override
	public void run() {
		int failedTrans = 0;
		for(DBTransaction transaction : transactions){
			DBConnection.exec(transaction, true);
			if(!transaction.isSuccess()){
				failedTrans ++;
				//HelperFunctions.printTransaction(transaction);
			}
			//HelperFunctions.printTransaction(transaction);
		}

		Assignment3.setTransCount(failedTrans, transactions.size());
	}

	private int countFailedTrans(){
		int failedTrans = 0;

		for(DBTransaction transaction : transactions){
			if(!transaction.isSuccess()){
				failedTrans += 1;
			}
		}

		return failedTrans;
	}
}
