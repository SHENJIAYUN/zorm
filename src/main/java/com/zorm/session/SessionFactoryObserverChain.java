package com.zorm.session;

import java.util.ArrayList;
import java.util.List;

public class SessionFactoryObserverChain implements SessionFactoryObserver{
	private List<SessionFactoryObserver> observers;

	public void addObserver(SessionFactoryObserver observer) {
		if ( observers == null ) {
			observers = new ArrayList<SessionFactoryObserver>();
		}
		observers.add( observer );
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		if ( observers == null ) {
			return;
		}

		for ( SessionFactoryObserver observer : observers ) {
			observer.sessionFactoryCreated( factory );
		}
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
		if ( observers == null ) {
			return;
		}

		//notify in reverse order of create notification
		int size = observers.size();
		for (int index = size - 1 ; index >= 0 ; index--) {
			observers.get( index ).sessionFactoryClosed( factory );
		}
	}
}
