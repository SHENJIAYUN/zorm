package com.zorm.persister.entity;

import java.util.Map;

import com.zorm.FilterAliasGenerator;
import com.zorm.StaticFilterAliasGenerator;
import com.zorm.config.Configuration;
import com.zorm.engine.LoadQueryInfluencers;
import com.zorm.exception.MappingException;
import com.zorm.exception.QueryException;
import com.zorm.loader.BatchingCollectionInitializer;
import com.zorm.loader.CollectionInitializer;
import com.zorm.mapping.Collection;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.sql.Delete;
import com.zorm.sql.Insert;
import com.zorm.sql.Update;
import com.zorm.type.Type;
import com.zorm.util.ArrayHelper;

public class BasicCollectionPersister extends AbstractCollectionPersister{
	
	public boolean isCascadeDeleteEnabled() {
		return false;
	}
	
	public BasicCollectionPersister(
			Collection collection,
			Configuration cfg,
			SessionFactoryImplementor factory) throws MappingException {
		super( collection, cfg, factory );
	}
	
	@Override
	public boolean isManyToMany() {
		return elementType.isEntityType();
	}

	@Override
	public String selectFragment(Joinable rhs, String rhsAlias,
			String lhsAlias, String currentEntitySuffix,
			String currentCollectionSuffix, boolean includeCollectionColumns) {
		return null;
	}

	@Override
	public String whereJoinFragment(String alias, boolean innerJoin,
			boolean includeSubclasses) {
		return null;
	}

	@Override
	public String fromJoinFragment(String alias, boolean innerJoin,
			boolean includeSubclasses) {
		return null;
	}

	@Override
	public String oneToManyFilterFragment(String alias) throws MappingException {
		return null;
	}

	@Override
	public boolean consumesEntityAlias() {
		return false;
	}
	
	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new StaticFilterAliasGenerator(rootAlias);
	}

	@Override
	public boolean consumesCollectionAlias() {
		return false;
	}

	@Override
	protected String generateInsertRowString() {
		Insert insert = new Insert( getDialect() )
		      .setTableName( qualifiedTableName )
		      .addColumns( keyColumnNames );
		
		if(hasIdentifier){
			insert.addColumn(identifierColumnName);
		}
		
		if(hasIndex){
			insert.addColumn(indexColumnNames, indexColumnIsSettable);
		}
		insert.addColumns( elementColumnNames, elementColumnIsSettable, elementColumnWriters );
		
		return insert.toStatementString();
	}

	@Override
	protected String generateDeleteString() {
		Delete delete = new Delete().setTableName(qualifiedTableName).addPrimaryKeyColumns(keyColumnNames);
		
		if(hasWhere){
			delete.setWhere(sqlWhereString);
		}
		
		return delete.toStatementString();
	}

	@Override
	protected String generateDeleteRowString() {
		Delete delete = new Delete().setTableName(qualifiedTableName);
		
		if ( hasIdentifier ) {
			delete.addPrimaryKeyColumns( new String[]{ identifierColumnName } );
		}
		else if ( hasIndex && !indexContainsFormula ) {
			delete.addPrimaryKeyColumns( ArrayHelper.join( keyColumnNames, indexColumnNames ) );
		}
		else {
			delete.addPrimaryKeyColumns( keyColumnNames );
			delete.addPrimaryKeyColumns( elementColumnNames, elementColumnIsInPrimaryKey, elementColumnWriters );
		}
		
		return delete.toStatementString();
	}

	@Override
	protected String generateUpdateRowString() {
		Update update = new Update( getDialect() )
		     .setTableName( qualifiedTableName );
		update.addColumns(elementColumnNames,elementColumnIsSettable, elementColumnWriters);
		
		if ( hasIdentifier ) {
			update.addPrimaryKeyColumns( new String[]{ identifierColumnName } );
		}
		else if ( hasIndex && !indexContainsFormula ) {
			update.addPrimaryKeyColumns( ArrayHelper.join( keyColumnNames, indexColumnNames ) );
		}
		else {
			update.addPrimaryKeyColumns( keyColumnNames );
			update.addPrimaryKeyColumns( elementColumnNames, elementColumnIsInPrimaryKey, elementColumnWriters );
		}
		
		return update.toStatementString();
	}
	
	@Override
    protected CollectionInitializer createCollectionInitializer(LoadQueryInfluencers loadQueryInfluencers)
			throws MappingException {
		return BatchingCollectionInitializer.createBatchingCollectionInitializer( this, batchSize, getFactory(), loadQueryInfluencers );
	}
	
	@Override
	public boolean isOneToMany() {
		return false;
	}
}
