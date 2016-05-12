package com.zorm.id;

import java.io.Serializable;
import java.util.Properties;

import com.zorm.dialect.Dialect;
import com.zorm.exception.IdentifierGenerationException;
import com.zorm.exception.MappingException;
import com.zorm.exception.ZormException;
import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;

public class Assigned implements IdentifierGenerator, Configurable{
	private String entityName;

	public Serializable generate(SessionImplementor session, Object obj) throws ZormException {
		final Serializable id = session.getEntityPersister( entityName, obj ).getIdentifier( obj, session );
		if ( id == null ) {
			throw new IdentifierGenerationException(
					"ids for this class must be manually assigned before calling save(): " + entityName
			);
		}
		
		return id;
	}

	public void configure(Type type, Properties params, Dialect d) throws MappingException {
		entityName = params.getProperty(ENTITY_NAME);
		if ( entityName == null ) {
			throw new MappingException("no entity name");
		}
	}
}
