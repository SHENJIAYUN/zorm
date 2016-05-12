package com.zorm.query;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.exception.QueryException;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.persister.entity.PropertyMapping;
import com.zorm.persister.entity.Queryable;
import com.zorm.type.EntityType;
import com.zorm.type.Type;

public class FromElement extends SqlWalkerNode implements DisplayableNode{


	private static final long serialVersionUID = -1293369644172940729L;

	private static final Log log = LogFactory.getLog(FromElement.class);
	
	private String className;
	private String classAlias;
	private String tableAlias;
	private String collectionTableAlias;
	private FromClause fromClause;
	private boolean includeSubclasses = true;
	private boolean collectionJoin = false;
	private FromElement origin;
	private String[] columns;
	private String role;
	private boolean fetch;
	private boolean isAllPropertyFetch;
	private boolean filter = false;
	private int sequence = -1;
	private boolean useFromFragment = false;
	private boolean initialized = false;
	private FromElementType elementType;
	private boolean useWhereFragment = true;
	private List destinations = new LinkedList();
	private boolean manyToMany = false;
	private String withClauseFragment = null;
	private String withClauseJoinAlias;
	private boolean dereferencedBySuperclassProperty;
	private boolean dereferencedBySubclassProperty;
	
	public FromElement() {
	}
	
	protected FromElement(
			FromClause fromClause,
			FromElement origin,
			String alias) {
		this.fromClause = fromClause;
		this.origin = origin;
		this.classAlias = alias;
		this.tableAlias = origin.getTableAlias();
		super.initialize( fromClause.getWalker() );
	}
	
	@Override
	public String getDisplayText() {
		// TODO Auto-generated method stub
		return null;
	}

	public EntityPersister getEntityPersister() {
		return elementType.getEntityPersister();
	}

	public String getTableAlias() {
		return tableAlias;
	}

	public boolean isAllPropertyFetch() {
		return isAllPropertyFetch;
	}

	public String getClassAlias() {
		return classAlias;
	}

	public boolean isFetch() {
		return fetch;
	}

	public void initializeEntity(
	        FromClause fromClause,
	        String className,
	        EntityPersister persister,
	        EntityType type,
	        String classAlias,
	        String tableAlias) {
		doInitialize( fromClause, tableAlias, className, classAlias, persister, type );
		this.sequence = fromClause.nextFromElementCounter();
		initialized = true;
	}
	
	private void doInitialize(FromClause fromClause, String tableAlias, String className, String classAlias,
			  EntityPersister persister, EntityType type) {
       if ( initialized ) {
              throw new IllegalStateException( "Already initialized!!" );
       }
          this.fromClause = fromClause;
          this.tableAlias = tableAlias;
          this.className = className;
           this.classAlias = classAlias;
            this.elementType = new FromElementType( this, persister, type );
          fromClause.registerFromElement( this );
          String alias = classAlias == null ? "<no alias>" : classAlias;
          log.debug( fromClause+":"+ className +" ("+alias+")"+"->"+ tableAlias );
       }
	
	public void setAllPropertyFetch(boolean fetch) {
		isAllPropertyFetch = fetch;
	}

	void checkInitialized() {
		if ( !initialized ) {
			throw new IllegalStateException( "FromElement has not been initialized!" );
		}
	}
	
	public String[] getIdentityColumns() {
		checkInitialized();
		final String table = getTableAlias();
		if ( table == null ) {
			throw new IllegalStateException( "No table alias for node " + this );
		}

		final String propertyName;
		if ( getEntityPersister() != null && getEntityPersister().getEntityMetamodel() != null
				&& getEntityPersister().getEntityMetamodel().hasNonIdentifierPropertyNamedId() ) {
			propertyName = getEntityPersister().getIdentifierPropertyName();
		}
		else {
			propertyName = EntityPersister.ENTITY_ID;
		}

		if ( getWalker().getStatementType() == SqlTokenTypes.SELECT ) {
			return getPropertyMapping( propertyName ).toColumns( table, propertyName );
		}
		else {
			return getPropertyMapping( propertyName ).toColumns( propertyName );
		}
	}
	
	public PropertyMapping getPropertyMapping(String propertyName) {
		return elementType.getPropertyMapping( propertyName );
	}

	public boolean inProjectionList() {
		return !isImplied() && isFromOrJoinFragment();
	}
	
	public boolean isImplied() {
		return false;	// This is an explicit FROM element.
	}
	
	public boolean isFromOrJoinFragment() {
		return getType() == SqlTokenTypes.FROM_FRAGMENT || getType() == SqlTokenTypes.JOIN_FRAGMENT;
	}

	public Type getSelectType() {
		return elementType.getSelectType();
	}

	public FromClause getFromClause() {
		return fromClause;
	}

	public boolean isCollectionOfValuesOrComponents() {
		return elementType.isCollectionOfValuesOrComponents();
	}

	String renderIdentifierSelect(int size, int k) {
		return elementType.renderIdentifierSelect( size, k );
	}

	String renderScalarIdentifierSelect(int i) {
		return elementType.renderScalarIdentifierSelect( i );
	}

	String renderPropertySelect(int size, int k) {
		return elementType.renderPropertySelect( size, k, isAllPropertyFetch );
	}
	
	@Override
    public Type getDataType() {
		return elementType.getDataType();
	}

	public void handlePropertyBeingDereferenced(Type propertySource, String propertyName) {
		if ( propertySource.isComponentType() ) {
			// property name is a sub-path of a component...
			return;
		}

		Queryable persister = getQueryable();
		if ( persister != null ) {
			try {
				Queryable.Declarer propertyDeclarer = persister.getSubclassPropertyDeclarer( propertyName );
				if ( propertyDeclarer == Queryable.Declarer.SUBCLASS ) {
					dereferencedBySubclassProperty = true;
					includeSubclasses = true;
				}
				else if ( propertyDeclarer == Queryable.Declarer.SUPERCLASS ) {
					dereferencedBySuperclassProperty = true;
				}
			}
			catch( QueryException ignore ) {
				// ignore it; the incoming property could not be found so we
				// cannot be sure what to do here.  At the very least, the
				// safest is to simply not apply any dereference toggling...

			}
		}
	}

	public Queryable getQueryable() {
		return elementType.getQueryable();
	}

	public Type getPropertyType(String propertyName, String propertyPath) {
		return elementType.getPropertyType( propertyName, propertyPath );
	}

	public String[] toColumns(String tableAlias, String path, boolean inSelect) {
		return elementType.toColumns( tableAlias, path, inSelect );
	}
}
