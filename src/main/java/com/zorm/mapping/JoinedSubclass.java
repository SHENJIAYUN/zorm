package com.zorm.mapping;

import java.util.Iterator;
import java.util.Map;

import com.zorm.engine.Mapping;
import com.zorm.exception.MappingException;

public class JoinedSubclass extends Subclass implements TableOwner {

	private Table table;
	private KeyValue key;

	public JoinedSubclass(PersistentClass superclass) {
		super(superclass);
	}

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table=table;
		getSuperclass().addSubclassTable(table);
	}

	public KeyValue getKey() {
		return key;
	}

	public void setKey(KeyValue key) {
		this.key = key;
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

	public Iterator getReferenceablePropertyIterator() {
		return getPropertyIterator();
	}

	public Object accept(PersistentClassVisitor mv) {
		return mv.accept(this);
	}

}
