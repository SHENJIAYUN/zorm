package com.zorm.engine;

import java.io.Serializable;
import java.sql.Connection;

import com.zorm.session.SessionFactoryImplementor;

public class ConnectionObserverStatsBridge implements ConnectionObserver, Serializable{
	private final SessionFactoryImplementor sessionFactory;

	public ConnectionObserverStatsBridge(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public void physicalConnectionObtained(Connection physicalConnection) {
		if(sessionFactory.getStatistics().isStatisticsEnabled()){
			
		}
	}

	@Override
	public void physicalConnectionReleased() {
		
	}

	@Override
	public void logicalConnectionClosed() {
		
	}
}
