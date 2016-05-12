package com.zorm.engine;

import java.util.LinkedHashSet;

import javax.transaction.Synchronization;

public class SynchronizationRegistryImpl implements SynchronizationRegistry{

	private LinkedHashSet<Synchronization> synchronizations;
	
	public void clearSynchronizations() {
		if ( synchronizations != null ) {
			synchronizations.clear();
			synchronizations = null;
		}
	}

	public void notifySynchronizationsBeforeTransactionCompletion() {
		if(synchronizations != null){
		}
	}
}
