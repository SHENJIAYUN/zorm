package com.zorm.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.exception.ZormException;
import com.zorm.util.ConfigHelper;
import com.zorm.util.ConfigurationHelper;

public class Environment implements AvailableSettings{
	
	private static final Log log = LogFactory.getLog(Environment.class);
	
	private static final Properties GLOBAL_PROPERTIES;
	private static final Map<Integer,String> ISOLATION_LEVELS;
	private static final boolean ENABLE_REFLECTION_OPTIMIZER;
	private static final BytecodeProvider BYTECODE_PROVIDER_INSTANCE;
	private static final Map OBSOLETE_PROPERTIES = new HashMap();
	private static final Map RENAMED_PROPERTIES = new HashMap();
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void verifyProperties(Map<?,?> configurationValues) {
		final Map propertiesToAdd = new HashMap();
		for ( Map.Entry entry : configurationValues.entrySet() ) {
			final Object replacementKey = OBSOLETE_PROPERTIES.get( entry.getKey() );
			if ( replacementKey != null ) {
				log.warn("Usage of obsolete property: "+entry.getKey()+" no longer supported, use: "+replacementKey);
			}
			final Object renamedKey = RENAMED_PROPERTIES.get( entry.getKey() );
			if ( renamedKey != null ) {
				log.warn("Property ["+entry.getKey()+"] has been renamed to ["+renamedKey+"]; update your properties appropriately");
				propertiesToAdd.put( renamedKey, entry.getValue() );
			}
		}
		configurationValues.putAll( propertiesToAdd );
	}
	
	public static Properties getProperties() {
		Properties copy = new Properties();
		copy.putAll(GLOBAL_PROPERTIES);
		return copy;
	}

	static{
		Map<Integer,String> temp = new HashMap<Integer, String>();
		temp.put(Connection.TRANSACTION_NONE, "NONE");
		temp.put(Connection.TRANSACTION_READ_UNCOMMITTED, "READ_UNCOMMITTED");
		temp.put( Connection.TRANSACTION_READ_COMMITTED, "READ_COMMITTED" );
		temp.put( Connection.TRANSACTION_REPEATABLE_READ, "REPEATABLE_READ" );
		temp.put( Connection.TRANSACTION_SERIALIZABLE, "SERIALIZABLE" );
		ISOLATION_LEVELS = Collections.unmodifiableMap(temp);
		GLOBAL_PROPERTIES = new Properties();
		GLOBAL_PROPERTIES.setProperty(USE_REFLECTION_OPTIMIZER, Boolean.FALSE.toString());
	
	    try{
	    	InputStream stream = ConfigHelper.getResourceAsStream("/hibernate.properties");
	    	try{
	    		GLOBAL_PROPERTIES.load(stream);
	    	}
	    	catch(Exception e){
	    		log.error("Problem loading properties from config.properties");
	    	}
	    	finally{
	    		try{
	    			stream.close();
	    		}
	    		catch(IOException e){
	    			log.error("Could not close stream on hibernate.properties:"+e);
	    		}
	    	}
	    }
	    catch(ZormException e){
	    	log.info("config.properties not found");
	    }
	    
	    try{
	    	GLOBAL_PROPERTIES.putAll(System.getProperties());
	    }
	    catch(SecurityException e){
	    	log.warn("Could not copy system properties, system properties will be ignored");
	    }
	    
	    verifyProperties(GLOBAL_PROPERTIES);
	    
		ENABLE_REFLECTION_OPTIMIZER = ConfigurationHelper.getBoolean(USE_REFLECTION_OPTIMIZER, GLOBAL_PROPERTIES);
		BYTECODE_PROVIDER_INSTANCE = buildBytecodeProvider( GLOBAL_PROPERTIES );
	}
	
	public static BytecodeProvider buildBytecodeProvider(Properties properties) {
		String provider = ConfigurationHelper.getString( BYTECODE_PROVIDER, properties, "javassist" );
		return buildBytecodeProvider( provider );
	}

	private static BytecodeProvider buildBytecodeProvider(String providerName) {
		if ( "javassist".equals( providerName ) ) {
			//return new org.hibernate.bytecode.internal.javassist.BytecodeProviderImpl();
		    return null;
		}

		return null;
	}

	public static boolean useReflectionOptimizer() {
		return ENABLE_REFLECTION_OPTIMIZER;
	}

	public static Object getBytecodeProvider() {
		return BYTECODE_PROVIDER_INSTANCE;
	}
}
