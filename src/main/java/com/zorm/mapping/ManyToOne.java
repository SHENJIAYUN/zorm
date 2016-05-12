package com.zorm.mapping;

import java.util.Map;

import com.zorm.config.Mappings;
import com.zorm.exception.MappingException;
import com.zorm.type.Type;
import com.zorm.type.EntityType;

public class ManyToOne extends ToOne{
	private boolean ignoreNotFound;
	private boolean isLogicalOneToOne;
	
	public ManyToOne(Mappings mappings, Table table) {
		super( mappings, table );
	}

	@Override
	public void createForeignKey() throws MappingException {
		if (referencedPropertyName==null && !hasFormula() ) {
			createForeignKeyOfEntity( ( (EntityType) getType() ).getAssociatedEntityName() );
		}
	}

	@Override
	public Type getType() throws MappingException {
		return getMappings().getTypeResolver().getTypeFactory().manyToOne(
				getReferencedEntityName(), 
				getReferencedPropertyName(),
				isLazy(),
				isUnwrapProxy(),
				isIgnoreNotFound(),
				isLogicalOneToOne
		);
	}
	
	public boolean isIgnoreNotFound() {
		return ignoreNotFound;
	}

	public void setIgnoreNotFound(boolean ignoreNotFound) {
		this.ignoreNotFound = ignoreNotFound;
	}

	public void markAsLogicalOneToOne() {
		this.isLogicalOneToOne = true;
	}

	public boolean isLogicalOneToOne() {
		return isLogicalOneToOne;
	}

	public void createPropertyRefConstraints(Map persistentClasses) {
		if(referencedPropertyName != null){
			
		}
	}
}
