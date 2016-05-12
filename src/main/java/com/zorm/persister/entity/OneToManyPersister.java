package com.zorm.persister.entity;

import com.zorm.FilterAliasGenerator;
import com.zorm.config.Configuration;
import com.zorm.engine.LoadQueryInfluencers;
import com.zorm.exception.MappingException;
import com.zorm.loader.BatchingCollectionInitializer;
import com.zorm.loader.CollectionInitializer;
import com.zorm.mapping.Collection;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.sql.Update;
import com.zorm.util.ArrayHelper;

public class OneToManyPersister extends AbstractCollectionPersister {
	private final boolean cascadeDeleteEnabled;
	private final boolean keyIsNullable;
	private final boolean keyIsUpdateable;

	@Override
	protected boolean isRowDeleteEnabled() {
		return keyIsUpdateable && keyIsNullable;
	}

	@Override
	protected boolean isRowInsertEnabled() {
		return keyIsUpdateable;
	}

	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	public OneToManyPersister(Collection collection, Configuration cfg,
			SessionFactoryImplementor factory) throws MappingException {
		super(collection, cfg, factory);
		cascadeDeleteEnabled = collection.getKey().isCascadeDeleteEnabled()
				&& factory.getDialect().supportsCascadeDelete();
		keyIsNullable = collection.getKey().isNullable();
		keyIsUpdateable = collection.getKey().isUpdateable();
	}

	@Override
	protected CollectionInitializer createCollectionInitializer(
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		return BatchingCollectionInitializer
				.createBatchingOneToManyInitializer(this, batchSize, getFactory(), loadQueryInfluencers);
	}
	
	@Override
    public String getTableName() {
		return ( ( Joinable ) getElementPersister() ).getTableName();
	}

	@Override
	public String selectFragment(
			Joinable rhs, 
			String rhsAlias,
			String lhsAlias, 
			String entitySuffix,
			String collectionSuffix,
			boolean includeCollectionColumns) {
		StringBuilder buf = new StringBuilder();
		if ( includeCollectionColumns ) {
			buf.append( selectFragment( lhsAlias, collectionSuffix ) )
					.append( ", " );
		}
		OuterJoinLoadable ojl = ( OuterJoinLoadable ) getElementPersister();
		return buf.append( ojl.selectFragment( lhsAlias, entitySuffix ) )//use suffix for the entity columns
				.toString();
	}

	@Override
	public String whereJoinFragment(String alias, boolean innerJoin,
			boolean includeSubclasses) {
		return ( ( Joinable ) getElementPersister() ).whereJoinFragment( alias, innerJoin, includeSubclasses );
	}

	@Override
	public String fromJoinFragment(String alias, boolean innerJoin,
			boolean includeSubclasses) {
		return ( ( Joinable ) getElementPersister() ).fromJoinFragment( alias, innerJoin, includeSubclasses );
	}

	@Override
	public boolean consumesEntityAlias() {
		return true;
	}

	@Override
	public boolean consumesCollectionAlias() {
		return true;
	}

	@Override
	public boolean isOneToMany() {
		return true;
	}

	@Override
	public boolean isManyToMany() {
		return false;
	}

	@Override
	protected String generateInsertRowString() {
		Update update = new Update(getDialect()).setTableName(
				qualifiedTableName).addColumns(keyColumnNames);

		if (hasIndex && !indexContainsFormula)
			update.addColumns(indexColumnNames);

		if (getFactory().getSettings().isCommentsEnabled()) {
			update.setComment("create one-to-many row " + getRole());
		}

		return update.addPrimaryKeyColumns(elementColumnNames,
				elementColumnWriters).toStatementString();
	}

	@Override
	protected String generateDeleteString() {
		Update update = new Update(getDialect()).setTableName(
				qualifiedTableName).addColumns(keyColumnNames, "null");

		if (hasIndex && !indexContainsFormula)
			update.addColumns(indexColumnNames, "null");

		if (getFactory().getSettings().isCommentsEnabled()) {
			update.setComment("delete one-to-many row " + getRole());
		}

		String[] rowSelectColumnNames = ArrayHelper.join(keyColumnNames,
				elementColumnNames);
		return update.addPrimaryKeyColumns(rowSelectColumnNames)
				.toStatementString();
	}

	@Override
	protected String generateDeleteRowString() {
		Update update = new Update(getDialect()).setTableName(
				qualifiedTableName).addColumns(keyColumnNames, "null");

		if (hasIndex && !indexContainsFormula)
			update.addColumns(indexColumnNames, "null");

		if (getFactory().getSettings().isCommentsEnabled()) {
			update.setComment("delete one-to-many row " + getRole());
		}

		String[] rowSelectColumnNames = ArrayHelper.join(keyColumnNames,
				elementColumnNames);
		return update.addPrimaryKeyColumns(rowSelectColumnNames)
				.toStatementString();
	}

	@Override
	protected String generateUpdateRowString() {
		return null;
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return getElementPersister().getFilterAliasGenerator(rootAlias);
	}
}
