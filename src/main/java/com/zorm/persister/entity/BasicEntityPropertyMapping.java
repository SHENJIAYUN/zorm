package com.zorm.persister.entity;

import com.zorm.exception.QueryException;
import com.zorm.type.Type;

public class BasicEntityPropertyMapping extends AbstractPropertyMapping {
	private final AbstractEntityPersister persister;

	public BasicEntityPropertyMapping(AbstractEntityPersister persister) {
		this.persister = persister;
	}
	
	public String[] getIdentifierColumnNames() {
		//return persister.getIdentifierColumnNames();
		return null;
	}
	
	public String[] getIdentifierColumnReaders() {
		//return persister.getIdentifierColumnReaders();
		return null;
	}
	
	public String[] getIdentifierColumnReaderTemplates() {
		//return persister.getIdentifierColumnReaderTemplates();
		return null;
	}

	protected String getEntityName() {
		return persister.getEntityName();
	}

	public Type getType() {
		//return persister.getType();
		return null;
	}

	public String[] toColumns(final String alias, final String propertyName) throws QueryException {
		return super.toColumns( 
				persister.generateTableAlias( alias, persister.getSubclassPropertyTableNumber(propertyName) ), 
				propertyName 
			);
	}
}
