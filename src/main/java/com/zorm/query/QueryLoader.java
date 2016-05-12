package com.zorm.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import com.zorm.LockMode;
import com.zorm.LockOptions;
import com.zorm.dialect.Dialect;
import com.zorm.entity.BasicLoader;
import com.zorm.entity.ResultTransformer;
import com.zorm.exception.QueryException;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.Loadable;
import com.zorm.persister.entity.Queryable;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.type.EntityType;
import com.zorm.type.Type;
import com.zorm.util.ArrayHelper;

public class QueryLoader extends BasicLoader{
	private QueryTranslatorImpl queryTranslator;

	private Queryable[] entityPersisters;
	private String[] entityAliases;
	private String[] sqlAliases;
	private String[] sqlAliasSuffixes;
	private boolean[] includeInSelect;

	private String[] collectionSuffixes;

	private boolean hasScalars;
	private String[][] scalarColumnNames;
	//private Type[] sqlResultTypes;
	private Type[] queryReturnTypes;

	private final Map sqlAliasByEntityAlias = new HashMap(8);

	private EntityType[] ownerAssociationTypes;
	private int[] owners;
	private boolean[] entityEagerPropertyFetches;

	private int[] collectionOwners;
//	private QueryableCollection[] collectionPersisters;

	private int selectLength;

	private AggregatedSelectExpression aggregatedSelectExpression;
	private String[] queryReturnAliases;

	private LockMode[] defaultLockModes;


	/**
	 * Creates a new Loader implementation.
	 *
	 * @param queryTranslator The query translator that is the delegator.
	 * @param factory The factory from which this loader is being created.
	 * @param selectClause The AST representing the select clause for loading.
	 */
	public QueryLoader(
			final QueryTranslatorImpl queryTranslator,
	        final SessionFactoryImplementor factory,
	        final SelectClause selectClause) {
		super( factory );
		this.queryTranslator = queryTranslator;
		initialize( selectClause );
		postInstantiate();
	}

	private void initialize(SelectClause selectClause) {

		List fromElementList = selectClause.getFromElementsForLoad();

		hasScalars = selectClause.isScalarSelect();
		scalarColumnNames = selectClause.getColumnNames();
		//sqlResultTypes = selectClause.getSqlResultTypes();
		queryReturnTypes = selectClause.getQueryReturnTypes();

		aggregatedSelectExpression = selectClause.getAggregatedSelectExpression();
		queryReturnAliases = selectClause.getQueryReturnAliases();

		int size = fromElementList.size();
		entityPersisters = new Queryable[size];
		entityEagerPropertyFetches = new boolean[size];
		entityAliases = new String[size];
		sqlAliases = new String[size];
		sqlAliasSuffixes = new String[size];
		includeInSelect = new boolean[size];
		owners = new int[size];
		ownerAssociationTypes = new EntityType[size];

		for ( int i = 0; i < size; i++ ) {
			final FromElement element = ( FromElement ) fromElementList.get( i );
			entityPersisters[i] = ( Queryable ) element.getEntityPersister();

			if ( entityPersisters[i] == null ) {
				throw new IllegalStateException( "No entity persister for " + element.toString() );
			}

			entityEagerPropertyFetches[i] = element.isAllPropertyFetch();
			sqlAliases[i] = element.getTableAlias();
			entityAliases[i] = element.getClassAlias();
			sqlAliasByEntityAlias.put( entityAliases[i], sqlAliases[i] );
			// TODO should we just collect these like with the collections above?
			sqlAliasSuffixes[i] = ( size == 1 ) ? "" : Integer.toString( i ) + "_";
//			sqlAliasSuffixes[i] = element.getColumnAliasSuffix();
			includeInSelect[i] = !element.isFetch();
			if ( includeInSelect[i] ) {
				selectLength++;
			}

			owners[i] = -1; //by default
			if ( element.isFetch() ) {
//				if ( element.isCollectionJoin() || element.getQueryableCollection() != null ) {
//					// This is now handled earlier in this method.
//				}
//				else if ( element.getDataType().isEntityType() ) {
//					EntityType entityType = ( EntityType ) element.getDataType();
//					if ( entityType.isOneToOne() ) {
//						owners[i] = fromElementList.indexOf( element.getOrigin() );
//					}
//					ownerAssociationTypes[i] = entityType;
//				}
			}
		}

		//NONE, because its the requested lock mode, not the actual! 
		defaultLockModes = ArrayHelper.fillArray( LockMode.NONE, size );
	}

	public AggregatedSelectExpression getAggregatedSelectExpression() {
		return aggregatedSelectExpression;
	}


	public Loadable[] getEntityPersisters() {
		return entityPersisters;
	}

	public String[] getAliases() {
		return sqlAliases;
	}

	public String[] getSqlAliasSuffixes() {
		return sqlAliasSuffixes;
	}

	public String[] getSuffixes() {
		return getSqlAliasSuffixes();
	}

	public String[] getCollectionSuffixes() {
		return collectionSuffixes;
	}


	protected int[] getCollectionOwners() {
		return collectionOwners;
	}

	protected boolean[] getEntityEagerPropertyFetches() {
		return entityEagerPropertyFetches;
	}
	
	@Override
	protected String applyLocks(
			String sql,
			QueryParameters parameters,
			Dialect dialect,
			List<AfterLoadAction> afterLoadActions) throws QueryException {
		final LockOptions lockOptions = parameters.getLockOptions();

		if ( lockOptions == null ||
			( lockOptions.getLockMode() == LockMode.NONE && lockOptions.getAliasLockCount() == 0 ) ) {
			return sql;
		}
		final LockOptions locks = new LockOptions( lockOptions.getLockMode() );
		final Map keyColumnNames = dialect.forUpdateOfColumns() ? new HashMap() : null;
		return dialect.applyLocksToSql( sql, locks, keyColumnNames );
	}

	/**
	 * An array of indexes of the entity that owns a one-to-one association
	 * to the entity at the given index (-1 if there is no "owner")
	 */
	protected int[] getOwners() {
		return owners;
	}

	protected EntityType[] getOwnerAssociationTypes() {
		return ownerAssociationTypes;
	}



	/**
	 * @param lockOptions a collection of lock modes specified dynamically via the Query interface
	 */
	protected LockMode[] getLockModes(LockOptions lockOptions) {
		if ( lockOptions == null ) {
			return defaultLockModes;
		}

		if ( lockOptions.getAliasLockCount() == 0
				&& ( lockOptions.getLockMode() == null || LockMode.NONE.equals( lockOptions.getLockMode() ) ) ) {
			return defaultLockModes;
		}

		// unfortunately this stuff can't be cached because
		// it is per-invocation, not constant for the
		// QueryTranslator instance

		LockMode[] lockModesArray = new LockMode[entityAliases.length];
		for ( int i = 0; i < entityAliases.length; i++ ) {
			LockMode lockMode = lockOptions.getEffectiveLockMode( entityAliases[i] );
			if ( lockMode == null ) {
				//NONE, because its the requested lock mode, not the actual!
				lockMode = LockMode.NONE;
			}
			lockModesArray[i] = lockMode;
		}

		return lockModesArray;
	}

	protected boolean upgradeLocks() {
		return true;
	}

	protected String[] getResultRowAliases() {
		return queryReturnAliases;
	}
	

	protected boolean[] includeInResultRow() {
		boolean[] includeInResultTuple = includeInSelect;
		if ( hasScalars ) {
			includeInResultTuple = new boolean[ queryReturnTypes.length ];
			Arrays.fill( includeInResultTuple, true );
		}
		return includeInResultTuple;
	}




	// -- Implementation private methods --

	private Object[] toResultRow(Object[] row) {
		if ( selectLength == row.length ) {
			return row;
		}
		else {
			Object[] result = new Object[selectLength];
			int j = 0;
			for ( int i = 0; i < row.length; i++ ) {
				if ( includeInSelect[i] ) {
					result[j++] = row[i];
				}
			}
			return result;
		}
	}

	@Override
	protected String getSQLString() {
		return queryTranslator.getSQLString();
	}

	public List list(SessionImplementor session,
			QueryParameters queryParameters) {
		checkQuery( queryParameters );
		return list( session, queryParameters, queryTranslator.getQuerySpaces(), queryReturnTypes );
	}
	
	private void checkQuery(QueryParameters queryParameters) {
		if ( hasSelectNew() && queryParameters.getResultTransformer() != null ) {
			throw new QueryException( "ResultTransformer is not allowed for 'select new' queries." );
		}
	}
	
	protected Object getResultColumnOrRow(Object[] row, ResultTransformer transformer, ResultSet rs, SessionImplementor session)
			throws SQLException, ZormException {

		Object[] resultRow = getResultRow( row, rs, session );
		boolean hasTransform = hasSelectNew() || transformer!=null;
		return ( ! hasTransform && resultRow.length == 1 ?
				resultRow[ 0 ] :
				resultRow
		);
	}
	
	private boolean hasSelectNew() {
		return aggregatedSelectExpression != null &&  aggregatedSelectExpression.getResultTransformer() != null;
	}
	
    protected List getResultList(List results, ResultTransformer resultTransformer) throws QueryException {
 		// meant to handle dynamic instantiation queries...
 		HolderInstantiator holderInstantiator = buildHolderInstantiator( resultTransformer );
 		if ( holderInstantiator.isRequired() ) {
 			for ( int i = 0; i < results.size(); i++ ) {
 				Object[] row = ( Object[] ) results.get( i );
 				Object result = holderInstantiator.instantiate(row);
 				results.set( i, result );
 			}

 			return results;
 		}
 		else {
 			return results;
 		}
 	}
    
    private HolderInstantiator buildHolderInstantiator(ResultTransformer queryLocalResultTransformer) {
		final ResultTransformer implicitResultTransformer = aggregatedSelectExpression == null
				? null
				: aggregatedSelectExpression.getResultTransformer();
		return HolderInstantiator.getHolderInstantiator(
				implicitResultTransformer,
				queryLocalResultTransformer,
				queryReturnAliases
		);
	}
}
