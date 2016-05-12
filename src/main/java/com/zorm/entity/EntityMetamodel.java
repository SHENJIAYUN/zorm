package com.zorm.entity;

import java.io.Serializable;
import java.util.*;

import com.zorm.engine.CascadeStyle;
import com.zorm.engine.OptimisticLockStyle;
import com.zorm.engine.ValueInclusion;
import com.zorm.engine.Versioning;
import com.zorm.exception.MappingException;
import com.zorm.exception.ZormException;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.Property;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.tuple.EntityTuplizer;
import com.zorm.tuple.IdentifierProperty;
import com.zorm.tuple.PropertyFactory;
import com.zorm.tuple.VersionProperty;
import com.zorm.type.AssociationType;
import com.zorm.type.EntityType;
import com.zorm.type.Type;
import com.zorm.util.ArrayHelper;
import com.zorm.util.ReflectHelper;

public class EntityMetamodel implements Serializable{
	private static final long serialVersionUID = 6920796273028376097L;
	
	private static final int NO_VERSION_INDX = -66;
	private final IdentifierProperty identifierProperty;
	private final SessionFactoryImplementor sessionFactory;
	private final String name;
	private final String rootName;
	private final EntityType entityType;
	
	private final boolean versioned;

	private final int propertySpan;
	private final int versionPropertyIndex;
	private final StandardProperty[] properties;
	
	private final CascadeStyle[] cascadeStyles;
	private final String[] propertyNames;
	private final Type[] propertyTypes;
	private final boolean[] propertyLaziness;
	private final boolean[] propertyUpdateability;
	private final boolean[] nonlazyPropertyUpdateability;
	private final boolean[] propertyCheckability;
	private final boolean[] propertyInsertability;
	private final ValueInclusion[] insertInclusions;
	private final ValueInclusion[] updateInclusions;
	private final boolean[] propertyNullability;
	private final boolean[] propertyVersionability;
	private final boolean hasInsertGeneratedValues;
	private final boolean hasUpdateGeneratedValues;
	
	private final Map<String, Integer> propertyIndexes = new HashMap<String, Integer>();
	private final boolean hasCollections;
	private final boolean hasMutableProperties;
	private final boolean hasLazyProperties;
	private final boolean hasNonIdentifierPropertyNamedId;

	private final int[] naturalIdPropertyNumbers;
	private final boolean hasImmutableNaturalId;

	private boolean lazy; //not final because proxy factory creation can fail
	private final boolean hasCascades;
	private final boolean mutable;
	private final boolean isAbstract;
	private final boolean selectBeforeUpdate;
	private final boolean dynamicUpdate;
	private final boolean dynamicInsert;
	private final OptimisticLockStyle optimisticLockStyle;

	private final boolean polymorphic;
	private final String superclass;  // superclass entity-name
	private final boolean explicitPolymorphism;
	private final boolean inherited;
	private final boolean hasSubclasses;
	private final Set subclassEntityNames = new HashSet();
	private final Map entityNameByInheritenceClassMap = new HashMap();
   //
	private final EntityMode entityMode;
	private final EntityTuplizer entityTuplizer;
	private final EntityInstrumentationMetadata instrumentationMetadata;
	
	public EntityMetamodel(PersistentClass persistentClass, SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;

		name = persistentClass.getEntityName();
		rootName = persistentClass.getRootClass().getEntityName();
		entityType = sessionFactory.getTypeResolver().getTypeFactory().manyToOne( name );
		
		identifierProperty = PropertyFactory.buildIdentifierProperty(
		        persistentClass,
		        sessionFactory.getIdentifierGenerator( rootName )
			);

		versioned = persistentClass.isVersioned();

		instrumentationMetadata = null;
		boolean hasLazy = false;

		propertySpan = persistentClass.getPropertyClosureSpan();
		properties = new StandardProperty[propertySpan];
		List<Integer> naturalIdNumbers = new ArrayList<Integer>();

		propertyNames = new String[propertySpan];
		propertyTypes = new Type[propertySpan];
		propertyUpdateability = new boolean[propertySpan];
		propertyInsertability = new boolean[propertySpan];
		insertInclusions = new ValueInclusion[propertySpan];
		updateInclusions = new ValueInclusion[propertySpan];
		nonlazyPropertyUpdateability = new boolean[propertySpan];
		propertyCheckability = new boolean[propertySpan];
		propertyNullability = new boolean[propertySpan];
		propertyVersionability = new boolean[propertySpan];
		propertyLaziness = new boolean[propertySpan];
		cascadeStyles = new CascadeStyle[propertySpan];


		Iterator iter = persistentClass.getPropertyClosureIterator();
		int i = 0;
		int tempVersionProperty = NO_VERSION_INDX;
		boolean foundCascade = false;
		boolean foundCollection = false;
		boolean foundMutable = false;
		boolean foundNonIdentifierPropertyNamedId = false;
		boolean foundInsertGeneratedValue = false;
		boolean foundUpdateGeneratedValue = false;
		boolean foundUpdateableNaturalIdProperty = false;

		while ( iter.hasNext() ) {
			Property prop = ( Property ) iter.next();
			
			properties[i] = PropertyFactory.buildStandardProperty( prop, false );

			if ( "id".equals( prop.getName() ) ) {
				foundNonIdentifierPropertyNamedId = true;
			}

			// temporary ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			boolean lazy = prop.isLazy();
			if ( lazy ) hasLazy = true;
			propertyLaziness[i] = lazy;

			propertyNames[i] = properties[i].getName();
			propertyTypes[i] = properties[i].getType();
			propertyNullability[i] = properties[i].isNullable();
			propertyUpdateability[i] = properties[i].isUpdateable();
			propertyInsertability[i] = properties[i].isInsertable();
			insertInclusions[i] = determineInsertValueGenerationType( prop, properties[i] );
			updateInclusions[i] = determineUpdateValueGenerationType( prop, properties[i] );
			propertyVersionability[i] = properties[i].isVersionable();
			nonlazyPropertyUpdateability[i] = properties[i].isUpdateable() && !lazy;
			propertyCheckability[i] = propertyUpdateability[i] ||
					( propertyTypes[i].isAssociationType() && ( (AssociationType) propertyTypes[i] ).isAlwaysDirtyChecked() );

			cascadeStyles[i] = properties[i].getCascadeStyle();

			if ( properties[i].isLazy() ) {
				hasLazy = true;
			}

			if ( properties[i].getCascadeStyle() != CascadeStyle.NONE ) {
				foundCascade = true;
		     }

			if ( indicatesCollection( properties[i].getType() ) ) {
				foundCollection = true;
			}

			if ( propertyTypes[i].isMutable() && propertyCheckability[i] ) {
				foundMutable = true;
			}

			if ( insertInclusions[i] != ValueInclusion.NONE ) {
				foundInsertGeneratedValue = true;
			}

			if ( updateInclusions[i] != ValueInclusion.NONE ) {
				foundUpdateGeneratedValue = true;
			}

			mapPropertyToIndex(prop, i);
			i++;
		}

		if (naturalIdNumbers.size()==0) {
			naturalIdPropertyNumbers = null;
			hasImmutableNaturalId = false;
		}
		else {
			naturalIdPropertyNumbers = ArrayHelper.toIntArray(naturalIdNumbers);
			hasImmutableNaturalId = !foundUpdateableNaturalIdProperty;
		}

		hasInsertGeneratedValues = foundInsertGeneratedValue;
		hasUpdateGeneratedValues = foundUpdateGeneratedValue;

		hasCascades = foundCascade;
		hasNonIdentifierPropertyNamedId = foundNonIdentifierPropertyNamedId;
		versionPropertyIndex = tempVersionProperty;
		hasLazyProperties = hasLazy;

		lazy = persistentClass.isLazy() && (
				!persistentClass.hasPojoRepresentation() ||
				!ReflectHelper.isFinalClass( persistentClass.getProxyInterface() )
		);
		mutable = persistentClass.isMutable();
		if ( persistentClass.isAbstract() == null ) {
			// legacy behavior (with no abstract attribute specified)
			isAbstract = persistentClass.hasPojoRepresentation() &&
			             ReflectHelper.isAbstractClass( persistentClass.getMappedClass() );
		}
		else {
			isAbstract = persistentClass.isAbstract().booleanValue();
			if ( !isAbstract && persistentClass.hasPojoRepresentation() &&
			     ReflectHelper.isAbstractClass( persistentClass.getMappedClass() ) ) {
			}
		}
		selectBeforeUpdate = persistentClass.hasSelectBeforeUpdate();
		dynamicUpdate = persistentClass.useDynamicUpdate();
		dynamicInsert = persistentClass.useDynamicInsert();

		polymorphic = persistentClass.isPolymorphic();
		explicitPolymorphism = persistentClass.isExplicitPolymorphism();
		inherited = persistentClass.isInherited();
		superclass = inherited ?
				persistentClass.getSuperclass().getEntityName() :
				null;
		hasSubclasses = persistentClass.hasSubclasses();

		optimisticLockStyle = interpretOptLockMode( persistentClass.getOptimisticLockMode() );
		final boolean isAllOrDirty =
				optimisticLockStyle == OptimisticLockStyle.ALL
						|| optimisticLockStyle == OptimisticLockStyle.DIRTY;
		if ( isAllOrDirty && !dynamicUpdate ) {
			throw new MappingException( "optimistic-lock=all|dirty requires dynamic-update=\"true\": " + name );
		}
		if ( versionPropertyIndex != NO_VERSION_INDX && isAllOrDirty ) {
			throw new MappingException( "version and optimistic-lock=all|dirty are not a valid combination : " + name );
		}

		hasCollections = foundCollection;
		hasMutableProperties = foundMutable;

		iter = persistentClass.getSubclassIterator();
		while ( iter.hasNext() ) {
			subclassEntityNames.add( ( (PersistentClass) iter.next() ).getEntityName() );
		}
		subclassEntityNames.add( name );

		if ( persistentClass.hasPojoRepresentation() ) {
			entityNameByInheritenceClassMap.put( persistentClass.getMappedClass(), persistentClass.getEntityName() );
			iter = persistentClass.getSubclassIterator();
			while ( iter.hasNext() ) {
				final PersistentClass pc = ( PersistentClass ) iter.next();
				entityNameByInheritenceClassMap.put( pc.getMappedClass(), pc.getEntityName() );
			}
		}

		entityMode = persistentClass.hasPojoRepresentation() ? EntityMode.POJO : EntityMode.MAP;
		final EntityTuplizerFactory entityTuplizerFactory = sessionFactory.getSettings().getEntityTuplizerFactory();
		final String tuplizerClassName = persistentClass.getTuplizerImplClassName( entityMode );
		if ( tuplizerClassName == null ) {
			entityTuplizer = entityTuplizerFactory.constructDefaultTuplizer( entityMode, this, persistentClass );
		}
		else {
			entityTuplizer = entityTuplizerFactory.constructTuplizer( tuplizerClassName, this, persistentClass );
		}
	}
	
	private boolean indicatesCollection(Type type) {
		if ( type.isCollectionType() ) {
			return true;
		}
		return false;
	}
	
	private OptimisticLockStyle interpretOptLockMode(int optimisticLockMode) {
		switch ( optimisticLockMode ) {
			case Versioning.OPTIMISTIC_LOCK_NONE: {
				return OptimisticLockStyle.NONE;
			}
			case Versioning.OPTIMISTIC_LOCK_DIRTY: {
				return OptimisticLockStyle.DIRTY;
			}
			case Versioning.OPTIMISTIC_LOCK_ALL: {
				return OptimisticLockStyle.ALL;
			}
			default: {
				return OptimisticLockStyle.VERSION;
			}
		}
	}
	
	private ValueInclusion determineInsertValueGenerationType(Property mappingProperty, StandardProperty runtimeProperty) {
		if ( runtimeProperty.isInsertGenerated() ) {
			return ValueInclusion.FULL;
		}
//		else if ( mappingProperty.getValue() instanceof Component ) {
//			if ( hasPartialInsertComponentGeneration( ( Component ) mappingProperty.getValue() ) ) {
//				return ValueInclusion.PARTIAL;
//			}
//		}
		return ValueInclusion.NONE;
	}
	
	private ValueInclusion determineUpdateValueGenerationType(Property mappingProperty, StandardProperty runtimeProperty) {
		if ( runtimeProperty.isUpdateGenerated() ) {
			return ValueInclusion.FULL;
		}
//		else if ( mappingProperty.getValue() instanceof Component ) {
//			if ( hasPartialUpdateComponentGeneration( ( Component ) mappingProperty.getValue() ) ) {
//				return ValueInclusion.PARTIAL;
//			}
//		}
		return ValueInclusion.NONE;
	}
	
	private void mapPropertyToIndex(Property prop, int i) {
		propertyIndexes.put( prop.getName(), i );
	}
	
	//***
	public IdentifierProperty getIdentifierProperty() {
		return identifierProperty;
	}

	public String getRootName() {
		return rootName;
	}

	public EntityMode getEntityMode() {
		return entityMode;
	}

	public Type[] getPropertyTypes() {
		return propertyTypes;
	}

	public boolean[] getPropertyUpdateability() {
		return propertyUpdateability;
	}

	public boolean isVersioned() {
		return versioned;
	}

	public int getPropertySpan() {
		return propertySpan;
	}

	public boolean isLazy() {
		return lazy;
	}

	public String getName() {
		return name;
	}

	@SuppressWarnings("rawtypes")
	public Set getSubclassEntityNames() {
		return subclassEntityNames;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	public int getVersionPropertyIndex() {
		return versionPropertyIndex;
	}

	public StandardProperty[] getProperties() {
		return properties;
	}

	public Integer getPropertyIndexOrNull(String basePropertyName) {
		return (Integer) propertyIndexes.get( basePropertyName );
	}

	public boolean hasLazyProperties() {
		return hasLazyProperties;
	}

	public int getPropertyIndex(String propertyName) {
		Integer index = getPropertyIndexOrNull(propertyName);
		if ( index == null ) {
			throw new ZormException("Unable to resolve property: " + propertyName);
		}
		return index.intValue();
	}

	public boolean isInstrumented() {
		//return instrumentationMetadata.isInstrumented();
		return false;
	}

	public boolean isMutable() {
		return mutable;
	}

	public boolean isDynamicInsert() {
		return dynamicInsert;
	}

	public boolean[] getPropertyInsertability() {
		return propertyInsertability;
	}

	public EntityTuplizer getTuplizer() {
		return entityTuplizer;
	}

	public boolean hasSubclasses() {
		return hasSubclasses;
	}

	public boolean hasImmutableNaturalId() {
		return hasImmutableNaturalId;
	}

	public boolean hasNonIdentifierPropertyNamedId() {
		return hasNonIdentifierPropertyNamedId;
	}

	public boolean isPolymorphic() {
		return polymorphic;
	}

	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
	}

	public EntityInstrumentationMetadata getInstrumentationMetadata() {
		return instrumentationMetadata;
	}

	public EntityType getEntityType() {
		return entityType;
	}

	public String[] getPropertyNames() {
		return propertyNames;
	}

	public boolean[] getPropertyLaziness() {
		return propertyLaziness;
	}

	public boolean hasNaturalIdentifier() {
		return naturalIdPropertyNumbers!=null;
	}

	public ValueInclusion[] getPropertyUpdateGenerationInclusions() {
		return updateInclusions;
	}

	public boolean isDynamicUpdate() {
		return dynamicUpdate;
	}

	public boolean[] getNonlazyPropertyUpdateability() {
		return nonlazyPropertyUpdateability;
	}

	public boolean[] getPropertyCheckability() {
		return propertyCheckability;
	}

	public boolean isExplicitPolymorphism() {
		return explicitPolymorphism;
	}

	public String getSuperclass() {
		return superclass;
	}

	public VersionProperty getVersionProperty() {
		if ( NO_VERSION_INDX == versionPropertyIndex ) {
			return null;
		}
		else {
			return ( VersionProperty ) properties[ versionPropertyIndex ];
		}
	}

	public boolean hasInsertGeneratedValues() {
		return hasInsertGeneratedValues;
	}

	public boolean isSelectBeforeUpdate() {
		return selectBeforeUpdate;
	}

	public boolean[] getPropertyVersionability() {
		return propertyVersionability;
	}

	public boolean[] getPropertyNullability() {
		return propertyNullability;
	}

	public ValueInclusion[] getPropertyInsertGenerationInclusions() {
		return insertInclusions;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public boolean isInherited() {
		return inherited;
	}

	public boolean hasCascades() {
		return hasCascades;
	}

	public boolean hasMutableProperties() {
		return hasMutableProperties;
	}

	public int[] getNaturalIdentifierProperties() {
		return naturalIdPropertyNumbers;
	}

	public CascadeStyle[] getCascadeStyles() {
		return cascadeStyles;
	}

	public boolean hasCollections() {
		return hasCollections;
	}

	public boolean hasUpdateGeneratedValues() {
		return hasUpdateGeneratedValues;
	}

}
