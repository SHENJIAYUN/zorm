package com.zorm.util;

import java.util.HashMap;
import java.util.Map;

import antlr.SemanticException;

import com.zorm.dialect.function.SQLFunction;
import com.zorm.exception.DetailedSemanticException;
import com.zorm.exception.MappingException;
import com.zorm.exception.QuerySyntaxException;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.query.NameGenerator;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.type.Type;

public class SessionFactoryHelper {
	private SessionFactoryImplementor sfi;
	private Map collectionPropertyMappingByRole;

	/**
	 * Construct a new SessionFactoryHelper instance.
	 *
	 * @param sfi The SessionFactory impl to be encapsulated.
	 */
	public SessionFactoryHelper(SessionFactoryImplementor sfi) {
		this.sfi = sfi;
		collectionPropertyMappingByRole = new HashMap();
	}

	/**
	 * Get a handle to the encapsulated SessionFactory.
	 *
	 * @return The encapsulated SessionFactory.
	 */
	public SessionFactoryImplementor getFactory() {
		return sfi;
	}

	public boolean isStrictJPAQLComplianceEnabled() {
		return sfi.getSettings().isStrictJPAQLCompliance();
	}

	public EntityPersister requireClassPersister(String name) throws SemanticException {
		EntityPersister cp;
		try {
			cp = findEntityPersisterByName( name );
			if ( cp == null ) {
				throw new QuerySyntaxException( name + " is not mapped" );
			}
		}
		catch ( MappingException e ) {
			throw new DetailedSemanticException( e.getMessage(), e );
		}
		return cp;
	}
	
	private EntityPersister findEntityPersisterByName(String name) throws MappingException {
		// First, try to get the persister using the given name directly.
		try {
			return sfi.getEntityPersister( name );
		}
		catch ( MappingException ignore ) {
			// unable to locate it using this name
		}

		// If that didn't work, try using the 'import' name.
		String importedClassName = sfi.getImportedClassName( name );
		if ( importedClassName == null ) {
			return null;
		}
		return sfi.getEntityPersister( importedClassName );
	}

	public String[][] generateColumnNames(Type[] sqlResultTypes) {
		return NameGenerator.generateColumnNames( sqlResultTypes, sfi );
	}

	public SQLFunction findSQLFunction(String text) {
		return null;
	}
}
