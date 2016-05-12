package com.zorm.persister.entity;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.zorm.collection.PersistentCollection;
import com.zorm.exception.ZormException;
import com.zorm.meta.CollectionMetadata;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.type.CollectionType;
import com.zorm.type.Type;

public interface CollectionPersister {

	public abstract boolean isCascadeDeleteEnabled();

	public CollectionMetadata getCollectionMetadata();

	public Type getIndexType();

	public String getRole();

	public Type getElementType();

	public void postInstantiate();
	
	public Type getKeyType();
	
	public boolean isOneToMany();

	public boolean isManyToMany();
	
	@SuppressWarnings("rawtypes")
	public String getManyToManyFilterFragment(String alias, Map enabledFilters);
	
	public CollectionType getCollectionType();

	public String[] getKeyColumnAliases(String suffix);

	public String[] getIndexColumnAliases(String suffix);

	public String[] getElementColumnAliases(String suffix);

	public String getIdentifierColumnAlias(String suffix);

	public boolean isMutable();

	public Serializable[] getCollectionSpaces();

	public EntityPersister getOwnerEntityPersister();

	public SessionFactoryImplementor getFactory();

	public boolean hasOrphanDelete();

	public int getBatchSize();

	public boolean isLazy();

	public Object readElement(ResultSet rs, Object owner,String[] suffixedElementAliases, SessionImplementor session) throws ZormException, SQLException;

	public boolean isExtraLazy();

	public int getSize(Serializable loadedKey,SessionImplementor session);

	public boolean isInverse();

	public Object readKey(ResultSet rs, String[] keyAliases, SessionImplementor session) throws ZormException,SQLException;

	public boolean isArray();

}
