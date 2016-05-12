package com.zorm.session;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SessionFactoryRegistry {
	
	private static final Log log = LogFactory.getLog(SessionFactoryRegistry.class);
	public static final SessionFactoryRegistry INSTANCE = new SessionFactoryRegistry();
	private final ConcurrentHashMap<String, SessionFactory> sessionFactoryMap = new ConcurrentHashMap<String, SessionFactory>();
	
	public SessionFactoryRegistry() {
		log.debug( "Initializing SessionFactoryRegistry : "+this );
	}
	
	public void addSessionFactory(
			String uuid,
			String name,
			boolean isNameAlsoJndiName,
			SessionFactory instance
			){
		if ( uuid == null ) {
			throw new IllegalArgumentException( "SessionFactory UUID cannot be null" );
		}
		
		log.debug("Registering SessionFactory: "+ uuid + name == null ? " <unnamed>" : name);
	    sessionFactoryMap.put(uuid, instance);
	    
	    if(name==null || ! isNameAlsoJndiName){
	    	log.debug("Not binding SessionFactory to JNDI, no JNDI name configured");
	    	return;
	    }
	}
}
