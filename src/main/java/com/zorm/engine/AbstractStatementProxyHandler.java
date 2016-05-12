package com.zorm.engine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.zorm.jdbc.JdbcResourceRegistry;
import com.zorm.jdbc.JdbcServices;

public abstract class AbstractStatementProxyHandler extends AbstractProxyHandler {


	private ConnectionProxyHandler connectionProxyHandler;
	private Connection connectionProxy;
	private Statement statement;

	protected AbstractStatementProxyHandler(
			Statement statement,
			ConnectionProxyHandler connectionProxyHandler,
			Connection connectionProxy) {
		super( statement.hashCode() );
		this.statement = statement;
		this.connectionProxyHandler = connectionProxyHandler;
		this.connectionProxy = connectionProxy;
	}

	protected ConnectionProxyHandler getConnectionProxy() {
		errorIfInvalid();
		return connectionProxyHandler;
	}

	protected JdbcServices getJdbcServices() {
		return getConnectionProxy().getJdbcServices();
	}

	protected JdbcResourceRegistry getResourceRegistry() {
		return getConnectionProxy().getResourceRegistry();
	}

	protected Statement getStatement() {
		errorIfInvalid();
		return statement;
	}

	protected Statement getStatementWithoutChecks() {
		return statement;
	}

	@Override
	protected Object continueInvocation(Object proxy, Method method, Object[] args) throws Throwable {
		final String methodName = method.getName();
		// other methods allowed while invalid ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		if ( "close".equals( methodName ) ) {
			explicitClose( ( Statement ) proxy );
			return null;
		}
		if ( "invalidate".equals( methodName ) ) {
			invalidateHandle();
			return null;
		}

		errorIfInvalid();

		// handle the JDBC 4 Wrapper#isWrapperFor and Wrapper#unwrap calls
		//		these cause problems to the whole proxy scheme though as we need to return the raw objects
		if ( "isWrapperFor".equals( methodName ) && args.length == 1 ) {
			return method.invoke( getStatementWithoutChecks(), args );
		}
		if ( "unwrap".equals( methodName ) && args.length == 1 ) {
			return method.invoke( getStatementWithoutChecks(), args );
		}

		if ( "getWrappedObject".equals( methodName ) ) {
			return getStatementWithoutChecks();
		}

		if ( "getConnection".equals( methodName ) ) {
			return connectionProxy;
		}

		beginningInvocationHandling( method, args );

		try {
			Object result = method.invoke( statement, args );
			result = wrapIfNecessary( result, proxy, method );
			return result;
		}
		catch ( InvocationTargetException e ) {
			Throwable realException = e.getTargetException();
			if ( SQLException.class.isInstance( realException ) ) {
				throw connectionProxyHandler.getJdbcServices().getSqlExceptionHelper()
						.convert( ( SQLException ) realException, realException.getMessage() );
			}
			else {
				throw realException;
			}
		}
	}

	private Object wrapIfNecessary(Object result, Object proxy, Method method) {
		if ( !( ResultSet.class.isAssignableFrom( method.getReturnType() ) ) ) {
			return result;
		}

		final ResultSet wrapper;
		if ( "getGeneratedKeys".equals( method.getName() ) ) {
			wrapper = ProxyBuilder.buildImplicitResultSet( ( ResultSet ) result, connectionProxyHandler, connectionProxy, ( Statement ) proxy );
		}
		else {
			wrapper = ProxyBuilder.buildResultSet( ( ResultSet ) result, this, ( Statement ) proxy );
		}
		getResourceRegistry().register( wrapper );
		return wrapper;
	}

	protected void beginningInvocationHandling(Method method, Object[] args) {
	}

	private void explicitClose(Statement proxy) {
		
	}

	private void invalidateHandle() {
		connectionProxyHandler = null;
		statement = null;
		invalidate();
	}
}
