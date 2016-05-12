package com.zorm.engine;

import java.sql.ResultSet;
import java.sql.Statement;

import com.zorm.jdbc.JdbcResourceRegistry;
import com.zorm.jdbc.JdbcServices;

public class ResultSetProxyHandler extends AbstractResultSetProxyHandler {
	private AbstractStatementProxyHandler statementProxyHandler;
	private Statement statementProxy;

	public ResultSetProxyHandler(
			ResultSet resultSet,
			AbstractStatementProxyHandler statementProxyHandler,
			Statement statementProxy) {
		super( resultSet );
		this.statementProxyHandler = statementProxyHandler;
		this.statementProxy = statementProxy;
	}

	protected AbstractStatementProxyHandler getStatementProxy() {
		return statementProxyHandler;
	}

	protected Statement getExposableStatement() {
		return statementProxy;
	}

	protected JdbcServices getJdbcServices() {
		return getStatementProxy().getJdbcServices();
	}

	protected JdbcResourceRegistry getResourceRegistry() {
		return getStatementProxy().getResourceRegistry();
	}

	protected void invalidateHandle() {
		statementProxyHandler = null;
		super.invalidateHandle();
	}
}
