package com.zorm.config.annotations;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.FetchMode;
import com.zorm.config.AccessType;
import com.zorm.config.AnnotatedClassType;
import com.zorm.config.BinderHelper;
import com.zorm.config.CollectionSecondPass;
import com.zorm.config.Ejb3Column;
import com.zorm.config.Ejb3JoinColumn;
import com.zorm.config.IndexColumn;
import com.zorm.config.InheritanceState;
import com.zorm.config.PropertyHolder;
import com.zorm.config.PropertyHolderBuilder;
import com.zorm.config.SecondPass;
import com.zorm.annotations.ForeignKey;
import com.zorm.annotations.ManyToMany;
import com.zorm.annotations.OneToMany;
import com.zorm.annotations.reflection.XClass;
import com.zorm.annotations.reflection.XProperty;
import com.zorm.config.Mappings;
import com.zorm.config.AnnotationBinder;
import com.zorm.exception.AnnotationException;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.MappingException;
import com.zorm.mapping.Backref;
import com.zorm.mapping.Collection;
import com.zorm.mapping.Column;
import com.zorm.mapping.DependantValue;
import com.zorm.mapping.Join;
import com.zorm.mapping.KeyValue;
import com.zorm.mapping.ManyToOne;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.Property;
import com.zorm.mapping.SimpleValue;
import com.zorm.mapping.Table;
import com.zorm.type.TypeDef;
import com.zorm.util.StringHelper;

public abstract class CollectionBinder {

	private static final Log log = LogFactory.getLog(CollectionBinder.class);
	
	protected Collection collection;
	private boolean hasToBeSorted;
	private boolean zormExtensionMapping;
	protected IndexColumn indexColumn;
	private String propertyName;
	PropertyHolder propertyHolder;
	private boolean ignoreNotFound;
	private XClass collectionType;
	private Mappings mappings;
	private AccessType accessType;
	private Ejb3Column[] elementColumns;
	private XProperty property;
	private Ejb3JoinColumn[] fkJoinColumns;
	private XClass targetEntity;
	private String cascadeStrategy;
	private boolean oneToMany;
	private String mappedBy;
	private TableBinder tableBinder;
	private Ejb3JoinColumn[] joinColumns;
	private Ejb3JoinColumn[] inverseJoinColumns;
	private Ejb3Column[] mapKeyColumns;
	private Ejb3JoinColumn[] mapKeyManyToManyColumns;
	private boolean cascadeDeleteEnabled;
	private boolean insertable = true;
	private boolean updatable = true;
	private Map<XClass, InheritanceState> inheritanceStatePerClass;
	private XClass declaringClass;
	private boolean declaringClassSet;
	private String explicitType;
	private Properties explicitTypeParameters = new Properties();
	int batchSize = -1;
	private boolean isEmbedded;
	private boolean isExplicitAssociationTable;
	private String hqlOrderBy;
	
	protected CollectionBinder() {
	}

	protected CollectionBinder(boolean sorted) {
		this.hasToBeSorted = sorted;
	}

	public void setEjb3OrderBy(javax.persistence.OrderBy orderByAnn) {
		if ( orderByAnn != null ) {
			hqlOrderBy = orderByAnn.value();
		}
	}
	
	protected Mappings getMappings() {
		return mappings;
	}
	
	public boolean isZormExtensionMapping() {
		return zormExtensionMapping;
	}

	public void setZormExtensionMapping(boolean zormExtensionMapping) {
		this.zormExtensionMapping = zormExtensionMapping;
	}

	public boolean isHasToBeSorted() {
		return hasToBeSorted;
	}

	public void setHasToBeSorted(boolean hasToBeSorted) {
		this.hasToBeSorted = hasToBeSorted;
	}
	
	public IndexColumn getIndexColumn() {
		return indexColumn;
	}

	public void setIndexColumn(IndexColumn indexColumn) {
		this.indexColumn = indexColumn;
	}
	
	public Collection getCollection() {
		return collection;
	}

	public static CollectionBinder getCollectionBinder(
			String entityName,
			XProperty property,
			boolean isIndexed,
			boolean isZormExtensionMapping,
			Mappings mappings) {
		CollectionBinder result;
		if ( property.isArray() ) {
			if ( property.getElementClass().isPrimitive() ) {
				result = new PrimitiveArrayBinder();
			}
			else {
				result = new ArrayBinder();
			}
		}
		else if ( property.isCollection() ) {
			Class returnedClass = property.getCollectionClass();
			if ( java.util.Set.class.equals( returnedClass ) ) {
				result = new SetBinder();
			}
			else if ( java.util.SortedSet.class.equals( returnedClass ) ) {
				result = new SetBinder( true );
			}
			else if ( java.util.Map.class.equals( returnedClass ) ) {
				result = new MapBinder();
			}
			else if ( java.util.SortedMap.class.equals( returnedClass ) ) {
				result = new MapBinder( true );
			}
			else if ( java.util.Collection.class.equals( returnedClass ) ) {
			    result = new BagBinder();
			}
			else if ( java.util.List.class.equals( returnedClass ) ) {
				if ( isIndexed ) {
					result = new ListBinder();
				}
				result = new BagBinder();
			}
			else {
				throw new AnnotationException(
						returnedClass.getName() + " collection not yet supported: "
								+ StringHelper.qualify( entityName, property.getName() )
				);
			}
		}
		else {
			throw new AnnotationException(
					"Illegal attempt to map a non collection as a @OneToMany, @ManyToMany or @CollectionOfElements: "
							+ StringHelper.qualify( entityName, property.getName() )
			);
		}
		result.setZormExtensionMapping( isZormExtensionMapping );

		return result;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setPropertyHolder(PropertyHolder propertyHolder) {
		this.propertyHolder = propertyHolder;
	}

	public void setIgnoreNotFound(boolean ignoreNotFound) {
		this.ignoreNotFound = ignoreNotFound;
	}

	public void setCollectionType(XClass collectionType) {
		this.collectionType = collectionType;
	}

	public void setMappings(Mappings mappings) {
		this.mappings = mappings;
	}

	public void setAccessType(AccessType accessType) {
		this.accessType = accessType;
	}

	public void setElementColumns(Ejb3Column[] elementColumns) {
		this.elementColumns = elementColumns;
	}

	public void setProperty(XProperty property) {
		this.property = property;
	}

	public void setFkJoinColumns(Ejb3JoinColumn[] ejb3JoinColumns) {
		this.fkJoinColumns = ejb3JoinColumns;
	}

	public void setTargetEntity(XClass targetEntity) {
		this.targetEntity = targetEntity;
	}

	public void setCascadeStrategy(String cascadeStrategy) {
		this.cascadeStrategy = cascadeStrategy;
	}

	public void setOneToMany(boolean oneToMany) {
		this.oneToMany = oneToMany;
	}

	public void setMappedBy(String mappedBy) {
		this.mappedBy = mappedBy;
	}

	public void setTableBinder(TableBinder tableBinder) {
		this.tableBinder = tableBinder;
	}

	public void setJoinColumns(Ejb3JoinColumn[] joinColumns) {
		this.joinColumns = joinColumns;
	}
	
	public void setInverseJoinColumns(Ejb3JoinColumn[] inverseJoinColumns) {
		this.inverseJoinColumns = inverseJoinColumns;
	}

	public void setCascadeDeleteEnabled(boolean onDeleteCascade) {
		this.cascadeDeleteEnabled = onDeleteCascade;
	}

	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}
	
	public void setUpdatable(boolean updatable) {
		this.updatable = updatable;
	}

	public void setInheritanceStatePerClass(Map<XClass, InheritanceState> inheritanceStatePerClass) {
		this.inheritanceStatePerClass = inheritanceStatePerClass;
	}

	public void setDeclaringClass(XClass declaringClass) {
		this.declaringClass = declaringClass;
		this.declaringClassSet = true;
	}
	
	protected abstract Collection createCollection(PersistentClass persistentClass);

	public void bind() {
		this.collection = createCollection( propertyHolder.getPersistentClass() );
		String role = StringHelper.qualify( propertyHolder.getPath(), propertyName );
		log.debug( "Collection role: "+role );
		collection.setRole( role );
		collection.setNodeName( propertyName );

		// set explicit type information
		if ( explicitType != null ) {
			final TypeDef typeDef = mappings.getTypeDef( explicitType );
			if ( typeDef == null ) {
				collection.setTypeName( explicitType );
				collection.setTypeParameters( explicitTypeParameters );
			}
			else {
				collection.setTypeName( typeDef.getTypeClass() );
				collection.setTypeParameters( typeDef.getParameters() );
			}
		}

		//set laziness
		defineFetchingStrategy();
		collection.setBatchSize( batchSize );
		collection.setMutable( true );

		//work on association
		boolean isMappedBy = !BinderHelper.isEmptyAnnotationValue( mappedBy );
		
		final boolean includeInOptimisticLockChecks = !isMappedBy;
		collection.setOptimisticLocked( includeInOptimisticLockChecks );

		if (isMappedBy
				&& (property.isAnnotationPresent( JoinColumn.class )
					|| property.isAnnotationPresent( JoinColumns.class )
					|| propertyHolder.getJoinTable( property ) != null ) ) {
			String message = "Associations marked as mappedBy must not define database mappings like @JoinTable or @JoinColumn: ";
			message += StringHelper.qualify( propertyHolder.getPath(), propertyName );
			throw new AnnotationException( message );
		}

		collection.setInverse( isMappedBy );

		//many to many may need some second pass informations
		if ( !oneToMany && isMappedBy ) {
			mappings.addMappedBy( getCollectionType().getName(), mappedBy, propertyName );
		}
		XClass collectionType = getCollectionType();
		if ( inheritanceStatePerClass == null) throw new AssertionFailure( "inheritanceStatePerClass not set" );
		SecondPass sp = getSecondPass(
				fkJoinColumns,
				joinColumns,
				inverseJoinColumns,
				elementColumns,
				mapKeyColumns, mapKeyManyToManyColumns, isEmbedded,
				property, collectionType,
				ignoreNotFound, oneToMany,
				tableBinder, mappings
		);
		if ( collectionType.isAnnotationPresent( Embeddable.class )
				|| property.isAnnotationPresent( ElementCollection.class ) //JPA 2
				) {
			// do it right away, otherwise @ManyToOne on composite element call addSecondPass
			// and raise a ConcurrentModificationException
			//sp.doSecondPass( CollectionHelper.EMPTY_MAP );
			mappings.addSecondPass( sp, !isMappedBy );
		}
		else {
			mappings.addSecondPass( sp, !isMappedBy );
		}

		mappings.addCollection( collection );

		//property building
		PropertyBinder binder = new PropertyBinder();
		binder.setName( propertyName );
		binder.setValue( collection );
		binder.setCascade( cascadeStrategy );
		if ( cascadeStrategy != null && cascadeStrategy.indexOf( "delete-orphan" ) >= 0 ) {
			collection.setOrphanDelete( true );
		}
		binder.setAccessType( accessType );
		binder.setProperty( property );
		binder.setInsertable( insertable );
		binder.setUpdatable( updatable );
		Property prop = binder.makeProperty();
		//we don't care about the join stuffs because the column is on the association table.
		if (! declaringClassSet) throw new AssertionFailure( "DeclaringClass is not set in CollectionBinder while binding" );
		propertyHolder.addProperty( prop, declaringClass );
	}
	
	public SecondPass getSecondPass(
			final Ejb3JoinColumn[] fkJoinColumns,
			final Ejb3JoinColumn[] keyColumns,
			final Ejb3JoinColumn[] inverseColumns,
			final Ejb3Column[] elementColumns,
			final Ejb3Column[] mapKeyColumns,
			final Ejb3JoinColumn[] mapKeyManyToManyColumns,
			final boolean isEmbedded,
			final XProperty property,
			final XClass collType,
			final boolean ignoreNotFound,
			final boolean unique,
			final TableBinder assocTableBinder,
			final Mappings mappings) {
		return new CollectionSecondPass( mappings, collection ) {
			@Override
            public void secondPass(java.util.Map persistentClasses, java.util.Map inheritedMetas) throws MappingException {
				bindStarToManySecondPass(
						persistentClasses, collType, fkJoinColumns, keyColumns, inverseColumns, elementColumns,
						isEmbedded, property, unique, assocTableBinder, ignoreNotFound, mappings
				);
			}
		};
	}
	
	protected boolean bindStarToManySecondPass(
			Map persistentClasses,
			XClass collType,
			Ejb3JoinColumn[] fkJoinColumns,
			Ejb3JoinColumn[] keyColumns,
			Ejb3JoinColumn[] inverseColumns,
			Ejb3Column[] elementColumns,
			boolean isEmbedded,
			XProperty property,
			boolean unique,
			TableBinder associationTableBinder,
			boolean ignoreNotFound,
			Mappings mappings) {
		PersistentClass persistentClass = (PersistentClass) persistentClasses.get( collType.getName() );
		boolean reversePropertyInJoin = false;
		if ( persistentClass != null && StringHelper.isNotEmpty( this.mappedBy ) ) {
			try {
				reversePropertyInJoin = 0 != persistentClass.getJoinNumber(
						persistentClass.getRecursiveProperty( this.mappedBy )
				);
			}
			catch (MappingException e) {
				StringBuilder error = new StringBuilder( 80 );
				error.append( "mappedBy reference an unknown target entity property: " )
						.append( collType ).append( "." ).append( this.mappedBy )
						.append( " in " )
						.append( collection.getOwnerEntityName() )
						.append( "." )
						.append( property.getName() );
				throw new AnnotationException( error.toString() );
			}
		}
		if ( persistentClass != null
				&& !reversePropertyInJoin
				&& oneToMany
				&& !this.isExplicitAssociationTable
				&& ( joinColumns[0].isImplicit() && !BinderHelper.isEmptyAnnotationValue( this.mappedBy ) //implicit @JoinColumn
				|| !fkJoinColumns[0].isImplicit() ) //this is an explicit @JoinColumn
				) {
			//this is a Foreign key
			bindOneToManySecondPass(
					getCollection(),
					persistentClasses,
					fkJoinColumns,
					collType,
					cascadeDeleteEnabled,
					ignoreNotFound, hqlOrderBy,
					mappings,
					inheritanceStatePerClass
			);
			return true;
		}
		else {
			//this is an association table
			bindManyToManySecondPass(
					this.collection,
					persistentClasses,
					keyColumns,
					inverseColumns,
					elementColumns,
					isEmbedded, collType,
					ignoreNotFound, unique,
					cascadeDeleteEnabled,
					associationTableBinder, property, propertyHolder, hqlOrderBy, mappings
			);
			return false;
		}
	}
	
	protected void bindManyToManySecondPass(
			Collection collValue,
			Map persistentClasses,
			Ejb3JoinColumn[] joinColumns,
			Ejb3JoinColumn[] inverseJoinColumns,
			Ejb3Column[] elementColumns,
			boolean isEmbedded,
			XClass collType,
			boolean ignoreNotFound, boolean unique,
			boolean cascadeDeleteEnabled,
			TableBinder associationTableBinder,
			XProperty property,
			PropertyHolder parentPropertyHolder,
			String hqlOrderBy,
			Mappings mappings) throws MappingException {

		PersistentClass collectionEntity = (PersistentClass) persistentClasses.get( collType.getName() );
		boolean isCollectionOfEntities = collectionEntity != null;
		//check for user error
		if ( !isCollectionOfEntities ) {
			if ( property.isAnnotationPresent( ManyToMany.class ) || property.isAnnotationPresent( OneToMany.class ) ) {
				String path = collValue.getOwnerEntityName() + "." + joinColumns[0].getPropertyName();
				throw new AnnotationException(
						"Use of @OneToMany or @ManyToMany targeting an unmapped class: " + path + "[" + collType + "]"
				);
			}
			else {
				JoinTable joinTableAnn = parentPropertyHolder.getJoinTable( property );
				if ( joinTableAnn != null && joinTableAnn.inverseJoinColumns().length > 0 ) {
					String path = collValue.getOwnerEntityName() + "." + joinColumns[0].getPropertyName();
					throw new AnnotationException(
							"Use of @JoinTable.inverseJoinColumns targeting an unmapped class: " + path + "[" + collType + "]"
					);
				}
			}
		}

		boolean mappedBy = !BinderHelper.isEmptyAnnotationValue( joinColumns[0].getMappedBy() );
		if ( mappedBy ) {
			if ( !isCollectionOfEntities ) {
				StringBuilder error = new StringBuilder( 80 )
						.append(
								"Collection of elements must not have mappedBy or association reference an unmapped entity: "
						)
						.append( collValue.getOwnerEntityName() )
						.append( "." )
						.append( joinColumns[0].getPropertyName() );
				throw new AnnotationException( error.toString() );
			}
			Property otherSideProperty;
			try {
				otherSideProperty = collectionEntity.getRecursiveProperty( joinColumns[0].getMappedBy() );
			}
			catch (MappingException e) {
				StringBuilder error = new StringBuilder( 80 );
				error.append( "mappedBy reference an unknown target entity property: " )
						.append( collType ).append( "." ).append( joinColumns[0].getMappedBy() )
						.append( " in " )
						.append( collValue.getOwnerEntityName() )
						.append( "." )
						.append( joinColumns[0].getPropertyName() );
				throw new AnnotationException( error.toString() );
			}
			Table table;
			if ( otherSideProperty.getValue() instanceof Collection ) {
				//this is a collection on the other side
				table = ( (Collection) otherSideProperty.getValue() ).getCollectionTable();
			}
			else {
				//This is a ToOne with a @JoinTable or a regular property
				table = otherSideProperty.getValue().getTable();
			}
			collValue.setCollectionTable( table );
			String entityName = collectionEntity.getEntityName();
			for (Ejb3JoinColumn column : joinColumns) {
				//column.setDefaultColumnHeader( joinColumns[0].getMappedBy() ); //seems not to be used, make sense
				column.setManyToManyOwnerSideEntityName( entityName );
			}
		}
		else {
			for (Ejb3JoinColumn column : joinColumns) {
				String mappedByProperty = mappings.getFromMappedBy(
						collValue.getOwnerEntityName(), column.getPropertyName()
				);
				Table ownerTable = collValue.getOwner().getTable();
				column.setMappedBy(
						collValue.getOwner().getEntityName(), mappings.getLogicalTableName( ownerTable ),
						mappedByProperty
				);
			}
			if ( StringHelper.isEmpty( associationTableBinder.getName() ) ) {
				//default value
				associationTableBinder.setDefaultName(
						collValue.getOwner().getEntityName(),
						mappings.getLogicalTableName( collValue.getOwner().getTable() ),
						collectionEntity != null ? collectionEntity.getEntityName() : null,
						collectionEntity != null ? mappings.getLogicalTableName( collectionEntity.getTable() ) : null,
						joinColumns[0].getPropertyName()
				);
			}
			associationTableBinder.setJPA2ElementCollection( !isCollectionOfEntities && property.isAnnotationPresent( ElementCollection.class ));
			collValue.setCollectionTable( associationTableBinder.bind() );
		}
		bindCollectionSecondPass( collValue, collectionEntity, joinColumns, cascadeDeleteEnabled, property, mappings );

		ManyToOne element = null;
		if ( isCollectionOfEntities ) {
			element =
					new ManyToOne( mappings,  collValue.getCollectionTable() );
			collValue.setElement( element );
			element.setReferencedEntityName( collType.getName() );
			element.setFetchMode( FetchMode.JOIN );
			element.setLazy( false );
			element.setIgnoreNotFound( ignoreNotFound );
			if ( hqlOrderBy != null ) {
				collValue.setManyToManyOrdering(
						buildOrderByClauseFromHql( hqlOrderBy, collectionEntity, collValue.getRole() )
				);
			}
			ForeignKey fk = property != null ? property.getAnnotation( ForeignKey.class ) : null;
			String fkName = fk != null ? fk.inverseName() : "";
			if ( !BinderHelper.isEmptyAnnotationValue( fkName ) )
				element.setForeignKeyName( fkName );
		}
		else {
			XClass elementClass;
			AnnotatedClassType classType;

			PropertyHolder holder = null;
			if ( BinderHelper.PRIMITIVE_NAMES.contains( collType.getName() ) ) {
				classType = AnnotatedClassType.NONE;
				elementClass = null;
			}
			else {
				elementClass = collType;
				classType = mappings.getClassType( elementClass );

				holder = PropertyHolderBuilder.buildPropertyHolder(
						collValue,
						collValue.getRole(),
						elementClass,
						property, parentPropertyHolder, mappings
				);
				//force in case of attribute override
				boolean attributeOverride = property.isAnnotationPresent( AttributeOverride.class )
						|| property.isAnnotationPresent( AttributeOverrides.class );
				if ( isEmbedded || attributeOverride ) {
					classType = AnnotatedClassType.EMBEDDABLE;
				}
			}
				SimpleValueBinder elementBinder = new SimpleValueBinder();
				elementBinder.setMappings( mappings );
				elementBinder.setReturnedClassName( collType.getName() );
				if ( elementColumns == null || elementColumns.length == 0 ) {
					elementColumns = new Ejb3Column[1];
					Ejb3Column column = new Ejb3Column();
					column.setImplicit( false );
					//not following the spec but more clean
					column.setNullable( true );
					column.setLength( Ejb3Column.DEFAULT_COLUMN_LENGTH );
					column.setLogicalColumnName( Collection.DEFAULT_ELEMENT_COLUMN_NAME );
					column.setJoins( new HashMap<String, Join>() );
					column.setMappings( mappings );
					column.bind();
					elementColumns[0] = column;
				}
				//override the table
				for (Ejb3Column column : elementColumns) {
					column.setTable( collValue.getCollectionTable() );
				}
				elementBinder.setColumns( elementColumns );
				elementBinder.setType( property, elementClass, collValue.getOwnerEntityName() );
				elementBinder.setPersistentClassName( propertyHolder.getEntityName() );
				elementBinder.setAccessType( accessType );
				collValue.setElement( elementBinder.make() );
				String orderBy = adjustUserSuppliedValueCollectionOrderingFragment( hqlOrderBy );
				if ( orderBy != null ) {
					collValue.setOrderBy( orderBy );
				}
		}

		if ( isCollectionOfEntities ) {
			bindManytoManyInverseFk( collectionEntity, inverseJoinColumns, element, unique, mappings );
		}

	}
	
	public static void bindManytoManyInverseFk(
			PersistentClass referencedEntity,
			Ejb3JoinColumn[] columns,
			SimpleValue value,
			boolean unique,
			Mappings mappings) {
		final String mappedBy = columns[0].getMappedBy();
		if ( StringHelper.isNotEmpty( mappedBy ) ) {
			final Property property = referencedEntity.getRecursiveProperty( mappedBy );
			Iterator mappedByColumns;
			if ( property.getValue() instanceof Collection ) {
				mappedByColumns = ( (Collection) property.getValue() ).getKey().getColumnIterator();
			}
			else {
				//find the appropriate reference key, can be in a join
				Iterator joinsIt = referencedEntity.getJoinIterator();
				KeyValue key = null;
				while ( joinsIt.hasNext() ) {
					Join join = (Join) joinsIt.next();
					if ( join.containsProperty( property ) ) {
						key = join.getKey();
						break;
					}
				}
				if ( key == null ) key = property.getPersistentClass().getIdentifier();
				mappedByColumns = key.getColumnIterator();
			}
			while ( mappedByColumns.hasNext() ) {
				Column column = (Column) mappedByColumns.next();
				columns[0].linkValueUsingAColumnCopy( column, value );
			}
			String referencedPropertyName =
					mappings.getPropertyReferencedAssociation(
							"inverse__" + referencedEntity.getEntityName(), mappedBy
					);
			if ( referencedPropertyName != null ) {
				( (ManyToOne) value ).setReferencedPropertyName( referencedPropertyName );
				mappings.addUniquePropertyReference( referencedEntity.getEntityName(), referencedPropertyName );
			}
			value.createForeignKey();
		}
		else {
			BinderHelper.createSyntheticPropertyReference( columns, referencedEntity, null, value, true, mappings );
			TableBinder.bindFk( referencedEntity, null, columns, value, unique, mappings );
		}
	}
	
	private static String adjustUserSuppliedValueCollectionOrderingFragment(String orderByFragment) {
		if ( orderByFragment != null ) {
			// NOTE: "$element$" is a specially recognized collection property recognized by the collection persister
			if ( orderByFragment.length() == 0 ) {
				//order by element
				return "$element$ asc";
			}
			else if ( "desc".equals( orderByFragment ) ) {
				return "$element$ desc";
			}
		}
		return orderByFragment;
	}
	
	protected void bindOneToManySecondPass(
			Collection collection,
			Map persistentClasses,
			Ejb3JoinColumn[] fkJoinColumns,
			XClass collectionType,
			boolean cascadeDeleteEnabled,
			boolean ignoreNotFound,
			String hqlOrderBy,
			Mappings mappings,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		com.zorm.mapping.OneToMany oneToMany = new com.zorm.mapping.OneToMany( mappings, collection.getOwner() );
		collection.setElement( oneToMany );
		oneToMany.setReferencedEntityName( collectionType.getName() );
		oneToMany.setIgnoreNotFound( ignoreNotFound );

		String assocClass = oneToMany.getReferencedEntityName();
		PersistentClass associatedClass = (PersistentClass) persistentClasses.get( assocClass );
		String orderBy = buildOrderByClauseFromHql( hqlOrderBy, associatedClass, collection.getRole() );
		if ( orderBy != null ) 
			collection.setOrderBy( orderBy );
		if ( mappings == null ) {
			throw new AssertionFailure(
					"CollectionSecondPass for oneToMany should not be called with null mappings"
			);
		}
		Map<String, Join> joins = mappings.getJoins( assocClass );
		if ( associatedClass == null ) {
			throw new MappingException(
					"Association references unmapped class: " + assocClass
			);
		}
		oneToMany.setAssociatedClass( associatedClass );
		for (Ejb3JoinColumn column : fkJoinColumns) {
			column.setPersistentClass( associatedClass, joins, inheritanceStatePerClass );
			column.setJoins( joins );
			collection.setCollectionTable( column.getTable() );
		}
		bindCollectionSecondPass( collection, null, fkJoinColumns, cascadeDeleteEnabled, property, mappings );
		if ( !collection.isInverse()
				&& !collection.getKey().isNullable() ) {
			// for non-inverse one-to-many, with a not-null fk, add a backref!
			String entityName = oneToMany.getReferencedEntityName();
			PersistentClass referenced = mappings.getClass( entityName );
			Backref prop = new Backref();
			prop.setName( '_' + fkJoinColumns[0].getPropertyName() + '_' + fkJoinColumns[0].getLogicalColumnName() + "Backref" );
			prop.setUpdateable( false );
			prop.setSelectable( false );
			prop.setCollectionRole( collection.getRole() );
			prop.setEntityName( collection.getOwner().getEntityName() );
			prop.setValue( collection.getKey() );
			referenced.addProperty( prop );
		}
	}
	
	private static void bindCollectionSecondPass(
			Collection collValue,
			PersistentClass collectionEntity,
			Ejb3JoinColumn[] joinColumns,
			boolean cascadeDeleteEnabled,
			XProperty property,
			Mappings mappings) {
		BinderHelper.createSyntheticPropertyReference(
				joinColumns, collValue.getOwner(), collectionEntity, collValue, false, mappings
		);
		SimpleValue key = buildCollectionKey( collValue, joinColumns, cascadeDeleteEnabled, property, mappings );
		TableBinder.bindFk( collValue.getOwner(), collectionEntity, joinColumns, key, false, mappings );
	}
	
	private static SimpleValue buildCollectionKey(
			Collection collValue,
			Ejb3JoinColumn[] joinColumns,
			boolean cascadeDeleteEnabled,
			XProperty property,
			Mappings mappings) {
		KeyValue keyVal;
		if ( joinColumns.length > 0 && StringHelper.isNotEmpty( joinColumns[0].getMappedBy() ) ) {
			String entityName = joinColumns[0].getManyToManyOwnerSideEntityName() != null ?
					"inverse__" + joinColumns[0].getManyToManyOwnerSideEntityName() :
					joinColumns[0].getPropertyHolder().getEntityName();
			String propRef = mappings.getPropertyReferencedAssociation(
					entityName,
					joinColumns[0].getMappedBy()
			);
			if ( propRef != null ) {
				collValue.setReferencedPropertyName( propRef );
				mappings.addPropertyReference( collValue.getOwnerEntityName(), propRef );
			}
		}
		String propRef = collValue.getReferencedPropertyName();
		if ( propRef == null ) {
			keyVal = collValue.getOwner().getIdentifier();
		}
		else {
			keyVal = (KeyValue) collValue.getOwner()
					.getReferencedProperty( propRef )
					.getValue();
		}
		DependantValue key = new DependantValue( mappings, collValue.getCollectionTable(), keyVal );
		key.setTypeName( null );
		Ejb3Column.checkPropertyConsistency( joinColumns, collValue.getOwnerEntityName() );
		key.setNullable( joinColumns.length == 0 || joinColumns[0].isNullable() );
		key.setUpdateable( joinColumns.length == 0 || joinColumns[0].isUpdatable() );
		key.setCascadeDeleteEnabled( cascadeDeleteEnabled );
		collValue.setKey( key );
//		ForeignKey fk = property != null ? property.getAnnotation( ForeignKey.class ) : null;
//		String fkName = fk != null ? fk.name() : "";
//		if ( !BinderHelper.isEmptyAnnotationValue( fkName ) ) key.setForeignKeyName( fkName );
		return key;
	}
	
	private static String buildOrderByClauseFromHql(String orderByFragment, PersistentClass associatedClass, String role) {
		if ( orderByFragment != null ) {
			if ( orderByFragment.length() == 0 ) {
				return "id asc";
			}
			else if ( "desc".equals( orderByFragment ) ) {
				return "id desc";
			}
		}
		return orderByFragment;
	}
	
	private XClass getCollectionType() {
		if ( AnnotationBinder.isDefault( targetEntity, mappings ) ) {
			if ( collectionType != null ) {
				return collectionType;
			}
			else {
				String errorMsg = "Collection has neither generic type or OneToMany.targetEntity() defined: "
						+ safeCollectionRole();
				throw new AnnotationException( errorMsg );
			}
		}
		else {
			return targetEntity;
		}
	}
	
	private String safeCollectionRole() {
		if ( propertyHolder != null ) {
			return propertyHolder.getEntityName() + "." + propertyName;
		}
		else {
			return "";
		}
	}
	
	private void defineFetchingStrategy() {
		OneToMany oneToMany = property.getAnnotation( OneToMany.class );
		ManyToMany manyToMany = property.getAnnotation( ManyToMany.class );
		FetchType fetchType;
		if ( oneToMany != null ) {
			fetchType = oneToMany.fetch();
		}
		else if ( manyToMany != null ) {
			fetchType = manyToMany.fetch();
		}
		else {
			throw new AssertionFailure(
					"Define fetch strategy on a property not annotated with @ManyToOne nor @OneToMany nor @CollectionOfElements"
			);
		}
		collection.setLazy( fetchType == FetchType.LAZY );
		collection.setExtraLazy( false );
		collection.setFetchMode( AnnotationBinder.getFetchMode( fetchType ) );
		
	}

	public void setExplicitAssociationTable(boolean explicitAssocTable) {
		this.isExplicitAssociationTable = explicitAssocTable;
	}

}
