package database;

import java.util.HashSet;
import java.util.TreeMap;

public class ConcurrencyControlManager {
	public static boolean serial = true;	//defines the behavior of our manager
									//false means two phase locking
	
	public static long TIMEOUT_IN_MILLISECONDS = 1*1000;	//time after witch the manager returns a null for lock
															//only for 2PL
	
    public enum LockType{
        S("S"), X("X"), IX("IX"), IS("IS"), SIX("SIX");
        String name;
        DBStructure.Table table;
        Integer row;
        TreeMap<Integer,Integer> reservationRow;	//for reservations table
        LockType(String name) {
            this.name = "LOCK-" + name;
        }
    }
    
    public enum LockStatus {
    	OPEN, LOCKED, REMOVED
    }

    public static class Lock {
    	LockType type;
    	LockStatus status;
    	HashSet<Integer> agentIDs;
    	DBStructure.Table table;
    	Integer row;
    	Integer flightId;	//For reservations and seats table
    	public Lock(LockType type) {
    		this.type = type;
    		status = LockStatus.OPEN;
    		agentIDs = new HashSet<>();
    		row = null;
    		flightId = null;
    	}
    	public Lock(){
    		status = LockStatus.OPEN;
    		this.agentIDs = new HashSet<>();
    		row = null;
    		flightId = null;
    	}
    }
    
    //Since all relevant information is located in the Lock object, we
    //just need a HashSet of them. A full iteration is needed anyway
    //A table lock-x conflicts with anything after all and row locks can conflict with
    //table locks.
    private static HashSet<Lock> flightLocks = new HashSet<>();
    private static HashSet<Lock> seatLocks = new HashSet<>();
    private static HashSet<Lock> passengersLocks = new HashSet<>();
    private static HashSet<Lock> reservationLocks = new HashSet<>();
    private static TreeMap<Integer, HashSet<Lock>> locksPerTransaction = new TreeMap<>();
    
    //Table locks
    private static Lock flightsTableLock = new Lock();
    private static Lock seatsTableLock = new Lock();
    private static Lock passengersTableLock = new Lock();
    private static Lock reservationsTableLock = new Lock();

    private static final Lock databaseLock = new Lock(LockType.X);	//global lock, no transaction during another transaction

    /**
     * Compare compatibility of two lock types. The values represent the compatibility matrix
     * taken from the lecture.
     * @param type1
     * @param type2
     * @return Returns a boolean value if anotherType is compatible with another lock type.
     */
    private static boolean isCompatible(LockType type1, LockType type2){
    	boolean compatible = false;
    	switch(type1){
		case IS:
			if(type2 == LockType.IS || type2 == LockType.IX || type2 == LockType.S || type2 == LockType.SIX)
				compatible = true;
			break;
		case IX:
			if(type2 == LockType.IS || type2 == LockType.IX)
				compatible = true;
			break;
		case S:
			if(type2 == LockType.IS || type2 == LockType.S)
				compatible = true;
			break;
		case SIX:
			if(type2 == LockType.IS)
				compatible = true;
			break;
		case X:
		default:
			break;
    	}
    	return compatible;
    }
    
    /**
     * Determine which lock type a table should be locked with.
     * @param wantedType Either LOCK-S or LOCK-X
     * @param tableLock Does the transaction want to read/alter a whole table?
     * @return
     */
    private static LockType determineWantedTableLockType(LockType wantedType, boolean tableLock){
    	LockType wantedTableLockType = LockType.X;
    	if(wantedType == LockType.S){	//transaction wants to read something
    		if(tableLock){	//transaction actually wants to read a whole table
    			wantedTableLockType = LockType.S;
    		} else {		//transaction wants to read parts of a table
    			wantedTableLockType = LockType.IS;
    		}
    	} else if(wantedType == LockType.X){		//Transaction wants to write
    		if(tableLock) {	//transaction wants to e.g. drop a table
    			wantedTableLockType = LockType.X;
    		} else {
    			wantedTableLockType = LockType.IX;
    		}
    	} else	//Intentional lock is wanted so set intentional lock on table
    		wantedTableLockType = wantedType;
    	return wantedTableLockType;
    }
    
    private static synchronized void lockDatabase(int myId) {		//at some point we have to synchronize
    	//System.out.println("Agent " + myId + " entered locking at " + System.currentTimeMillis()/1000);
    	while(true) {
	    	synchronized(databaseLock) {
		    	if(!(databaseLock.status == LockStatus.OPEN)) continue;		//let another thread enter here
		    	databaseLock.status = LockStatus.LOCKED;		//because of synchronized only one thread can enter here -> serial schedule
		    	//if(databaseLock.agentIDs.size() != 0)
		    		//System.out.println("Agent " + myId + " can start transaction although another is still working!");
		    	databaseLock.agentIDs.add(myId);
		    	//System.out.println("Agent " + myId + " locked database at " + System.currentTimeMillis()/1000);
		    	break;
	    	}
    	}
    }
    private static void unlockDatabase(int myId) {
    	while(true) {
    		synchronized(databaseLock) {
	    		//if(!databaseLock.open && databaseLock.agentIDs.size() == 1 &&
	    			//HelperFunctions.getRandomValueFromHashSet(databaseLock.agentIDs) == myId) { //== myID of course
		    		databaseLock.agentIDs.remove(myId);
		    		databaseLock.status = LockStatus.OPEN;
		    		//System.out.println("Agent " + myId + " unlocked database at " + System.currentTimeMillis()/1000);
		    	//} else
		    		//System.out.println("agent " + myId + " unlocks database: lock status " +databaseLock.status.name() + " size: " + databaseLock.agentIDs.size() + 
		    		//		" id: " + HelperFunctions.getRandomValueFromHashSet(databaseLock.agentIDs));
	    		break;
	    	}
    	}
    }
    /**
     * This method saves the locks on a per transaction basis, which is important for the
     * shrinking phase.
     * @param lock
     * @param transaction
     */
    private static void registerLock(Lock lock, DBTransaction transaction) {
    	synchronized(locksPerTransaction){
	    	if(locksPerTransaction.containsKey(transaction.getAgentId()))
	    		locksPerTransaction.get(transaction.getAgentId()).add(lock);
	    	else {
	    		HashSet<Lock> lockSet = new HashSet<>();
	    		lockSet.add(lock);
	    		locksPerTransaction.put(transaction.getAgentId(), lockSet);
	    	}
    	}
    }
    /**
     * The following two functions simulate our locking mechanisms, serial and two phase locking.
     * Every method has a phase of acquiring and freeing of a lock. For serial method we use
     * the global glock "databaseLock" whereas for two phase locking the locks are mapped for each
     * table to a row integer/number.
     * 
     * This method in particular unlocks a lock.
     * @param transaction
     * @return Returns either a corresponding lock or null. 
     */
    protected static void lockManagementShrinking(DBTransaction transaction) {
    	if(serial) {
    		unlockDatabase(transaction.getAgentId());
    	} else {
    		if(locksPerTransaction.containsKey(transaction.getAgentId())) {
    			HashSet<Lock> myLocks;
    			synchronized(locksPerTransaction){
    				myLocks = locksPerTransaction.get(transaction.getAgentId());
    			}
    			for(Lock lock: myLocks) {
    				if(lock.row != null){
	    				//To avoid concurrent modifications, synchronize on lock sets
	    				switch(lock.table) {
						case FLIGHTS:
							synchronized(ConcurrencyControlManager.flightLocks) {
								flightLocks.remove(lock);
							}
							break;
						case PASSENGERS:
							synchronized(ConcurrencyControlManager.passengersLocks) {
								passengersLocks.remove(lock);
							}
							break;
						case RESERVATIONS:
							synchronized(ConcurrencyControlManager.reservationLocks) {
								reservationLocks.remove(lock);
							}
							break;
						case SEATS:
							synchronized(ConcurrencyControlManager.seatLocks) {
								seatLocks.remove(lock);
							}
							break;
						default:
							break;
	    				
	    				}
	    				//Now set lock status to removed
	    				synchronized(lock){
	    					lock.status = LockStatus.REMOVED;
	    				}
    				} else {		//Table lock
    					synchronized(lock){
    						//Only remove / free /open up lock if transaction is an owner
    						if(lock.agentIDs.contains(transaction.getAgentId())){
	    						lock.agentIDs.remove(transaction.getAgentId());
	    						if(lock.agentIDs.size() == 0){
	    							lock.status = LockStatus.OPEN;
	    						}
    						}
    					}
    				}
    			}
    			myLocks.clear();
    			/*synchronized(locksPerTransaction) {
    				locksPerTransaction.remove(transaction.getAgentId());
    			}*/
    		} else
    			System.out.println("No locks for transaction " + transaction.toString());
    	}
    }
    /**
     * This method locks based on serial or 2pl schedule/mechanism. In case a conflicting lock exists, the calling
     * thread will wait.
     * @param transaction The transaction for whom a lock is acquired.
     * @param table The table this lock is for
     * @param row The row in the table to be locked.
     * @param flightId If the table is reservations or seats, then a mapping is needed for row identification.
     * @param wantedType The wanted lock type (LOCK-S, LOCK-X)
     * @return The acquired lock
     */
    protected static Lock lockManagementGrowing(DBTransaction transaction, DBStructure.Table table, Integer row,
    		Integer flightId, LockType wantedType) {
    	Lock correspondingLock = null;
    	if(serial){ 
    		if(table == null) {		//Normal, starting case, transaction started
    			lockDatabase(transaction.getAgentId());
    			correspondingLock = databaseLock;
    		} else
    			return databaseLock;
    	} else if(table != null){
    		//This condition has to be here for differentiating between serial and 2pl
    		//The 2pl mechanism has no fixed starting point whereas serial schedule does (global lock)
    		Lock tableLock = getTableLock(table);
    		LockType wantedTableLockType = determineWantedTableLockType(wantedType, row == null);
    		long timeout = System.currentTimeMillis() + TIMEOUT_IN_MILLISECONDS;
    		//First try to lock table
    		while(true){
    			if(System.currentTimeMillis() >= timeout)
    				return null;
    			synchronized(tableLock){
    				if(tableLock.status == LockStatus.OPEN){
    					tableLock.status = LockStatus.LOCKED;
    					tableLock.agentIDs.add(transaction.getAgentId());
    					tableLock.type = wantedTableLockType;
    					registerLock(tableLock,transaction);
    					break;
    				} else if(ConcurrencyControlManager.isCompatible(wantedTableLockType, tableLock.type)) {
    					tableLock.agentIDs.add(transaction.getAgentId());
    					if(wantedTableLockType != tableLock.type)		//Switch to higher granularity, granularity escalation
    						tableLock.type = (tableLock.type == LockType.IX)? LockType.X: LockType.S;
    					registerLock(tableLock,transaction);
    					break;
    				} else {
    					continue;
    				}
    			}
    		}
    		HashSet<Lock> lockSet = getLockSetCopy(table);
    		HashSet<Lock> conflictingLocks = new HashSet<>();
    		if(row == null && flightId == null) {	//table lock
    			if(wantedType == LockType.X) {				//No other locks allowed
    				conflictingLocks.addAll(lockSet);
    			}
				correspondingLock = tableLock;
    		} else {
	    		for(Lock existingLock: lockSet) {
	    			synchronized(existingLock) {
		    			//Now check if existing Lock conflicts and gather those
		    			if(existingLock.row == row){	//same tuple
		    				if(flightId!=null){
		    					if(table != DBStructure.Table.RESERVATIONS && table != DBStructure.Table.SEATS)
		    						System.out.println("Hm, we want to lock something in the reservations or seats table, but from table " + table + " in actual? Strange...");
		    					//For reservations, the flight id still has to be the same for the same row/tuple
		    					if(existingLock.flightId == flightId && existingLock.type == LockType.X)
		    						conflictingLocks.add(existingLock);
		    				} else if(!isCompatible(wantedType, existingLock.type)){
		    					conflictingLocks.add(existingLock);
		    				}
		    			}
	    			}
	    		}
    		}
    		//Transaction has to be blocked while conflicting locks are up
    		timeout = System.currentTimeMillis() + TIMEOUT_IN_MILLISECONDS;
    		for(Lock conflictingLock: conflictingLocks) {
    			while(true){
    				if(System.currentTimeMillis() >= timeout){	//aetsch, timeout -> no lock for you
    					return null;		//Could not get the lock in the specified timeout interval
    				}
    				synchronized(conflictingLock){
    					if(conflictingLock.status != LockStatus.OPEN && conflictingLock.status != LockStatus.REMOVED)
    						continue;
    				}
    			}
    		}
    		if(row != null){		//Do not do this for a table lock
	    		lockSet = getLockSet(table);
	    		synchronized(lockSet){
	    			correspondingLock = new Lock();
	    			correspondingLock.row = row;
	    			if(flightId != null)
	    				correspondingLock.flightId = flightId;
	    			correspondingLock.table = table;
	    			correspondingLock.type = wantedType;
	    			lockSet.add(correspondingLock);
	    		}
	    		registerLock(correspondingLock, transaction);
    		}
    	}
    	return correspondingLock;
    }
    
    /**
     * This method returns a table lock and has the simple and single reason to reduce the amount of source code lines.
     * @param table
     * @return
     */
    private static Lock getTableLock(DBStructure.Table table){
    	switch(table){
		case FLIGHTS:
			return flightsTableLock;
		case PASSENGERS:
			return passengersTableLock;
		case RESERVATIONS:
			return reservationsTableLock;
		case SEATS:
			return seatsTableLock;
		default:
			return null;
    	
    	}
    }
    
    /**
     * This method returns a new lock set with the same object references as in the
     * original lock set.
     * 
     * By doing this, we can avoid all those synchronized accesses on the lock sets,
     * so acquisition will be faster and concurrent modifications are less probable.
     * @param table
     * @return
     */
    private static HashSet<Lock> getLockSetCopy(DBStructure.Table table) {
    	HashSet<Lock> lockSet;
    	switch (table) {
		case FLIGHTS:
			//We still have to synchronize here though, however, we may have to iterate more than once
			//in the growth function.
			synchronized(ConcurrencyControlManager.flightLocks) {
				lockSet = new HashSet<>(flightLocks);
				for(Lock l: flightLocks) {
					lockSet.add(l);
				}
			}
			break;
		case PASSENGERS:
			synchronized(ConcurrencyControlManager.passengersLocks) {
				lockSet = new HashSet<>();
				for(Lock l: ConcurrencyControlManager.passengersLocks) {
					lockSet.add(l);
				}
			}
			break;
		case RESERVATIONS:
			synchronized(ConcurrencyControlManager.reservationLocks) {
				lockSet = new HashSet<>();
				for(Lock l: ConcurrencyControlManager.reservationLocks) {
					lockSet.add(l);
				}
			}
			break;
		case SEATS:
			synchronized(ConcurrencyControlManager.seatLocks) {
				lockSet = new HashSet<>();
				for(Lock l: ConcurrencyControlManager.seatLocks) {
					lockSet.add(l);
				}
			}
			break;
		default:
			return null;
    	}
    	return lockSet;
    }
    
    /**
     * Same as above, just no separate copy.
     * 
     * By doing this, we can avoid all those synchronized accesses on the lock sets,
     * so acquisition will be faster and concurrent modifications are less probable.
     * @param table
     * @return
     */
    private static HashSet<Lock> getLockSet(DBStructure.Table table) {
    	switch (table) {
		case FLIGHTS:
			return ConcurrencyControlManager.flightLocks;
		case PASSENGERS:
			return ConcurrencyControlManager.passengersLocks;
		case RESERVATIONS:
			return ConcurrencyControlManager.reservationLocks;
		case SEATS:
			return ConcurrencyControlManager.seatLocks;
		default:
			return null;
    	}
    }
}
