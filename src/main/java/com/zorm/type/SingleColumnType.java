package com.zorm.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.zorm.exception.ZormException;
import com.zorm.session.SessionImplementor;

public interface SingleColumnType<T> extends Type{
	public int sqlType();

	public String toString(T value) throws ZormException;

	public T fromStringValue(String xml) throws ZormException;
	
	/**
	 * Get a column value from a result set by name.
	 *
	 * @param rs The result set from which to extract the value.
	 * @param name The name of the value to extract.
	 * @param session The session from which the request originates
	 *
	 * @return The extracted value.
	 *
	 * @throws org.hibernate.HibernateException Generally some form of mismatch error.
	 * @throws java.sql.SQLException Indicates problem making the JDBC call(s).
	 */
	public T nullSafeGet(ResultSet rs, String name, SessionImplementor session) throws ZormException, SQLException;

	/**
	 * Get a column value from a result set, without worrying about the possibility of null values.
	 *
	 * @param rs The result set from which to extract the value.
	 * @param name The name of the value to extract.
	 * @param session The session from which the request originates
	 *
	 * @return The extracted value.
	 *
	 * @throws org.hibernate.HibernateException Generally some form of mismatch error.
	 * @throws java.sql.SQLException Indicates problem making the JDBC call(s).
	 */
	public Object get(ResultSet rs, String name, SessionImplementor session) throws ZormException, SQLException;

	/**
	 * Set a parameter value without worrying about the possibility of null
	 * values.  Called from {@link #nullSafeSet} after nullness checks have
	 * been performed.
	 *
	 * @param st The statement into which to bind the parameter value.
	 * @param value The parameter value to bind.
	 * @param index The position or index at which to bind the param value.
	 * @param session The session from which the request originates
	 *
	 * @throws org.hibernate.HibernateException Generally some form of mismatch error.
	 * @throws java.sql.SQLException Indicates problem making the JDBC call(s).
	 */
	public void set(PreparedStatement st, T value, int index, SessionImplementor session) throws ZormException, SQLException;
}
