package com.zorm.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.zorm.FetchMode;
import com.zorm.LockMode;
import com.zorm.LockOptions;
import com.zorm.dialect.Dialect;
import com.zorm.engine.CascadeStyle;
import com.zorm.engine.LoadQueryInfluencers;
import com.zorm.exception.MappingException;
import com.zorm.persister.QueryableCollection;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.persister.entity.Joinable;
import com.zorm.persister.entity.Loadable;
import com.zorm.persister.entity.OuterJoinLoadable;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.sql.InFragment;
import com.zorm.sql.JoinFragment;
import com.zorm.sql.JoinType;
import com.zorm.type.AssociationType;
import com.zorm.type.EntityType;
import com.zorm.type.ForeignKeyDirection;
import com.zorm.type.Type;
import com.zorm.util.ArrayHelper;
import com.zorm.util.JoinHelper;
import com.zorm.util.StringHelper;

public class JoinWalker {
	private final SessionFactoryImplementor factory;
	protected final List associations = new ArrayList();
	private final Set visitedAssociationKeys = new HashSet();
	private final LoadQueryInfluencers loadQueryInfluencers;

	protected String[] suffixes;
	protected String[] collectionSuffixes;
	protected Loadable[] persisters;
	protected int[] owners;
	protected EntityType[] ownerAssociationTypes;
	protected CollectionPersister[] collectionPersisters;
	protected int[] collectionOwners;
	protected String[] aliases;
	protected LockOptions lockOptions;
	protected LockMode[] lockModeArray;
	protected String sql;

	protected JoinWalker(SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) {
		this.factory = factory;
		this.loadQueryInfluencers = loadQueryInfluencers;

	}
	
	protected final void walkEntityTree(
			OuterJoinLoadable persister,
			String alias) throws MappingException {
		walkEntityTree( persister, alias, new PropertyPath(), 0 );
	}

	private void walkEntityTree(final OuterJoinLoadable persister,
			final String alias, final PropertyPath path, final int currentDepth)
			throws MappingException {
		int n = persister.countSubclassProperties();
		for (int i = 0; i < n; i++) {
			Type type = persister.getSubclassPropertyType(i);
			if (type.isAssociationType()) {
				walkEntityAssociationTree(
						(AssociationType) type, 
						persister, 
						i,
						alias,
						path,
						persister.isSubclassPropertyNullable(i),
						currentDepth);
			}
		}
	}

	protected JoinType getJoinType(OuterJoinLoadable persister,
			final PropertyPath path, int propertyNumber,
			AssociationType associationType, FetchMode metadataFetchMode,
			CascadeStyle metadataCascadeStyle, String lhsTable,
			String[] lhsColumns, final boolean nullable, final int currentDepth)
			throws MappingException {
		return getJoinType(associationType, metadataFetchMode, path, lhsTable,
				lhsColumns, nullable, currentDepth, metadataCascadeStyle);
	}

	protected boolean isJoinedFetchEnabledInMapping(FetchMode config,
			AssociationType type) throws MappingException {
		if (!type.isEntityType() && !type.isCollectionType()) {
			return false;
		} else {
			if (config == FetchMode.JOIN)
				return true;
			if (config == FetchMode.SELECT)
				return false;
			if (type.isEntityType()) {
				EntityType entityType = (EntityType) type;
				EntityPersister persister = getFactory().getEntityPersister(
						entityType.getAssociatedEntityName());
				return !persister.hasProxy();
			} else {
				return false;
			}
		}
	}

	protected boolean isJoinedFetchEnabled(AssociationType type,
			FetchMode config, CascadeStyle cascadeStyle) {
		return type.isEntityType()
				&& isJoinedFetchEnabledInMapping(config, type);
	}

	protected boolean isTooDeep(int currentDepth) {
		Integer maxFetchDepth = getFactory().getSettings()
				.getMaximumFetchDepth();
		return maxFetchDepth != null
				&& currentDepth >= maxFetchDepth.intValue();
	}

	protected boolean isDuplicateAssociation(final String lhsTable,
			final String[] lhsColumnNames, final AssociationType type) {
		final String foreignKeyTable;
		final String[] foreignKeyColumns;
		if (type.getForeignKeyDirection() == ForeignKeyDirection.FOREIGN_KEY_FROM_PARENT) {
			foreignKeyTable = lhsTable;
			foreignKeyColumns = lhsColumnNames;
		} else {
			foreignKeyTable = type.getAssociatedJoinable(getFactory())
					.getTableName();
			foreignKeyColumns = JoinHelper
					.getRHSColumnNames(type, getFactory());
		}
		return isDuplicateAssociation(foreignKeyTable, foreignKeyColumns);
	}

	protected boolean isDuplicateAssociation(final String foreignKeyTable,
			final String[] foreignKeyColumns) {
		AssociationKey associationKey = new AssociationKey(foreignKeyColumns,
				foreignKeyTable);
		return !visitedAssociationKeys.add(associationKey);
	}

	private static final class AssociationKey {
		private String[] columns;
		private String table;

		private AssociationKey(String[] columns, String table) {
			this.columns = columns;
			this.table = table;
		}

		@Override
		public boolean equals(Object other) {
			AssociationKey that = (AssociationKey) other;
			return that.table.equals(table)
					&& Arrays.equals(columns, that.columns);
		}

		@Override
		public int hashCode() {
			return table.hashCode(); // TODO: inefficient
		}
	}

	protected boolean isTooManyCollections() {
		return false;
	}

	protected JoinType getJoinType(
			AssociationType associationType,
			FetchMode config, 
			PropertyPath path, 
			String lhsTable,
			String[] lhsColumns, 
			boolean nullable, 
			int currentDepth,
			CascadeStyle cascadeStyle) throws MappingException {
		if (!isJoinedFetchEnabled(associationType, config, cascadeStyle)) {
			return JoinType.NONE;
		}
		if (isTooDeep(currentDepth)
				|| (associationType.isCollectionType() && isTooManyCollections())) {
			return JoinType.NONE;
		}
		if (isDuplicateAssociation(lhsTable, lhsColumns, associationType)) {
			return JoinType.NONE;
		}
		return getJoinType(nullable, currentDepth);
	}

	protected JoinType getJoinType(boolean nullable, int currentDepth) {
		return !nullable && currentDepth <= 0 ? JoinType.INNER_JOIN
				: JoinType.LEFT_OUTER_JOIN;
	}

	private void walkEntityAssociationTree(
			final AssociationType associationType,
			final OuterJoinLoadable persister, final int propertyNumber,
			final String alias, final PropertyPath path,
			final boolean nullable, final int currentDepth)
			throws MappingException {
		String[] aliasedLhsColumns = JoinHelper.getAliasedLHSColumnNames(associationType, alias,propertyNumber, persister, getFactory());
		String[] lhsColumns = JoinHelper.getLHSColumnNames(associationType,propertyNumber, persister, getFactory());
		String lhsTable = JoinHelper.getLHSTableName(associationType,propertyNumber, persister);

		PropertyPath subPath = path.append(persister
				.getSubclassPropertyName(propertyNumber));
		JoinType joinType = getJoinType(persister, subPath, propertyNumber,
				associationType, 
				persister.getFetchMode(propertyNumber),
				persister.getCascadeStyle(propertyNumber), 
				lhsTable,
				lhsColumns, 
				nullable, 
				currentDepth);
		addAssociationToJoinTreeIfNecessary(
				associationType, 
				aliasedLhsColumns,
				alias, 
				subPath, 
				currentDepth, 
				joinType);
	}

	private void addAssociationToJoinTreeIfNecessary(
			final AssociationType type, final String[] aliasedLhsColumns,
			final String alias, final PropertyPath path, int currentDepth,
			final JoinType joinType) throws MappingException {
		if (joinType != JoinType.NONE) {
			addAssociationToJoinTree(
					type, 
					aliasedLhsColumns, 
					alias,
					path,
					currentDepth, 
					joinType);
		}
	}

	protected String generateTableAlias(final int n, final PropertyPath path,
			final Joinable joinable) {
		return StringHelper.generateAlias(joinable.getName(), n);
	}

	protected String getWithClause(PropertyPath path) {
		return "";
	}

	protected boolean hasRestriction(PropertyPath path) {
		return false;
	}

	@SuppressWarnings("unchecked")
	private void addAssociationToJoinTree(
			final AssociationType type,
			final String[] aliasedLhsColumns, 
			final String alias,
			final PropertyPath path, 
			int currentDepth,
			final JoinType joinType) throws MappingException {

		Joinable joinable = type.getAssociatedJoinable(getFactory());

		String subalias = generateTableAlias(associations.size() + 1, path,joinable);

		OuterJoinableAssociation assoc = new OuterJoinableAssociation(
				path,
				type, 
				alias, 
				aliasedLhsColumns, 
				subalias, 
				joinType,
				getWithClause(path), 
				hasRestriction(path), 
				getFactory(),
				loadQueryInfluencers.getEnabledFilters());
		assoc.validateJoin(path.getFullPath());
		associations.add(assoc);

		int nextDepth = currentDepth + 1;
		// path = "";
		if (!joinable.isCollection()) {
			if (joinable instanceof OuterJoinLoadable) {
				walkEntityTree((OuterJoinLoadable) joinable, 
						subalias, 
						path,
						nextDepth);
			}
		} else {
			if (joinable instanceof QueryableCollection) {
				walkCollectionTree((QueryableCollection) joinable, subalias,
						path, nextDepth);
			}
		}

	}

	protected final void walkCollectionTree(QueryableCollection persister,
			String alias) throws MappingException {
		walkCollectionTree(persister, alias, new PropertyPath(), 0);
	}

	private void walkCollectionTree(final QueryableCollection persister,
			final String alias, final PropertyPath path, final int currentDepth)
			throws MappingException {

		if (persister.isOneToMany()) {
			walkEntityTree((OuterJoinLoadable) persister.getElementPersister(),
					alias, path, currentDepth);
		} else {
			Type type = persister.getElementType();
			if (type.isAssociationType()) {
				AssociationType associationType = (AssociationType) type;
				String[] aliasedLhsColumns = persister
						.getElementColumnNames(alias);
				String[] lhsColumns = persister.getElementColumnNames();
				boolean useInnerJoin = currentDepth == 0;
				final JoinType joinType = getJoinType(associationType,
						persister.getFetchMode(), path,
						persister.getTableName(), lhsColumns, !useInnerJoin,
						currentDepth - 1, null // operations which cascade as
												// far as the collection also
												// cascade to collection
												// elements
				);
				addAssociationToJoinTreeIfNecessary(associationType,
						aliasedLhsColumns, alias, path, currentDepth - 1,
						joinType);
			}
		}

	}

	protected SessionFactoryImplementor getFactory() {
		return factory;
	}

	protected static final int countEntityPersisters(List associations)
			throws MappingException {
		int result = 0;
		Iterator iter = associations.iterator();
		while (iter.hasNext()) {
			OuterJoinableAssociation oj = (OuterJoinableAssociation) iter
					.next();
			if (oj.getJoinable().consumesEntityAlias()) {
				result++;
			}
		}
		return result;
	}

	@SuppressWarnings("rawtypes")
	protected static final int countCollectionPersisters(List associations)
			throws MappingException {
		int result = 0;
		Iterator iter = associations.iterator();
		while (iter.hasNext()) {
			OuterJoinableAssociation oj = (OuterJoinableAssociation) iter.next();
			if (oj.getJoinType() == JoinType.LEFT_OUTER_JOIN
					&& oj.getJoinable().isCollection() && !oj.hasRestriction()) {
				result++;
			}
		}
		return result;
	}

	protected void initPersisters(final List associations,
			final LockOptions lockOptions,
			final AssociationInitCallback callback) throws MappingException {
		final int joins = countEntityPersisters(associations);
		final int collections = countCollectionPersisters(associations);

		collectionOwners = collections == 0 ? null : new int[collections];
		collectionPersisters = collections == 0 ? null
				: new CollectionPersister[collections];
		collectionSuffixes = BasicLoader.generateSuffixes(joins + 1,
				collections);

		this.lockOptions = lockOptions;

		persisters = new Loadable[joins];
		aliases = new String[joins];
		owners = new int[joins];
		ownerAssociationTypes = new EntityType[joins];
		lockModeArray = ArrayHelper.fillArray(lockOptions.getLockMode(), joins);

		int i = 0;
		int j = 0;
		Iterator iter = associations.iterator();
		while (iter.hasNext()) {
			final OuterJoinableAssociation oj = (OuterJoinableAssociation) iter.next();
			if (!oj.isCollection()) {

				persisters[i] = (Loadable) oj.getJoinable();
				aliases[i] = oj.getRHSAlias();
				owners[i] = oj.getOwner(associations);
				ownerAssociationTypes[i] = (EntityType) oj.getJoinableType();
				callback.associationProcessed(oj, i);
				i++;

			} else {

				QueryableCollection collPersister = (QueryableCollection) oj
						.getJoinable();
				if (oj.getJoinType() == JoinType.LEFT_OUTER_JOIN
						&& !oj.hasRestriction()) {
					collectionPersisters[j] = collPersister;
					collectionOwners[j] = oj.getOwner(associations);
					j++;
				}

				if (collPersister.isOneToMany()) {
					persisters[i] = (Loadable) collPersister
							.getElementPersister();
					aliases[i] = oj.getRHSAlias();
					callback.associationProcessed(oj, i);
					i++;
				}
			}
		}

		if (ArrayHelper.isAllNegative(owners))
			owners = null;
		if (collectionOwners != null
				&& ArrayHelper.isAllNegative(collectionOwners)) {
			collectionOwners = null;
		}
	}
	
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return loadQueryInfluencers;
	}

	protected String generateRootAlias(final String description) {
		return StringHelper.generateAlias(description, 0);
	}

	protected StringBuilder whereString(String alias, String[] columnNames,
			int batchSize) {
		if (columnNames.length == 1) {
			// if not a composite key, use "foo in (?, ?, ?)" for batching
			// if no batch, and not a composite key, use "foo = ?"
			InFragment in = new InFragment().setColumn(alias, columnNames[0]);
			for (int i = 0; i < batchSize; i++)
				in.addValue("?");
			return new StringBuilder(in.toFragmentString());
		} else {
			// a composite key
			// ConditionFragment byId = new ConditionFragment()
			// .setTableAlias(alias)
			// .setCondition( columnNames, "?" );
			//
			// StringBuilder whereString = new StringBuilder();
			// if ( batchSize==1 ) {
			// // if no batch, use "foo = ? and bar = ?"
			// whereString.append( byId.toFragmentString() );
			// }
			// else {
			// // if a composite key, use
			// "( (foo = ? and bar = ?) or (foo = ? and bar = ?) )" for batching
			// whereString.append('('); //TODO: unnecessary for databases with
			// ANSI-style joins
			// DisjunctionFragment df = new DisjunctionFragment();
			// for ( int i=0; i<batchSize; i++ ) {
			// df.addCondition(byId);
			// }
			// whereString.append( df.toFragmentString() );
			// whereString.append(')'); //TODO: unnecessary for databases with
			// ANSI-style joins
			// }
			// return whereString;
			return null;
		}
	}

	public EntityType[] getOwnerAssociationTypes() {
		return ownerAssociationTypes;
	}

	protected Dialect getDialect() {
		return factory.getDialect();
	}

	protected final String selectString(List associations)
			throws MappingException {

		if (associations.size() == 0) {
			return "";
		} else {
			StringBuilder buf = new StringBuilder(associations.size() * 100);
			int entityAliasCount = 0;
			int collectionAliasCount = 0;
			for (int i = 0; i < associations.size(); i++) {
				OuterJoinableAssociation join = (OuterJoinableAssociation) associations
						.get(i);
				OuterJoinableAssociation next = (i == associations.size() - 1) ? null
						: (OuterJoinableAssociation) associations.get(i + 1);
				final Joinable joinable = join.getJoinable();
				final String entitySuffix = (suffixes == null || entityAliasCount >= suffixes.length) ? null
						: suffixes[entityAliasCount];
				final String collectionSuffix = (collectionSuffixes == null || collectionAliasCount >= collectionSuffixes.length) ? null
						: collectionSuffixes[collectionAliasCount];
				final String selectFragment = joinable.selectFragment(
						next == null ? null : next.getJoinable(),
						next == null ? null : next.getRHSAlias(), join
								.getRHSAlias(), entitySuffix, collectionSuffix,
						join.getJoinType() == JoinType.LEFT_OUTER_JOIN);
				if (selectFragment.trim().length() > 0) {
					buf.append(", ").append(selectFragment);
				}
				if (joinable.consumesEntityAlias())
					entityAliasCount++;
				if (joinable.consumesCollectionAlias()
						&& join.getJoinType() == JoinType.LEFT_OUTER_JOIN)
					collectionAliasCount++;
			}
			return buf.toString();
		}
	}

	protected String orderBy(final List associations, final String orderBy) {
		return mergeOrderings(orderBy(associations), orderBy);
	}

	protected static String mergeOrderings(String ordering1, String ordering2) {
		if (ordering1.length() == 0) {
			return ordering2;
		} else if (ordering2.length() == 0) {
			return ordering1;
		} else {
			return ordering1 + ", " + ordering2;
		}
	}

	protected static final String orderBy(List associations)
			throws MappingException {
		StringBuilder buf = new StringBuilder();
		Iterator iter = associations.iterator();
		OuterJoinableAssociation last = null;
		while (iter.hasNext()) {
			OuterJoinableAssociation oj = (OuterJoinableAssociation) iter
					.next();
			if (oj.getJoinType() == JoinType.LEFT_OUTER_JOIN) { // why does this
																// matter?
				if (oj.getJoinable().isCollection()) {
					// final QueryableCollection queryableCollection =
					// (QueryableCollection) oj.getJoinable();
					// if ( queryableCollection.hasOrdering() ) {
					// final String orderByString =
					// queryableCollection.getSQLOrderByString( oj.getRHSAlias()
					// );
					// buf.append( orderByString ).append(", ");
					// }
				} else {
					// it might still need to apply a collection ordering based
					// on a
					// many-to-many defined order-by...
					if (last != null && last.getJoinable().isCollection()) {
						// final QueryableCollection queryableCollection =
						// (QueryableCollection) last.getJoinable();
						// if ( queryableCollection.isManyToMany() &&
						// last.isManyToManyWith( oj ) ) {
						// if ( queryableCollection.hasManyToManyOrdering() ) {
						// final String orderByString =
						// queryableCollection.getManyToManyOrderByString(
						// oj.getRHSAlias() );
						// buf.append( orderByString ).append(", ");
						// }
						// }
					}
				}
			}
			last = oj;
		}
		if (buf.length() > 0)
			buf.setLength(buf.length() - 2);
		return buf.toString();
	}

	protected final JoinFragment mergeOuterJoins(List associations)
			throws MappingException {
		JoinFragment outerjoin = getDialect().createOuterJoinFragment();
		Iterator iter = associations.iterator();
		OuterJoinableAssociation last = null;
		while (iter.hasNext()) {
			 OuterJoinableAssociation oj = (OuterJoinableAssociation)iter.next();
			 if ( last != null && last.isManyToManyWith( oj ) ) {
			   oj.addManyToManyJoin( outerjoin, ( QueryableCollection )
			   last.getJoinable() );
			 }
			 else {
			   oj.addJoins(outerjoin);
			 }
			 last = oj;
		}
		last = null;
		return outerjoin;
	}

	@SuppressWarnings("rawtypes")
	protected void initPersisters(final List associations,
			final LockMode lockMode) throws MappingException {
		initPersisters(associations, new LockOptions(lockMode));
	}

	protected void initPersisters(final List associations,
			final LockOptions lockOptions) throws MappingException {
		initPersisters(associations, lockOptions,
				AssociationInitCallback.NO_CALLBACK);
	}

	public Loadable[] getPersisters() {
		return persisters;
	}

	public LockOptions getLockModeOptions() {
		return lockOptions;
	}

	public LockMode[] getLockModeArray() {
		return lockModeArray;
	}

	public String[] getSuffixes() {
		return suffixes;
	}

	public String getSQLString() {
		return sql;
	}

	public String[] getAliases() {
		return aliases;
	}

	protected static interface AssociationInitCallback {
		public static final AssociationInitCallback NO_CALLBACK = new AssociationInitCallback() {
			public void associationProcessed(OuterJoinableAssociation oja,
					int position) {
			}
		};

		public void associationProcessed(OuterJoinableAssociation oja,
				int position);
	}

	public CollectionPersister[] getCollectionPersisters() {
		return collectionPersisters;
	}

	public String[] getCollectionSuffixes() {
		return collectionSuffixes;
	}

	public int[] getOwners() {
		return owners;
	}

	public int[] getCollectionOwners() {
		return collectionOwners;
	}
}
