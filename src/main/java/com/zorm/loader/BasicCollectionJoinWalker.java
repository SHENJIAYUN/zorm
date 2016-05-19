package com.zorm.loader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.zorm.LockMode;
import com.zorm.engine.LoadQueryInfluencers;
import com.zorm.entity.BasicLoader;
import com.zorm.entity.OuterJoinableAssociation;
import com.zorm.exception.MappingException;
import com.zorm.persister.QueryableCollection;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.sql.JoinFragment;
import com.zorm.sql.Select;
import com.zorm.type.AssociationType;
import com.zorm.util.StringHelper;

public class BasicCollectionJoinWalker extends CollectionJoinWalker {
	private final QueryableCollection collectionPersister;
	
	public BasicCollectionJoinWalker(
			QueryableCollection collectionPersister, 
			int batchSize, 
			String subquery, 
			SessionFactoryImplementor factory, 
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {

		super( factory, loadQueryInfluencers );

		this.collectionPersister = collectionPersister;

		String alias = generateRootAlias( collectionPersister.getRole() );

		walkCollectionTree(collectionPersister, alias);

		List allAssociations = new ArrayList();
		allAssociations.addAll(associations);
		allAssociations.add( OuterJoinableAssociation.createRoot( collectionPersister.getCollectionType(), alias, getFactory() ) );
		initPersisters(allAssociations, LockMode.NONE);
		initStatementString(alias, batchSize, subquery);
	}
	
	@SuppressWarnings("rawtypes")
	private void initStatementString(
			final String alias,
			final int batchSize,
			final String subquery) throws MappingException {

			final int joins = countEntityPersisters( associations );
			final int collectionJoins = countCollectionPersisters( associations ) + 1;

			suffixes = BasicLoader.generateSuffixes( joins );
			collectionSuffixes = BasicLoader.generateSuffixes( joins, collectionJoins );

			StringBuilder whereString = whereString(
					alias, 
					collectionPersister.getKeyColumnNames(), 
					subquery,
					batchSize
				);

			String filter = collectionPersister.filterFragment( alias, getLoadQueryInfluencers().getEnabledFilters() );
			if ( collectionPersister.isManyToMany() ) {
				Iterator itr = associations.iterator();
				AssociationType associationType = ( AssociationType ) collectionPersister.getElementType();
				while ( itr.hasNext() ) {
					OuterJoinableAssociation oja = ( OuterJoinableAssociation ) itr.next();
					if ( oja.getJoinableType() == associationType ) {
						filter += collectionPersister.getManyToManyFilterFragment( 
								oja.getRHSAlias(), 
								getLoadQueryInfluencers().getEnabledFilters() 
							);
					}
				}
			}
			whereString.insert( 0, StringHelper.moveAndToBeginning( filter ) );

			JoinFragment ojf = mergeOuterJoins(associations);
			Select select = new Select( getDialect() )
				.setSelectClause(
					collectionPersister.selectFragment(alias, collectionSuffixes[0] ) +
					selectString(associations)
				)
				.setFromClause( collectionPersister.getTableName(), alias )
				.setWhereClause( whereString.toString()	)
				.setOuterJoins(
					ojf.toFromFragmentString(),
					ojf.toWhereFragmentString()
				);

			sql = select.toStatementString();
		}

}
