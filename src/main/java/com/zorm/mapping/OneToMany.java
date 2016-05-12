package com.zorm.mapping;

import java.util.Iterator;

import com.zorm.FetchMode;
import com.zorm.config.Mappings;
import com.zorm.engine.Mapping;
import com.zorm.exception.MappingException;
import com.zorm.type.EntityType;
import com.zorm.type.Type;

public class OneToMany implements Value{
	private final Mappings mappings;
	private final Table referencingTable;

	private String referencedEntityName;
	private PersistentClass associatedClass;
	private boolean embedded;
	private boolean ignoreNotFound;

	private EntityType getEntityType() {
		return mappings.getTypeResolver().getTypeFactory().manyToOne(
				getReferencedEntityName(), 
				null, 
				false,
				false,
				isIgnoreNotFound(),
				false
			);
	}

	public OneToMany(Mappings mappings, PersistentClass owner) throws MappingException {
		this.mappings = mappings;
		this.referencingTable = (owner==null) ? null : owner.getTable();
	}

	public PersistentClass getAssociatedClass() {
		return associatedClass;
	}

    /**
     * Associated entity on the many side
     */
	public void setAssociatedClass(PersistentClass associatedClass) {
		this.associatedClass = associatedClass;
	}

	public void createForeignKey() {
		// no foreign key element of for a one-to-many
	}

	public Iterator getColumnIterator() {
		return associatedClass.getKey().getColumnIterator();
	}

	public int getColumnSpan() {
		return associatedClass.getKey().getColumnSpan();
	}

	public FetchMode getFetchMode() {
		return FetchMode.JOIN;
	}

    /** 
     * Table of the owner entity (the "one" side)
     */
	public Table getTable() {
		return referencingTable;
	}

	public Type getType() {
		return getEntityType();
	}

	public boolean isNullable() {
		return false;
	}

	public boolean isSimpleValue() {
		return false;
	}

	public boolean isAlternateUniqueKey() {
		return false;
	}

	public boolean hasFormula() {
		return false;
	}
	
	public boolean isValid(Mapping mapping) throws MappingException {
		if (referencedEntityName==null) {
			throw new MappingException("one to many association must specify the referenced entity");
		}
		return true;
	}

    public String getReferencedEntityName() {
		return referencedEntityName;
	}

    /** 
     * Associated entity on the "many" side
     */    
	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName==null ? null : referencedEntityName.intern();
	}

	public void setTypeUsingReflection(String className, String propertyName) {}
	
	
	public boolean[] getColumnInsertability() {
		throw new UnsupportedOperationException();
	}
	
	public boolean[] getColumnUpdateability() {
		throw new UnsupportedOperationException();
	}

	public boolean isIgnoreNotFound() {
		return ignoreNotFound;
	}

	public void setIgnoreNotFound(boolean ignoreNotFound) {
		this.ignoreNotFound = ignoreNotFound;
	}
}
