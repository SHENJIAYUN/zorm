package com.zorm.engine;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import com.zorm.exception.JdbcProxyException;

public class ProxyBuilder {
	
	public static final Class[] CONNECTION_PROXY_INTERFACES = new Class[] {
		Connection.class,
		JdbcWrapper.class
    };
	
	public static final Class[] RESULTSET_PROXY_INTERFACES = new Class[] {
		ResultSet.class,
		JdbcWrapper.class,
		InvalidatableWrapper.class
   };
	
	private static final ValueHolder<Constructor<Connection>> connectionProxyConstructorValue = new ValueHolder<Constructor<Connection>>(
			new ValueHolder.DeferredInitializer<Constructor<Connection>>() {
				@Override
				public Constructor<Connection> initialize() {
					try {
						return locateConnectionProxyClass().getConstructor( InvocationHandler.class );
					}
					catch (NoSuchMethodException e) {
						throw new JdbcProxyException( "Could not find proxy constructor in JDK generated Connection proxy class", e );
					}
				}

				@SuppressWarnings("unchecked")
				private Class<Connection> locateConnectionProxyClass() {
					return (Class<Connection>) Proxy.getProxyClass(
							JdbcWrapper.class.getClassLoader(),
							CONNECTION_PROXY_INTERFACES
					);
				}
			}
	);

	
	public static Connection buildConnection(LogicalConnectionImplementor logicalConnection) {
		final ConnectionProxyHandler proxyHandler = new ConnectionProxyHandler( logicalConnection );
		try {
			return connectionProxyConstructorValue.getValue().newInstance( proxyHandler );
		}
		catch (Exception e) {
			throw new JdbcProxyException( "Could not instantiate JDBC Connection proxy", e );
		}
	}
	
	public static final Class[] PREPARED_STMNT_PROXY_INTERFACES = new Class[] {
		PreparedStatement.class,
		JdbcWrapper.class,
		InvalidatableWrapper.class
    };

	private static final ValueHolder<Constructor<PreparedStatement>> preparedStatementProxyConstructorValue = new ValueHolder<Constructor<PreparedStatement>>(
			new ValueHolder.DeferredInitializer<Constructor<PreparedStatement>>() {
				@Override
				public Constructor<PreparedStatement> initialize() {
					try {
						return locatePreparedStatementProxyClass().getConstructor( InvocationHandler.class );
					}
					catch (NoSuchMethodException e) {
						throw new JdbcProxyException( "Could not find proxy constructor in JDK generated Statement proxy class", e );
					}
				}

				@SuppressWarnings("unchecked")
				private Class<PreparedStatement> locatePreparedStatementProxyClass() {
					return (Class<PreparedStatement>) Proxy.getProxyClass(
							JdbcWrapper.class.getClassLoader(),
							PREPARED_STMNT_PROXY_INTERFACES
					);
				}
			}
	);

	public static PreparedStatement buildPreparedStatement(
			String sql,
			Statement statement,
			ConnectionProxyHandler connectionProxyHandler, 
			Connection connectionProxy) {
		final PreparedStatementProxyHandler proxyHandler = new PreparedStatementProxyHandler(
				sql,
				statement,
				connectionProxyHandler,
				connectionProxy
		);
		try {
			return preparedStatementProxyConstructorValue.getValue().newInstance( proxyHandler );
		}
		catch (Exception e) {
			throw new JdbcProxyException( "Could not instantiate JDBC PreparedStatement proxy", e );
		}
	}


	public static ResultSet buildImplicitResultSet(ResultSet result,
			ConnectionProxyHandler connectionProxyHandler,
			Connection connectionProxy, Statement proxy) {
		return null;
	}

	private static final ValueHolder<Constructor<ResultSet>> resultSetProxyConstructorValue = new ValueHolder<Constructor<ResultSet>>(
			new ValueHolder.DeferredInitializer<Constructor<ResultSet>>() {
				@Override
				public Constructor<ResultSet> initialize() {
					try {
						return locateCallableStatementProxyClass().getConstructor( InvocationHandler.class );
					}
					catch (NoSuchMethodException e) {
						throw new JdbcProxyException( "Could not find proxy constructor in JDK generated ResultSet proxy class", e );
					}
				}

				@SuppressWarnings("unchecked")
				private Class<ResultSet> locateCallableStatementProxyClass() {
					return (Class<ResultSet>) Proxy.getProxyClass(
							JdbcWrapper.class.getClassLoader(),
							RESULTSET_PROXY_INTERFACES
					);
				}
			}
	);
	

	public static ResultSet buildResultSet(
			ResultSet resultSet,
			AbstractStatementProxyHandler statementProxyHandler,
			Statement statementProxy) {
		final ResultSetProxyHandler proxyHandler = new ResultSetProxyHandler( resultSet, statementProxyHandler, statementProxy );
		try {
			return resultSetProxyConstructorValue.getValue().newInstance( proxyHandler );
		}
		catch (Exception e) {
			throw new JdbcProxyException( "Could not instantiate JDBC ResultSet proxy", e );
		}
	}
}
