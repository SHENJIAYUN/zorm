package com.zorm.engine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import com.zorm.jdbc.JdbcResourceRegistry;
import com.zorm.jdbc.JdbcServices;

public class ConnectionProxyHandler extends AbstractProxyHandler
implements NonDurableConnectionObserver{
	private LogicalConnectionImplementor logicalConnection;

	public ConnectionProxyHandler(LogicalConnectionImplementor logicalConnection) {
		super( logicalConnection.hashCode() );
		this.logicalConnection = logicalConnection;
		this.logicalConnection.addObserver( this );
	}
	
	protected LogicalConnectionImplementor getLogicalConnection() {
		errorIfInvalid();
		return logicalConnection;
	}

	@Override
	public void physicalConnectionObtained(Connection physicalConnection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected Object continueInvocation(Object proxy, Method method,
			Object[] args) throws Throwable {
		final String methodName = method.getName();
		errorIfInvalid();
		try {
			Object result = method.invoke( extractPhysicalConnection(), args );
			result = postProcess( result, proxy, method, args );

			return result;
		}
		catch( InvocationTargetException e ) {
			Throwable realException = e.getTargetException();
			if ( SQLException.class.isInstance( realException ) ) {
				throw logicalConnection.getJdbcServices().getSqlExceptionHelper()
						.convert( ( SQLException ) realException, realException.getMessage() );
			}
			else {
				throw realException;
			}
		}
	}

	private Object postProcess(Object result, Object proxy, Method method,
			Object[] args) throws SQLException {
		String methodName = method.getName();
		Object wrapped = result;
		if("prepareStatement".equals(methodName)){
			wrapped = ProxyBuilder.buildPreparedStatement(
					( String ) args[0],
					(PreparedStatement) result,
					this,
					( Connection ) proxy
			);
			postProcessPreparedStatement( ( Statement ) wrapped );
		}
		return wrapped;
	}

	private void postProcessPreparedStatement(Statement statement) throws SQLException{
		postProcessStatement( statement );
	}

	private void postProcessStatement(Statement statement) {
		getResourceRegistry().register( statement );
	}

	private Connection extractPhysicalConnection() {
		return logicalConnection.getConnection();
	}

	public JdbcServices getJdbcServices() {
		return logicalConnection.getJdbcServices();
	}

	public JdbcResourceRegistry getResourceRegistry() {
		return logicalConnection.getResourceRegistry();
	}

	@Override
	public void physicalConnectionReleased() {
		
	}

	@Override
	public void logicalConnectionClosed() {
		
	}
}
