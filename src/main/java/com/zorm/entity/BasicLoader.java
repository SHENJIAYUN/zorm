package com.zorm.entity;

import java.util.ArrayList;
import java.util.List;

import com.zorm.LockOptions;
import com.zorm.loader.CollectionAliases;
import com.zorm.loader.GeneratedCollectionAliases;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.persister.entity.Loadable;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.type.BagType;

public abstract class BasicLoader extends Loader{
	protected static final String[] NO_SUFFIX = {""};
	
	private EntityAliases[] descriptors;
	private CollectionAliases[] collectionDescriptors;
	
	public BasicLoader(SessionFactoryImplementor factory) {
		super(factory);
	}
	
	protected final EntityAliases[] getEntityAliases() {
		return descriptors;
	}
	
	protected abstract String[] getSuffixes();
	protected abstract String[] getCollectionSuffixes();
	
	protected final CollectionAliases[] getCollectionAliases() {
		return collectionDescriptors;
	}
	
	protected void postInstantiate() {
		Loadable[] persisters = getEntityPersisters();
		String[] suffixes = getSuffixes();
		descriptors = new EntityAliases[persisters.length];
		for ( int i=0; i<descriptors.length; i++ ) {
			descriptors[i] = new DefaultEntityAliases( persisters[i], suffixes[i] );
		}
		
		CollectionPersister[] collectionPersisters = getCollectionPersisters();
		List bagRoles = null;
		if ( collectionPersisters != null ) {
			String[] collectionSuffixes = getCollectionSuffixes();
			collectionDescriptors = new CollectionAliases[collectionPersisters.length];
			for ( int i = 0; i < collectionPersisters.length; i++ ) {
				if ( isBag( collectionPersisters[i] ) ) {
					if ( bagRoles == null ) {
						bagRoles = new ArrayList();
					}
					bagRoles.add( collectionPersisters[i].getRole() );
				}
				collectionDescriptors[i] = new GeneratedCollectionAliases(
						collectionPersisters[i],
						collectionSuffixes[i]
					);
			}
		}
		else {
			collectionDescriptors = null;
		}
	}

	private boolean isBag(CollectionPersister collectionPersister) {
		return collectionPersister.getCollectionType().getClass().isAssignableFrom( BagType.class );
	}
	
	public static String[] generateSuffixes(int length) {
		return generateSuffixes( 0, length );
	}

	public static String[] generateSuffixes(int seed, int length) {
		if ( length == 0 ) return NO_SUFFIX;

		String[] suffixes = new String[length];
		for ( int i = 0; i < length; i++ ) {
			suffixes[i] = Integer.toString( i + seed ) + "_";
		}
		return suffixes;
	}
}
