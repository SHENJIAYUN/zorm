package com.zorm.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.config.AvailableSettings;
import com.zorm.exception.ClassLoadingException;
import com.zorm.exception.UnknownUnwrapTypeException;
import com.zorm.exception.ZormException;
import com.zorm.service.ClassLoaderService;
import com.zorm.service.Configurable;
import com.zorm.service.ConnectionProviderInitiator;
import com.zorm.service.ServiceRegistryAwareService;
import com.zorm.service.ServiceRegistryImplementor;
import com.zorm.service.Stoppable;
import com.zorm.util.ConfigurationHelper;
import com.zorm.util.ReflectHelper;

public class DriverManagerConnectionProviderImpl implements ConnectionProvider,Configurable,
               Stoppable,ServiceRegistryAwareService{

	private static final Log log = LogFactory.getLog(DriverManagerConnectionProviderImpl.class);
	
	//数据库URL
	private String url;
	//新建连接时使用的参数
	private Properties connectionProps;
	private Integer isolation;
	private int poolSize;
	private boolean autocommit;
	
	private final ArrayList<Connection> pool = new ArrayList<Connection>();
	private int checkedOut = 0;
	private boolean stopped;
	
	private transient ServiceRegistryImplementor serviceRegistry;
	
	@Override
	public boolean isUnwrappableAs(Class unwrapType) {
		return ConnectionProvider.class.equals( unwrapType ) ||
				DriverManagerConnectionProviderImpl.class.isAssignableFrom( unwrapType );
	}
	
	@Override
	@SuppressWarnings( {"unchecked"})
	public <T> T unwrap(Class<T> unwrapType) {
		if ( ConnectionProvider.class.equals( unwrapType ) ||
				DriverManagerConnectionProviderImpl.class.isAssignableFrom( unwrapType ) ) {
			return (T) this;
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void stop() {
		for(Connection connection : pool){
			try{
				connection.close();
			}
			catch(SQLException sqle){
				log.warn("Problem closing pooled connection");
			}
		}
		pool.clear();
		stopped = true;
	}

	@Override
	public void configure(Map configurationValues) {
		String driverClassName = (String) configurationValues.get( AvailableSettings.DRIVER );
		if(driverClassName==null){
		}
		else if(serviceRegistry != null){
			try {
				serviceRegistry.getService( ClassLoaderService.class ).classForName( driverClassName );
			}
			catch ( ClassLoadingException e ) {
				throw new ClassLoadingException(
						"Specified JDBC Driver " + driverClassName + " class not found",
						e
				);
			}
		}
		else{
			try {
				// trying via forName() first to be as close to DriverManager's semantics
				Class.forName( driverClassName );
			}
			catch ( ClassNotFoundException cnfe ) {
				try{
					ReflectHelper.classForName( driverClassName );
				}
				catch ( ClassNotFoundException e ) {
					throw new ZormException( "Specified JDBC Driver " + driverClassName + " class not found", e );
				}
			}
		}
		
		poolSize = ConfigurationHelper.getInt( AvailableSettings.POOL_SIZE, configurationValues, 20);
		autocommit = ConfigurationHelper.getBoolean( AvailableSettings.AUTOCOMMIT, configurationValues );
		isolation = ConfigurationHelper.getInteger( AvailableSettings.ISOLATION, configurationValues );
		url = (String) configurationValues.get( AvailableSettings.URL );
		if(url==null){
			throw new ZormException("url should not be null");
		}
		
		connectionProps = ConnectionProviderInitiator.getConnectionProperties( configurationValues );
	}

	@Override
	public Connection getConnection() throws SQLException {
		synchronized (pool) {
		  //连接池里面不为空，则在连接池里面取一个连接
		  if(!pool.isEmpty()){
			  int last = pool.size()-1;
			  Connection pooled = pool.remove( last );
			  if(isolation!=null){
				  pooled.setTransactionIsolation(isolation.intValue());
			  }
			  if(pooled.getAutoCommit()!=autocommit){
				  pooled.setAutoCommit(autocommit);
			  }
			  checkedOut++;
			  return pooled;
		  }
		}
		//连接池里面为空，则新建一个连接
		Connection conn = DriverManager.getConnection(url,connectionProps);
		if(isolation!=null){
			conn.setTransactionIsolation(isolation.intValue());
		}
		if(conn.getAutoCommit()!=autocommit){
			conn.setAutoCommit(autocommit);
		}
		checkedOut++;
		return conn;
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		checkedOut--;
		synchronized(pool){
			int currentSize = pool.size();
			if(currentSize<poolSize){
				pool.add(conn);
				return;
			}
		}
		log.debug("Closing JDBC connection");
		conn.close();
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}
	
}
