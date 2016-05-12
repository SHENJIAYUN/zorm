package com.zorm.config.annotations;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.Entity;

import com.zorm.annotations.OptimisticLockType;
import com.zorm.annotations.PolymorphismType;
import com.zorm.annotations.reflection.XAnnotatedElement;
import com.zorm.annotations.reflection.XClass;
import com.zorm.config.AccessType;
import com.zorm.config.AnnotationBinder;
import com.zorm.config.BinderHelper;
import com.zorm.config.InheritanceState;
import com.zorm.config.Mappings;
import com.zorm.config.NamingStrategy;
import com.zorm.config.ObjectNameSource;
import com.zorm.config.PropertyHolder;
import com.zorm.config.UniqueConstraintHolder;
import com.zorm.config.ObjectNameNormalizer;
import com.zorm.engine.Versioning;
import com.zorm.exception.AnnotationException;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.MappingException;
import com.zorm.mapping.Join;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.RootClass;
import com.zorm.mapping.Table;
import com.zorm.mapping.TableOwner;
import com.zorm.mapping.Value;
import com.zorm.util.StringHelper;

public class EntityBinder {

	private String name;
	private Mappings mappings;
	private String discriminatorValue = "";
	private PersistentClass persistentClass;
	private boolean ignoreIdAnnotations;
	private boolean lazy;
	private boolean dynamicInsert = false;
	private boolean dynamicUpdate = false;
	private boolean selectBeforeUpdate = false;
	private XClass annotatedClass;
	private XClass proxyClass;
	private InheritanceState inheritanceState;
	private OptimisticLockType optimisticLockType = OptimisticLockType.VERSION;
	private PolymorphismType polymorphismType = PolymorphismType.IMPLICIT;
	private AccessType propertyAccessType = AccessType.DEFAULT;
	private String subselect;
	private boolean wrapIdsInEmbeddedComponents;
	private String where;
	private Boolean forceDiscriminator;
	private Boolean insertableDiscriminator;
	private java.util.Map<String, Join> secondaryTables = new HashMap<String, Join>();
	private java.util.Map<String, Object> secondaryTableJoins = new HashMap<String, Object>();
	private int batchSize = -1;
	
	public EntityBinder() {
	}
	
	public EntityBinder(
			Entity ejb3Ann,
			XClass annotatedClass,
			PersistentClass persistentClass,
			Mappings mappings) {
		this.mappings = mappings;
		this.persistentClass = persistentClass;
		this.annotatedClass = annotatedClass;
		bindEjb3Annotation( ejb3Ann );
	}

	private void bindEjb3Annotation(Entity ejb3Ann) {
        if(ejb3Ann==null) throw new AssertionFailure( "@Entity should always be not null" );	
        if(BinderHelper.isEmptyAnnotationValue( ejb3Ann.name() )){
        	name = StringHelper.unqualify(annotatedClass.getName());
        }
        else{
        	name = ejb3Ann.name();
        }
	}

	public void setInheritanceState(InheritanceState inheritanceState) {
       this.inheritanceState = inheritanceState;		
	}

	public boolean isIgnoreIdAnnotations() {
		return ignoreIdAnnotations;
	}

	public void setPropertyAccessType(AccessType propertyAccessor) {
		this.propertyAccessType = getExplicitAccessType( annotatedClass );
		// only set the access type if there is no explicit access type for this class
		if( this.propertyAccessType == null ) {
			this.propertyAccessType = propertyAccessor;
		}
	}
	
	public AccessType getExplicitAccessType(XAnnotatedElement element) {
		AccessType accessType = null;
		AccessType jpaAccessType = null;
		
		Access access = element.getAnnotation( Access.class );
		if ( access != null ) {
			jpaAccessType = AccessType.getAccessStrategy( access.value() );
		}
		
		if(jpaAccessType!=null){
			accessType = jpaAccessType;
		}
		
		return accessType;
	}

	public void bindEntity() {
		persistentClass.setAbstract(annotatedClass.isAbstract());
		persistentClass.setClassName(annotatedClass.getName());
		persistentClass.setNodeName(name);
		persistentClass.setJpaEntityName(name);
		persistentClass.setEntityName(annotatedClass.getName());
		bindDiscriminatorValue();
		
		persistentClass.setLazy(lazy);
		if(proxyClass != null){
			persistentClass.setProxyInterfaceName(proxyClass.getName());
		}
		persistentClass.setDynamicInsert(dynamicInsert);
		persistentClass.setDynamicUpdate(dynamicUpdate);
		
		if(persistentClass instanceof RootClass){
			RootClass rootClass = (RootClass)persistentClass;
			boolean mutable = true;
			
			rootClass.setMutable(mutable);
			rootClass.setExplicitPolymorphism(isExplicitPolymorphism( polymorphismType ));
		    if(StringHelper.isNotEmpty(where)){
		    	rootClass.setWhere(where);
		    }
		    boolean forceDiscriminatorInSelects = forceDiscriminator==null
		    		?mappings.forceDiscriminatorInSelectsByDefault()
		    				:forceDiscriminator;
		    rootClass.setForceDiscriminator(forceDiscriminatorInSelects);
		    if( insertableDiscriminator != null) {
				rootClass.setDiscriminatorInsertable( insertableDiscriminator );
			}
		}
		
		persistentClass.setOptimisticLockMode(getVersioning(optimisticLockType));
		persistentClass.setSelectBeforeUpdate(selectBeforeUpdate);
		persistentClass.setBatchSize(batchSize);
		
		try {
			mappings.addImport( persistentClass.getEntityName(), name );
			String entityName = persistentClass.getEntityName();
			if ( !entityName.equals( name ) ) {
				mappings.addImport( entityName, entityName );
			}
		}
		catch (MappingException me) {
			throw new AnnotationException( "Use of the same entity name twice: " + name, me );
		}
	}
	
	int getVersioning(OptimisticLockType type) {
      switch(type){
       case VERSION:
    	   return Versioning.OPTIMISTIC_LOCK_VERSION;
       case NONE:
    	   return Versioning.OPTIMISTIC_LOCK_NONE;
       case DIRTY:
    	   return Versioning.OPTIMISTIC_LOCK_DIRTY;
       case ALL:
    	   return Versioning.OPTIMISTIC_LOCK_ALL;
       default:
    	   throw new AssertionFailure( "optimistic locking not supported: " + type );
      }
	}

	private boolean isExplicitPolymorphism(PolymorphismType type) {
        switch( type ){
         case IMPLICIT:
        	 return false;
         case EXPLICIT:
        	 return true;
         default:
        	 throw new AssertionFailure("Unknown polymorphism type: "+type);
        }
	}

	public void bindDiscriminatorValue(){
		if(StringHelper.isEmpty(discriminatorValue)){
			Value discriminator = persistentClass.getDiscriminator();
			if(discriminator==null){
				persistentClass.setDiscriminatorValue( name );
			}
			else if("character".equals(discriminator.getType().getName())){
				throw new AnnotationException(
						"Using default @DiscriminatorValue for a discriminator of type CHAR is not safe"
				);
			}
			else if ( "integer".equals( discriminator.getType().getName() ) ) {
				persistentClass.setDiscriminatorValue( String.valueOf( name.hashCode() ) );
			}
			else {
				persistentClass.setDiscriminatorValue( name ); //Spec compliant
			}
		}
		else {
			//persistentClass.getDiscriminator()
			persistentClass.setDiscriminatorValue( discriminatorValue );
		}
	}

	public void bindTable(
			String schema, 
			String catalog, 
			String tableName,
			List<UniqueConstraintHolder> uniqueConstraints,
			Table denormalizedSuperclassTable) {
		EntityTableObjectNameSource tableNameContext = new EntityTableObjectNameSource( tableName, name );
		EntityTableNamingStrategyHelper namingStrategyHelper = new EntityTableNamingStrategyHelper( name );
		final Table table = TableBinder.buildAndFillTable(
				schema,
				catalog,
				tableNameContext,
				namingStrategyHelper,
				persistentClass.isAbstract(),
				uniqueConstraints,
				denormalizedSuperclassTable,
				mappings,
				this.subselect
				);
		if ( persistentClass instanceof TableOwner ) {
			//设置持久类对应的表
			( (TableOwner) persistentClass ).setTable( table );
		}
		else {
			throw new AssertionFailure( "binding a table for a subclass" );
		}
	}
	
	private static class EntityTableObjectNameSource implements ObjectNameSource {
		private final String explicitName;
		private final String logicalName;

		private EntityTableObjectNameSource(String explicitName, String entityName) {
			//数据库表名
			this.explicitName = explicitName;
			this.logicalName = StringHelper.isNotEmpty( explicitName )
					? explicitName
					: StringHelper.unqualify( entityName );
		}

		public String getExplicitName() {
			return explicitName;
		}

		public String getLogicalName() {
			return logicalName;
		}
	}
	
	private static class EntityTableNamingStrategyHelper implements ObjectNameNormalizer.NamingStrategyHelper {
		private final String entityName;

		private EntityTableNamingStrategyHelper(String entityName) {
			this.entityName = entityName;
		}

		public String determineImplicitName(NamingStrategy strategy) {
			return strategy.classToTableName( entityName );
		}

		public String handleExplicitName(NamingStrategy strategy, String name) {
			return strategy.tableName( name );
		}
	}

	public void setWrapIdsInEmbeddedComponents(boolean wrapIdsInEmbeddedComponents) {
		this.wrapIdsInEmbeddedComponents = wrapIdsInEmbeddedComponents;
	}

	public void setDiscriminatorValue(String discriminatorValue) {
		this.discriminatorValue = discriminatorValue;
	}

	public boolean wrapIdsInEmbeddedComponents() {
		return wrapIdsInEmbeddedComponents;
	}

	public java.util.Map<String, Join> getSecondaryTables() {
		return secondaryTables;
	}

	@SuppressWarnings("unused")
	public void finalSecondaryTableBinding(PropertyHolder propertyHolder) {
		Iterator joins = secondaryTables.values().iterator();
		Iterator joinColumns = secondaryTableJoins.values().iterator();

		while ( joins.hasNext() ) {
			Object uncastedColumn = joinColumns.next();
			Join join = (Join) joins.next();
//			createPrimaryColumnsToSecondaryTable( uncastedColumn, propertyHolder, join );
		}
		mappings.addJoins( persistentClass, secondaryTables );
	}

	public void setProxy(Proxy proxy) {
		lazy = true;
		proxyClass = annotatedClass;
	}

}
