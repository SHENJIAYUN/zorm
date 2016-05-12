package com.zorm.engine;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;

public class PreparedStatementProxyHandler extends AbstractStatementProxyHandler {

	private final String sql;

	protected PreparedStatementProxyHandler(
			String sql,
			Statement statement,
			ConnectionProxyHandler connectionProxyHandler,
			Connection connectionProxy) {
		super( statement, connectionProxyHandler, connectionProxy );
		//connectionProxyHandler.getJdbcServices().getSqlStatementLogger().logStatement( sql );
		this.sql = sql;
	}

	@Override
	protected void beginningInvocationHandling(Method method, Object[] args) {
		if ( isExecution( method ) ) {
			logExecution();
		}
		else {
			journalPossibleParameterBind( method, args );
		}
	}

	private void journalPossibleParameterBind(Method method, Object[] args) {
		String methodName = method.getName();
		// todo : is this enough???
		if ( methodName.startsWith( "set" ) && args != null && args.length >= 2 ) {
			journalParameterBind( method, args );
		}
	}

	private void journalParameterBind(Method method, Object[] args) {
	}

	private boolean isExecution(Method method) {
		return false;
	}

    private void logExecution() {
    }
}