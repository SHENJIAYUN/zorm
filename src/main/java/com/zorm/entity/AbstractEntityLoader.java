package com.zorm.entity;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.zorm.LockOptions;
import com.zorm.engine.LoadQueryInfluencers;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.OuterJoinLoadable;
import com.zorm.persister.entity.UniqueEntityLoader;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;

public class AbstractEntityLoader extends OuterJoinLoader implements UniqueEntityLoader{

	protected final OuterJoinLoadable persister;
	protected final Type uniqueKeyType;
	protected final String entityName;
	protected int[] collectionOwners;
	
	public AbstractEntityLoader(
			OuterJoinLoadable persister,
			Type uniqueKeyType,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) {
		super( factory, loadQueryInfluencers );
		this.uniqueKeyType = uniqueKeyType;
		this.entityName = persister.getEntityName();
		this.persister = persister;

	}
	
	public Object load(Serializable id, Object optionalObject, SessionImplementor session, LockOptions lockOptions) {
		return load( session, id, optionalObject, id, lockOptions );
	}

	@SuppressWarnings("rawtypes")
	protected Object load(
			SessionImplementor session,
			Object id,
			Object optionalObject,
			Serializable optionalId,
			LockOptions lockOptions)  {
		List list = loadEntity(
				session,
				id,
				uniqueKeyType,
				optionalObject,
				entityName,
				optionalId,
				persister,
				lockOptions
			);
		if ( list.size()==1 ) {
			return list.get(0);
		}
		else if ( list.size()==0 ) {
			return null;
		}
		else {
			if ( getCollectionOwners()!=null ) {
				return list.get(0);
			}
			else {
				throw new ZormException(
						"More than one row with the given identifier was found: " +
						id +
						", for class: " +
						persister.getEntityName()
					);
			}
		}
	}

	@Override
    protected boolean isSingleRowLoader() {
		return true;
	}
	
	  protected Object getResultColumnOrRow(Object[] row, ResultTransformer transformer, ResultSet rs, SessionImplementor session)
				throws SQLException {
					return row[row.length-1];
	}
	
}
