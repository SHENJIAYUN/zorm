package com.zorm.mapping;

import java.util.Iterator;

import com.zorm.config.Mappings;
import com.zorm.engine.Mapping;
import com.zorm.exception.MappingException;
import com.zorm.type.CollectionType;

public class Set extends Collection {

	private static final long serialVersionUID = -3267265810646741168L;

	public Set(Mappings mappings,PersistentClass owner){
		super(mappings,owner);
	}
	
	public void validate(Mapping mapping) throws MappingException {
		super.validate( mapping );
	}
	
	public boolean isSet(){
		return true;
	}
	
	@Override
	public CollectionType getDefaultCollectionType() throws MappingException {
		if ( isSorted() ) {
			return getMappings().getTypeResolver()
					.getTypeFactory()
					.sortedSet( getRole(), getReferencedPropertyName(), getComparator() );
		}
		else if ( hasOrder() ) {
			return getMappings().getTypeResolver()
					.getTypeFactory()
					.orderedSet( getRole(), getReferencedPropertyName() );
		}
		else {
			return getMappings().getTypeResolver()
					.getTypeFactory()
					.set( getRole(), getReferencedPropertyName() );
		}
	}

	@Override
	void createPrimaryKey() {
		if ( !isOneToMany() ) {
			PrimaryKey pk = new PrimaryKey();
			pk.addColumns( getKey().getColumnIterator() );
			Iterator iter = getElement().getColumnIterator();
			while ( iter.hasNext() ) {
				Object selectable = iter.next();
				if ( selectable instanceof Column ) {
					Column col = (Column) selectable;
					if ( !col.isNullable() ) {
						pk.addColumn(col);
					}
				}
			}
			if ( pk.getColumnSpan()==getKey().getColumnSpan() ) { 
			}
			else {
				getCollectionTable().setPrimaryKey(pk);
			}
		}
		else {
		}
	}

}
