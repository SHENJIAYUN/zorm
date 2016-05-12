package com.zorm.config;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.exception.MappingException;
import com.zorm.mapping.Collection;
import com.zorm.mapping.Selectable;
import com.zorm.mapping.Value;

public abstract class CollectionSecondPass implements SecondPass {

	private static final Log log = LogFactory.getLog(CollectionSecondPass.class);
	
	Mappings mappings;
	Collection collection;
	private Map localInheritedMetas;
	
	public CollectionSecondPass(Mappings mappings, Collection collection, java.util.Map inheritedMetas) {
		this.collection = collection;
		this.mappings = mappings;
		this.localInheritedMetas = inheritedMetas;
	}

	public CollectionSecondPass(Mappings mappings, Collection collection) {
		this(mappings, collection, Collections.EMPTY_MAP);
	}
	
	abstract public void secondPass(java.util.Map persistentClasses, java.util.Map inheritedMetas)
			throws MappingException;
	
	@Override
	public void doSecondPass(Map persistentClasses) throws MappingException {
		log.debug( "Second pass for collection: " + collection.getRole() );

		secondPass( persistentClasses, localInheritedMetas ); // using local since the inheritedMetas at this point is not the correct map since it is always the empty map
		collection.createAllKeys();
	}
	
	private static String columns(Value val) {
		StringBuilder columns = new StringBuilder();
		Iterator iter = val.getColumnIterator();
		while ( iter.hasNext() ) {
			columns.append( ( (Selectable) iter.next() ).getText() );
			if ( iter.hasNext() ) columns.append( ", " );
		}
		return columns.toString();
	}

}
