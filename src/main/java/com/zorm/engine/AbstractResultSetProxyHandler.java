package com.zorm.engine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;

import com.zorm.jdbc.JdbcResourceRegistry;
import com.zorm.jdbc.JdbcServices;

public abstract class AbstractResultSetProxyHandler extends AbstractProxyHandler {


	private ResultSet resultSet;

	public AbstractResultSetProxyHandler(ResultSet resultSet) {
		super( resultSet.hashCode() );
		this.resultSet = resultSet;
	}

	protected abstract JdbcServices getJdbcServices();

	protected abstract JdbcResourceRegistry getResourceRegistry();

	protected abstract Statement getExposableStatement();

	protected final ResultSet getResultSet() {
		errorIfInvalid();
		return resultSet;
	}

	protected final ResultSet getResultSetWithoutChecks() {
		return resultSet;
	}

	@Override
	protected Object continueInvocation(Object proxy, Method method, Object[] args) throws Throwable {
		final String methodName = method.getName();

		// other methods allowed while invalid ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		if ( "close".equals( methodName ) ) {
			explicitClose( ( ResultSet ) proxy );
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
			return method.invoke( getResultSetWithoutChecks(), args );
		}
		if ( "unwrap".equals( methodName ) && args.length == 1 ) {
			return method.invoke( getResultSetWithoutChecks(), args );
		}

		if ( "getWrappedObject".equals( methodName ) ) {
			return getResultSetWithoutChecks();
		}

		if ( "getStatement".equals( methodName ) ) {
			return getExposableStatement();
		}

		try {
			return method.invoke( resultSet, args );
		}
		catch ( InvocationTargetException e ) {
			Throwable realException = e.getTargetException();
            if (SQLException.class.isInstance(realException)) throw getJdbcServices().getSqlExceptionHelper().convert((SQLException)realException,
                                                                                                                      realException.getMessage());
            throw realException;
		}
	}

	private void explicitClose(ResultSet proxy) {
		if ( isValid() ) {
			getResourceRegistry().release( proxy );
		}
	}

	protected void invalidateHandle() {
		resultSet = null;
		invalidate();
	}
}
