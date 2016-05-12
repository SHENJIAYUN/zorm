package com.zorm.engine;

import java.io.Serializable;
import java.sql.ResultSet;

import com.zorm.collection.PersistentCollection;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.util.MessageHelper;

public class LoadingCollectionEntry {
	private final ResultSet resultSet;
	private final CollectionPersister persister;
	private final Serializable key;
	private final PersistentCollection collection;

	public LoadingCollectionEntry(
			ResultSet resultSet,
			CollectionPersister persister,
			Serializable key,
			PersistentCollection collection) {
		this.resultSet = resultSet;
		this.persister = persister;
		this.key = key;
		this.collection = collection;
	}

	public ResultSet getResultSet() {
		return resultSet;
	}

	public CollectionPersister getPersister() {
		return persister;
	}

	public Serializable getKey() {
		return key;
	}

	public PersistentCollection getCollection() {
		return collection;
	}

	public String toString() {
		return getClass().getName() + "<rs=" + resultSet + ", coll=" + MessageHelper.collectionInfoString( persister.getRole(), key ) + ">@" + Integer.toHexString( hashCode() );
	}
}
