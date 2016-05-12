package com.zorm.mapping;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zorm.engine.Mapping;
import com.zorm.exception.MappingException;
import com.zorm.util.*;

public class SingleTableSubclass extends Subclass {

	public SingleTableSubclass(PersistentClass superclass) {
		super(superclass);
	}
	
	protected Iterator getNonDuplicatedPropertyIterator() {
		return new JoinedIterator(
				getSuperclass().getUnjoinedPropertyIterator(),
				getUnjoinedPropertyIterator()
		);
	}
	
	protected Iterator getDiscriminatorColumnIterator() {
		if ( isDiscriminatorInsertable() && !getDiscriminator().hasFormula() ) {
			return getDiscriminator().getColumnIterator();
		}
		else {
			return super.getDiscriminatorColumnIterator();
		}
	}

	public Object accept(PersistentClassVisitor mv) {
		return mv.accept(this);
	}
    
    public void validate(Mapping mapping) throws MappingException {
        if(getDiscriminator()==null) {
            throw new MappingException("No discriminator found for " + getEntityName() + ". Discriminator is needed when 'single-table-per-hierarchy' is used and a class has subclasses");
        }
        super.validate(mapping);
    }
}
