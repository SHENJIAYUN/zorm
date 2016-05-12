package com.zorm.loader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.zorm.LockMode;
import com.zorm.engine.LoadQueryInfluencers;
import com.zorm.entity.BasicLoader;
import com.zorm.entity.JoinWalker;
import com.zorm.entity.OuterJoinableAssociation;
import com.zorm.exception.MappingException;
import com.zorm.persister.QueryableCollection;
import com.zorm.persister.entity.OuterJoinLoadable;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.sql.JoinFragment;
import com.zorm.sql.Select;
import com.zorm.util.StringHelper;

public class OneToManyJoinWalker  extends CollectionJoinWalker {
	private final QueryableCollection oneToManyPersister;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public OneToManyJoinWalker(
			QueryableCollection oneToManyPersister,
			int batchSize,
			String subquery,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		super( factory, loadQueryInfluencers );

		this.oneToManyPersister = oneToManyPersister;

		final OuterJoinLoadable elementPersister = (OuterJoinLoadable) oneToManyPersister.getElementPersister();
		final String alias = generateRootAlias( oneToManyPersister.getRole() );

		walkEntityTree(elementPersister, alias);

		List allAssociations = new ArrayList();
		allAssociations.addAll(associations);
		allAssociations.add( OuterJoinableAssociation.createRoot( oneToManyPersister.getCollectionType(), alias, getFactory() ) );
		initPersisters(allAssociations, LockMode.NONE);
		initStatementString(elementPersister, alias, batchSize, subquery);
	}
	
	private void initStatementString(
			final OuterJoinLoadable elementPersister,
			final String alias,
			final int batchSize,
			final String subquery)
		throws MappingException {

			final int joins = countEntityPersisters( associations );
			suffixes = BasicLoader.generateSuffixes( joins + 1 );

			final int collectionJoins = countCollectionPersisters( associations ) + 1;
			collectionSuffixes = BasicLoader.generateSuffixes( joins + 1, collectionJoins );

			StringBuilder whereString = whereString(
					alias,
					oneToManyPersister.getKeyColumnNames(),
					subquery,
					batchSize
				);
			String filter = oneToManyPersister.filterFragment( alias, getLoadQueryInfluencers().getEnabledFilters() );
			whereString.insert( 0, StringHelper.moveAndToBeginning( filter ) );

			JoinFragment ojf = mergeOuterJoins(associations);
			Select select = new Select( getDialect() )
				.setSelectClause(
					oneToManyPersister.selectFragment(null, null, alias, suffixes[joins], collectionSuffixes[0], true) +
					selectString(associations)
				)
				.setFromClause(
					elementPersister.fromTableFragment(alias) +
					elementPersister.fromJoinFragment(alias, true, true)
				)
				.setWhereClause( whereString.toString() )
				.setOuterJoins(
					ojf.toFromFragmentString(),
					ojf.toWhereFragmentString() +
					elementPersister.whereJoinFragment(alias, true, true)
				);

			select.setOrderByClause( orderBy( associations, oneToManyPersister.getSQLOrderByString(alias) ) );

			if ( getFactory().getSettings().isCommentsEnabled() ) {
				select.setComment( "load one-to-many " + oneToManyPersister.getRole() );
			}

			sql = select.toStatementString();
		}
	
	@Override
    protected boolean isDuplicateAssociation(
		final String foreignKeyTable,
		final String[] foreignKeyColumns) {
		final boolean isSameJoin = oneToManyPersister.getTableName().equals(foreignKeyTable) &&
			Arrays.equals( foreignKeyColumns, oneToManyPersister.getKeyColumnNames() );
		return isSameJoin || super.isDuplicateAssociation(foreignKeyTable, foreignKeyColumns);
	}
}
