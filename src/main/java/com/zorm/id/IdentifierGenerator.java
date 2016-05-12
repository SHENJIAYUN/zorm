package com.zorm.id;

import java.io.Serializable;

import com.zorm.exception.ZormException;
import com.zorm.session.SessionImplementor;

public interface IdentifierGenerator {
	/**
     * The configuration parameter holding the entity name
     */
    public static final String ENTITY_NAME = "entity_name";
    /**
     * The configuration parameter holding the JPA entity name
     */
    public static final String JPA_ENTITY_NAME = "jpa_entity_name";
    
    /**
	 * Generate a new identifier.
	 * @param session
	 * @param object the entity or toplevel collection for which the id is being generated
	 *
	 * @return a new identifier
	 * @throws ZormException
	 */
	public Serializable generate(SessionImplementor session, Object object) 
	throws ZormException;
}
