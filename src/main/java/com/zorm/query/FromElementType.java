package com.zorm.query;

import java.util.Map;

import com.zorm.exception.QueryException;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.persister.entity.PropertyMapping;
import com.zorm.persister.entity.Queryable;
import com.zorm.type.EntityType;
import com.zorm.type.Type;

public class FromElementType {
	private FromElement fromElement;
	private EntityType entityType;
	private EntityPersister persister;
//	private QueryableCollection queryableCollection;
//	private CollectionPropertyMapping collectionPropertyMapping;
//	private JoinSequence joinSequence;
	private String collectionSuffix;
	private ParameterSpecification indexCollectionSelectorParamSpec;

	public FromElementType(FromElement fromElement, EntityPersister persister, EntityType entityType) {
		this.fromElement = fromElement;
		this.persister = persister;
		this.entityType = entityType;
		if ( persister != null ) {
			fromElement.setText( ( ( Queryable ) persister ).getTableName() + " " + getTableAlias() );
		}
	}
	
	private String getTableAlias() {
		return fromElement.getTableAlias();
	}

	protected FromElementType(FromElement fromElement) {
		this.fromElement = fromElement;
	}

	public EntityPersister getEntityPersister() {
		return persister;
	}
	
	private void checkInitialized() {
		fromElement.checkInitialized();
	}

	public PropertyMapping getPropertyMapping(String propertyName) {
		checkInitialized();
		return (PropertyMapping) persister;
	}

	public Type getSelectType() {
		if (entityType==null) return null;
		boolean shallow = fromElement.getFromClause().getWalker().isShallowQuery();
		return fromElement.getSessionFactoryHelper()
				.getFactory()
				.getTypeResolver()
				.getTypeFactory().manyToOne( entityType.getAssociatedEntityName(), shallow );
	}

	public boolean isCollectionOfValuesOrComponents() {
		return persister == null;
	}

	public String renderIdentifierSelect(int size, int k) {
		checkInitialized();
		if (persister==null) {
			throw new QueryException( "not an entity" );
		}
		String fragment = ( ( Queryable ) persister ).identifierSelectFragment( getTableAlias(), getSuffix( size, k ) );
		return trimLeadingCommaAndSpaces( fragment );
	}
	
	private String getSuffix(int size, int sequence) {
		return generateSuffix( size, sequence );
	}
	
	private static String generateSuffix(int size, int k) {
		String suffix = size == 1 ? "" : Integer.toString( k ) + '_';
		return suffix;
	}
	
	private static String trimLeadingCommaAndSpaces(String fragment) {
		if ( fragment.length() > 0 && fragment.charAt( 0 ) == ',' ) {
			fragment = fragment.substring( 1 );
		}
		fragment = fragment.trim();
		return fragment.trim();
	}
	
	public Type getDataType() {
		if ( persister == null ) {
//			if ( queryableCollection == null ) {
				return null;
//			}
//			return queryableCollection.getType();
		}
		else {
			return entityType;
		}
	}

	String renderScalarIdentifierSelect(int i) {
		checkInitialized();
		String[] cols = getPropertyMapping( EntityPersister.ENTITY_ID ).toColumns( getTableAlias(), EntityPersister.ENTITY_ID );
		StringBuilder buf = new StringBuilder();
		// For property references generate <tablealias>.<columnname> as <projectionalias>
		for ( int j = 0; j < cols.length; j++ ) {
			String column = cols[j];
			if ( j > 0 ) {
				buf.append( ", " );
			}
			buf.append( column ).append( " as " ).append( NameGenerator.scalarName( i, j ) );
		}
		return buf.toString();
	}

	String renderPropertySelect(int size, int k, boolean allProperties) {
		checkInitialized();
		if ( persister == null ) {
			return "";
		}
		else {
			String fragment =  ( ( Queryable ) persister ).propertySelectFragment(
					getTableAlias(),
					getSuffix( size, k ),
					allProperties
			);
			return trimLeadingCommaAndSpaces( fragment );
		}
	}

	public Queryable getQueryable() {
		return ( persister instanceof Queryable ) ? ( Queryable ) persister : null;
	}

	public Type getPropertyType(String propertyName, String propertyPath) {
		checkInitialized();
		Type type = null;
		if ( persister != null && propertyName.equals( propertyPath ) && propertyName.equals( persister.getIdentifierPropertyName() ) ) {
			type = persister.getIdentifierType();
		}else{
			PropertyMapping mapping = getPropertyMapping( propertyName );
			type = mapping.toType( propertyPath );
		}
		return type;
	}

	String[] toColumns(String tableAlias, String path, boolean inSelect) {
		return toColumns( tableAlias, path, inSelect, false );
	}
	
	String[] toColumns(String tableAlias, String path, boolean inSelect, boolean forceAlias) {
		checkInitialized();
		PropertyMapping propertyMapping = getPropertyMapping( path );

        if (forceAlias) {
            return propertyMapping.toColumns(tableAlias, path);
        }

		if (fromElement.getWalker().getStatementType() == SqlTokenTypes.SELECT) {
            return propertyMapping.toColumns(tableAlias, path);
        }

		if (fromElement.getWalker().getCurrentClauseType() == SqlTokenTypes.SELECT) {
            return propertyMapping.toColumns(tableAlias, path);
        }

		if (fromElement.getWalker().isSubQuery()) {
            // for a subquery, the alias to use depends on a few things (we
            // already know this is not an overall SELECT):
            // 1) if this FROM_ELEMENT represents a correlation to the
            // outer-most query
            // A) if the outer query represents a multi-table
            // persister, we need to use the given alias
            // in anticipation of one of the multi-table
            // executors being used (as this subquery will
            // actually be used in the "id select" phase
            // of that multi-table executor)
            // B) otherwise, we need to use the persister's
            // table name as the column qualification
            // 2) otherwise (not correlated), use the given alias
//            if (isCorrelation()) {
//                if (isMultiTable()) {
//					return propertyMapping.toColumns(tableAlias, path);
//				}
//                return propertyMapping.toColumns(extractTableName(), path);
//			}
            return propertyMapping.toColumns(tableAlias, path);
        }

//		if ( isManipulationQuery() && isMultiTable() && inWhereClause() ) {
//			// the actual where-clause will end up being ripped out the update/delete and used in
//			// a select to populate the temp table, so its ok to use the table alias to qualify the table refs
//			// and safer to do so to protect from same-named columns
//			return propertyMapping.toColumns( tableAlias, path );
//		}

		String[] columns = propertyMapping.toColumns( path );
		return columns;
	}
}
