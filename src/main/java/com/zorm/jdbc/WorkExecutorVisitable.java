package com.zorm.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

public interface WorkExecutorVisitable<T> {
	/**
	 * Accepts a {@link WorkExecutor} visitor for executing a discrete
	 * piece of work, and returns an implementation-defined result..
	 *
	 * @param executor The visitor that executes the work.
	 * @param connection The connection on which to perform the work.
	 *
	 * @return an implementation-defined result
	 *
	 * @throws SQLException Thrown during execution of the underlying JDBC interaction.
	 * @throws org.hibernate.HibernateException Generally indicates a wrapped SQLException.
	 */
	public T accept(WorkExecutor<T> executor, Connection connection) throws SQLException;
}
