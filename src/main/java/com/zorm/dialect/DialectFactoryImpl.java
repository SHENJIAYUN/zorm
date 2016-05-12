package com.zorm.dialect;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;

import com.zorm.config.AvailableSettings;
import com.zorm.exception.ClassLoadingException;
import com.zorm.exception.ZormException;
import com.zorm.service.ClassLoaderService;
import com.zorm.service.DialectResolver;
import com.zorm.service.InjectService;

public class DialectFactoryImpl implements DialectFactory{
	private ClassLoaderService classLoaderService;
	private DialectResolver dialectResolver;
	@InjectService
	public void setClassLoaderService(ClassLoaderService classLoaderService) {
		this.classLoaderService = classLoaderService;
	}
	
	@InjectService
	public void setDialectResolver(DialectResolver dialectResolver) {
		this.dialectResolver = dialectResolver;
	}
	
	@Override
	public Dialect buildDialect(Map configValues, Connection connection) throws ZormException {
		final String dialectName = (String) configValues.get( AvailableSettings.DIALECT );
		if ( dialectName != null ) {
			return constructDialect( dialectName );
		}
		else {
			return determineDialect( connection );
		}
	}

	private Dialect determineDialect(Connection connection) {
		if ( connection == null ) {
			throw new ZormException( "Connection cannot be null when 'hibernate.dialect' not set" );
		}

		try {
			final DatabaseMetaData databaseMetaData = connection.getMetaData();
			final Dialect dialect = dialectResolver.resolveDialect( databaseMetaData );

			if ( dialect == null ) {
				throw new ZormException(
						"Unable to determine Dialect to use [name=" + databaseMetaData.getDatabaseProductName() +
								", majorVersion=" + databaseMetaData.getDatabaseMajorVersion() +
								"]; user must register resolver or explicitly set 'hibernate.dialect'"
				);
			}

			return dialect;
		}
		catch ( SQLException sqlException ) {
			throw new ZormException(
					"Unable to access java.sql.DatabaseMetaData to determine appropriate Dialect to use",
					sqlException
			);
		}
	}

	private Dialect constructDialect(String dialectName) {
		try{
			return (Dialect)classLoaderService.classForName(dialectName).newInstance();
		}
		catch ( ClassLoadingException e ) {
			throw new ZormException( "Dialect class not found: " + dialectName, e );
		}
		catch ( ZormException e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw new ZormException( "Could not instantiate dialect class", e );
		}
	}
    
}
