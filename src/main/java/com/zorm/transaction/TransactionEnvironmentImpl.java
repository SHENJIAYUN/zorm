package com.zorm.transaction;

import com.zorm.jdbc.JdbcServices;
import com.zorm.service.JtaPlatform;
import com.zorm.service.ServiceRegistry;
import com.zorm.service.TransactionFactory;
import com.zorm.session.SessionFactoryImpl;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.stat.StatisticsImplementor;

public class TransactionEnvironmentImpl implements TransactionEnvironment {
	 private final SessionFactoryImpl sessionFactory;
	    private final transient StatisticsImplementor statisticsImplementor;
	    private final transient ServiceRegistry serviceRegistry;
	    private final transient JdbcServices jdbcServices;
	    private final transient JtaPlatform jtaPlatform;
	    private final transient TransactionFactory transactionFactory;

	    public TransactionEnvironmentImpl(SessionFactoryImpl sessionFactory) {
	        this.sessionFactory = sessionFactory;
	        this.statisticsImplementor = sessionFactory.getStatisticsImplementor();
	        this.serviceRegistry = sessionFactory.getServiceRegistry();
	        this.jdbcServices = serviceRegistry.getService( JdbcServices.class );
	        this.jtaPlatform = serviceRegistry.getService( JtaPlatform.class );
	        this.transactionFactory = serviceRegistry.getService( TransactionFactory.class );
	    }
	    
	    @Override
	    public SessionFactoryImplementor getSessionFactory() {
	        return sessionFactory;
	    }

	    protected ServiceRegistry serviceRegistry() {
	        return serviceRegistry;
	    }

	    @Override
	    public JdbcServices getJdbcServices() {
	        return jdbcServices;
	    }

	    @Override
	    public JtaPlatform getJtaPlatform() {
	        return jtaPlatform;
	    }

	    @Override
	    public TransactionFactory getTransactionFactory() {
	        return transactionFactory;
	    }

	    @Override
	    public StatisticsImplementor getStatisticsImplementor() {
	        return statisticsImplementor;
	    }
}
