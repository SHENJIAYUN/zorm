package com.zorm.service;

import com.zorm.exception.UnknownPersisterException;
import com.zorm.mapping.Collection;
import com.zorm.mapping.JoinedSubclass;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.RootClass;
import com.zorm.mapping.SingleTableSubclass;
import com.zorm.mapping.UnionSubclass;
import com.zorm.persister.entity.BasicCollectionPersister;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.persister.entity.JoinedSubclassEntityPersister;
import com.zorm.persister.entity.OneToManyPersister;
import com.zorm.persister.entity.SingleTableEntityPersister;
import com.zorm.persister.entity.UnionSubclassEntityPersister;

public class StandardPersisterClassResolver implements PersisterClassResolver {

	@Override
	public Class<? extends EntityPersister> getEntityPersisterClass(PersistentClass metadata) {
		if ( RootClass.class.isInstance( metadata ) ) {
            if ( metadata.hasSubclasses() ) {
                metadata = (PersistentClass) metadata.getDirectSubclasses().next();
            }
            else {
			    return singleTableEntityPersister();
            }
		}
		if(SingleTableSubclass.class.isInstance( metadata )){
			return singleTableEntityPersister();
		}
		else if(JoinedSubclass.class.isInstance(metadata)){
			return joinedSubclassEntityPersister();
		}
		else if ( UnionSubclass.class.isInstance( metadata ) ) {
			return unionSubclassEntityPersister();
		}
		else{
		  throw new UnknownPersisterException(
				"Could not determine persister implementation for entity [" + metadata.getEntityName() + "]"
		  );
		}
	}
	
	@Override
	public Class<? extends CollectionPersister> getCollectionPersisterClass(Collection metadata) {
		return metadata.isOneToMany() ? oneToManyPersister() : basicCollectionPersister();
	}
	
	private Class<BasicCollectionPersister> basicCollectionPersister() {
		return BasicCollectionPersister.class;
	}
	
	private Class<OneToManyPersister> oneToManyPersister() {
		return OneToManyPersister.class;
	}
	
	public Class<? extends EntityPersister> joinedSubclassEntityPersister() {
		return JoinedSubclassEntityPersister.class;
	}

    public Class<? extends EntityPersister> singleTableEntityPersister() {
		return SingleTableEntityPersister.class;
	}
    
    public Class<? extends EntityPersister> unionSubclassEntityPersister() {
		return UnionSubclassEntityPersister.class;
	}

}
