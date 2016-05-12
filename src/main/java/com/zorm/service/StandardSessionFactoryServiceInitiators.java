package com.zorm.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.zorm.session.SessionFactoryServiceInitiator;


public class StandardSessionFactoryServiceInitiators {
	public static List<SessionFactoryServiceInitiator> LIST = buildStandardServiceInitiatorList();

	//添加sessionFactory服务
	private static List<SessionFactoryServiceInitiator> buildStandardServiceInitiatorList() {
		final List<SessionFactoryServiceInitiator> serviceInitiators = new ArrayList<SessionFactoryServiceInitiator>();

		serviceInitiators.add( EventListenerServiceInitiator.INSTANCE );
		serviceInitiators.add( StatisticsInitiator.INSTANCE );
		//serviceInitiators.add( CacheInitiator.INSTANCE );

		return Collections.unmodifiableList( serviceInitiators );
	}
}
