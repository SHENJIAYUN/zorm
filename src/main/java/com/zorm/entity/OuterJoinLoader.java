package com.zorm.entity;

import com.zorm.LockMode;
import com.zorm.LockOptions;
import com.zorm.engine.LoadQueryInfluencers;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.persister.entity.Loadable;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.type.EntityType;

@SuppressWarnings("unused")
public abstract class OuterJoinLoader extends BasicLoader{
	protected Loadable[] persisters;
	private LockOptions lockOptions;
	protected LockMode[] lockModeArray;
	protected EntityType[] ownerAssociationTypes;
	protected String sql;
	protected String[] aliases;
	protected String[] suffixes;
	private LoadQueryInfluencers loadQueryInfluencers;
	protected CollectionPersister[] collectionPersisters;
	protected String[] collectionSuffixes;
	protected int[] owners;
	protected int[] collectionOwners;
	
	public OuterJoinLoader(
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) {
		super( factory );
		this.loadQueryInfluencers = loadQueryInfluencers;
	}
	
	@Override
	protected String getSQLString() {
		return sql;
	}
	
	protected final CollectionPersister[] getCollectionPersisters() {
		return collectionPersisters;
	}
	
	protected final Loadable[] getEntityPersisters() {
		return persisters;
	}
	
	protected EntityType[] getOwnerAssociationTypes() {
		return ownerAssociationTypes;
	}
	
	protected LockMode[] getLockModes(LockOptions lockOptions) {
		return lockModeArray;
	}
	
	protected String[] getCollectionSuffixes() {
		return collectionSuffixes;
	}
	
	protected void initFromWalker(JoinWalker walker) {
		persisters = walker.getPersisters();
		collectionPersisters = walker.getCollectionPersisters();
		ownerAssociationTypes = walker.getOwnerAssociationTypes();
		lockOptions = walker.getLockModeOptions();
		lockModeArray = walker.getLockModeArray();
		suffixes = walker.getSuffixes();
		collectionSuffixes = walker.getCollectionSuffixes();
		owners = walker.getOwners();
		collectionOwners = walker.getCollectionOwners();
		sql = walker.getSQLString();
		aliases = walker.getAliases();
	}
	
	protected final int[] getCollectionOwners() {
		return collectionOwners;
	}
	
	protected String[] getSuffixes() {
		return suffixes;
	}
}
