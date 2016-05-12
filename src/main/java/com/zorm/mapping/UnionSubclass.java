package com.zorm.mapping;

import java.util.Iterator;

import com.zorm.engine.Mapping;
import com.zorm.exception.MappingException;

public class UnionSubclass extends Subclass implements TableOwner {

	private Table table;
	private KeyValue key;

	public UnionSubclass(PersistentClass superclass) {
		super(superclass);
	}

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
		getSuperclass().addSubclassTable(table);
	}

	public java.util.Set getSynchronizedTables() {
		return synchronizedTables;
	}
	
	protected Iterator getNonDuplicatedPropertyIterator() {
		return getPropertyClosureIterator();
	}

	public void validate(Mapping mapping) throws MappingException {
		super.validate(mapping);
		if ( key!=null && !key.isValid(mapping) ) {
			throw new MappingException(
				"subclass key mapping has wrong number of columns: " +
				getEntityName() +
				" type: " +
				key.getType().getName()
			);
		}
	}
	
	public Table getIdentityTable() {
		return getTable();
	}
	
	public Object accept(PersistentClassVisitor mv) {
		return mv.accept(this);
	}
}
