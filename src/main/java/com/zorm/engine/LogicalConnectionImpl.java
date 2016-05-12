package com.zorm.engine;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.zorm.ConnectionReleaseMode;
import com.zorm.exception.JDBCException;
import com.zorm.exception.ZormException;
import com.zorm.jdbc.JdbcConnectionAccess;
import com.zorm.jdbc.JdbcResourceRegistry;
import com.zorm.jdbc.JdbcResourceRegistryImpl;
import com.zorm.jdbc.JdbcServices;

public class LogicalConnectionImpl implements LogicalConnectionImplementor{
	
	private transient Connection physicalConnection;
	private transient Connection shareableConnectionProxy;

	private final transient ConnectionReleaseMode connectionReleaseMode;
	private final transient JdbcServices jdbcServices;
	private final transient JdbcConnectionAccess jdbcConnectionAccess;
	private final transient JdbcResourceRegistry jdbcResourceRegistry;
	private final transient List<ConnectionObserver> observers;

	private boolean releasesEnabled = true;
	

	private final boolean isUserSuppliedConnection;

	private boolean isClosed;

	public LogicalConnectionImpl(
			Connection userSuppliedConnection,
			ConnectionReleaseMode connectionReleaseMode,
			JdbcServices jdbcServices,
			JdbcConnectionAccess jdbcConnectionAccess) {
		this(
				connectionReleaseMode,
				jdbcServices,
				jdbcConnectionAccess,
				(userSuppliedConnection != null),
				false,
				new ArrayList<ConnectionObserver>()
		);
		this.physicalConnection = userSuppliedConnection;
	}
	
	private LogicalConnectionImpl(
			ConnectionReleaseMode connectionReleaseMode,
			JdbcServices jdbcServices,
			JdbcConnectionAccess jdbcConnectionAccess,
			boolean isUserSuppliedConnection,
			boolean isClosed,
			List<ConnectionObserver> observers) {
		this.connectionReleaseMode = determineConnectionReleaseMode(
				jdbcConnectionAccess, isUserSuppliedConnection, connectionReleaseMode
		);
		this.jdbcServices = jdbcServices;
		this.jdbcConnectionAccess = jdbcConnectionAccess;
		this.jdbcResourceRegistry = new JdbcResourceRegistryImpl( getJdbcServices().getSqlExceptionHelper() );
		this.observers = observers;

		this.isUserSuppliedConnection = isUserSuppliedConnection;
		this.isClosed = isClosed;
	}

	public JdbcServices getJdbcServices() {
		return jdbcServices;
	}
	
	@Override
	public JdbcResourceRegistry getResourceRegistry() {
		return jdbcResourceRegistry;
	}
	
	@Override
	public void addObserver(ConnectionObserver observer) {
		observers.add( observer );
	}

	private ConnectionReleaseMode determineConnectionReleaseMode(
			JdbcConnectionAccess jdbcConnectionAccess,
			boolean isUserSuppliedConnection,
			ConnectionReleaseMode connectionReleaseMode) {

		if(isUserSuppliedConnection){
			return ConnectionReleaseMode.ON_CLOSE;
		}
		else if(connectionReleaseMode == ConnectionReleaseMode.AFTER_STATEMENT &&
				! jdbcConnectionAccess.supportsAggressiveRelease()){
			return ConnectionReleaseMode.AFTER_TRANSACTION;
		}
		else{
			return connectionReleaseMode;
		}
		
	}

	@Override
	public Connection getConnection() {
		if(isClosed){
			throw new ZormException( "Logical connection is closed" );
		}
		if(physicalConnection == null){
			if ( isUserSuppliedConnection ) {
				// should never happen
				throw new ZormException( "User-supplied connection was null" );
			}	
			obtainConnection();
		}
		return physicalConnection;
	}

	private void obtainConnection() throws JDBCException{
      try{
    	  physicalConnection = jdbcConnectionAccess.obtainConnection();
          for(ConnectionObserver observer:observers){
        	  observer.physicalConnectionObtained( physicalConnection );
          }
      }
      catch(SQLException sqle){
    	  throw getJdbcServices().getSqlExceptionHelper().convert( sqle, "Could not open connection" );
      }
	}

	public void disableReleases() {
		releasesEnabled = false;
	}

	@Override
	public Connection getShareableConnectionProxy() {
		if ( shareableConnectionProxy == null ) {
			shareableConnectionProxy = buildConnectionProxy();
		}
		return shareableConnectionProxy;
	}
	
	private Connection buildConnectionProxy() {
		return ProxyBuilder.buildConnection( this );
	}

	public void enableReleases() {
		releasesEnabled = true;
		afterStatementExecution();
	}

	private void afterStatementExecution() {
		if ( connectionReleaseMode == ConnectionReleaseMode.AFTER_STATEMENT ) {
			if ( ! releasesEnabled ) {
				return;
			}
			if ( jdbcResourceRegistry.hasRegisteredResources() ) {
				return;
			}
			releaseConnection();
		}
	}

	public void afterTransaction() {
		if ( connectionReleaseMode == ConnectionReleaseMode.AFTER_STATEMENT ||
				connectionReleaseMode == ConnectionReleaseMode.AFTER_TRANSACTION ) {
			if ( jdbcResourceRegistry.hasRegisteredResources() ) {
				jdbcResourceRegistry.releaseResources();
			}
			aggressiveRelease();
		}
	}

	private void aggressiveRelease() {
		if ( isUserSuppliedConnection ) {
		}
		else {
			if ( physicalConnection != null ) {
				releaseConnection();
			}
		}
	}

	private void releaseConnection() {
		if ( physicalConnection == null ) {
			return;
		}
		try {
			if ( !physicalConnection.isClosed() ) {
				getJdbcServices().getSqlExceptionHelper().logAndClearWarnings( physicalConnection );
			}
			if ( !isUserSuppliedConnection ) {
				jdbcConnectionAccess.releaseConnection( physicalConnection );
			}
		}
		catch (SQLException e) {
			throw getJdbcServices().getSqlExceptionHelper().convert( e, "Could not close connection" );
		}
		finally {
			physicalConnection = null;
		}
		for ( ConnectionObserver observer : observers ) {
			observer.physicalConnectionReleased();
		}
//		releaseNonDurableObservers();
		
	}

	private void releaseProxies() {
		if ( shareableConnectionProxy != null ) {
			try {
				shareableConnectionProxy.close();
			}
			catch (SQLException e) {
			}
		}
		shareableConnectionProxy = null;
	}

	
	@Override
	public Connection close() {
		Connection c = isUserSuppliedConnection ? physicalConnection : null;
		try {
			releaseProxies();
			jdbcResourceRegistry.close();
			if ( !isUserSuppliedConnection && physicalConnection != null ) {
				releaseConnection();
			}
			return c;
		}
		finally {
			physicalConnection = null;
			isClosed = true;
			for ( ConnectionObserver observer : observers ) {
				observer.logicalConnectionClosed();
			}
			observers.clear();
		}
	}
}
