package com.zorm.transaction;

import com.zorm.jdbc.JdbcServices;
import com.zorm.service.TransactionFactory;
import com.zorm.service.jta.platform.spi.JtaPlatform;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.stat.StatisticsImplementor;

public interface TransactionEnvironment {
	/**
	 * Retrieve the session factory for this environment.
	 *
	 * @return The session factory
	 */
	public SessionFactoryImplementor getSessionFactory();

	/**
	 * Retrieve the JDBC services for this environment.
	 *
	 * @return The JDBC services
	 */
	public JdbcServices getJdbcServices();

	/**
	 * Retrieve the JTA platform for this environment.
	 *
	 * @return The JTA platform
	 */
	public JtaPlatform getJtaPlatform();

	/**
	 * Retrieve the transaction factory for this environment.
	 *
	 * @return The transaction factory
	 */
	public TransactionFactory getTransactionFactory();

	/**
	 * Get access to the statistics collector
	 *
	 * @return The statistics collector
	 */
	public StatisticsImplementor getStatisticsImplementor();
}
