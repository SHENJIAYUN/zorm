package com.zorm.mapping;

import com.zorm.dialect.Dialect;
import com.zorm.exception.MappingException;
import com.zorm.id.IdentifierGenerator;
import com.zorm.id.IdentifierGeneratorFactory;

/**
 * Represents an identifying key of a table: the value for primary key
 * of an entity, or a foreign key of a collection or join table or
 * joined subclass table.
 * @author JIA
 */
public interface KeyValue extends Value{
	
	public IdentifierGenerator createIdentifierGenerator(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect,
			String defaultCatalog,
			String defaultSchema,
			RootClass rootClass) throws MappingException;
	
	public boolean isIdentityColumn(IdentifierGeneratorFactory identifierGeneratorFactory, Dialect dialect);
	
    public String getNullValue();
	
	public boolean isUpdateable();

	public boolean isCascadeDeleteEnabled();

	public void createForeignKeyOfEntity(String entityName);
	
	//public void createForeignKeyOfEntity(String entityName);
}
