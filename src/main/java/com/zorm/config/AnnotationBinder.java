package com.zorm.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.MapsId;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.PrimaryKeyJoinColumns;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.FetchMode;
import com.zorm.annotations.CascadeType;
import com.zorm.annotations.ManyToMany;
import com.zorm.annotations.ManyToOne;
import com.zorm.annotations.OneToMany;
import com.zorm.annotations.OneToOne;
import com.zorm.annotations.reflection.XAnnotatedElement;
import com.zorm.annotations.reflection.XClass;
import com.zorm.annotations.reflection.XProperty;
import com.zorm.config.annotations.CollectionBinder;
import com.zorm.config.annotations.EntityBinder;
import com.zorm.config.annotations.Nullability;
import com.zorm.config.annotations.PropertyBinder;
import com.zorm.config.annotations.TableBinder;
import com.zorm.exception.AnnotationException;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.MappingException;
import com.zorm.mapping.DependantValue;
import com.zorm.mapping.IdGenerator;
import com.zorm.mapping.Join;
import com.zorm.mapping.JoinedSubclass;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.RootClass;
import com.zorm.mapping.SimpleValue;
import com.zorm.mapping.SingleTableSubclass;
import com.zorm.mapping.Subclass;
import com.zorm.mapping.ToOne;
import com.zorm.mapping.UnionSubclass;

public final class AnnotationBinder {

	private static final Log log = LogFactory.getLog(AnnotationBinder.class);
	
	private AnnotationBinder(){}
	
	public static void bindClass(XClass clazzToProcess,Map<XClass, InheritanceState> inheritanceStatePerClass,Mappings mappings){
		
		InheritanceState inheritanceState = inheritanceStatePerClass.get(clazzToProcess);
		//主要关注是否注解有@Entity,即判断clazzToProcess该类是否是Entity
		AnnotatedClassType classType = mappings.getClassType(clazzToProcess);
		
		//判断clazzToProcess是否是Entity
		if(!isEntityClassType(clazzToProcess,classType)){
			return;
		}
		
		//取得父类的Entity
		PersistentClass superEntity = getSuperEntity(clazzToProcess,inheritanceStatePerClass,
				mappings,inheritanceState);
		//没有父类，则初始化为RootClass,RootClass继承PersistentClass
		PersistentClass persistentClass = makePersistentClass( inheritanceState, superEntity );
		Entity entityAnn = clazzToProcess.getAnnotation(Entity.class);
		//将JavaXClass、PersistentClass、继承关系和注解等信息组合在一起
		EntityBinder entityBinder = new EntityBinder(entityAnn,clazzToProcess,persistentClass,mappings);
		entityBinder.setInheritanceState( inheritanceState );		
		
		String schema = "";
		String table = ""; 
		String catalog = "";
		//唯一性约束
		List<UniqueConstraintHolder> uniqueConstraints = new ArrayList<UniqueConstraintHolder>();
		//获取数据库表的信息
		if ( clazzToProcess.isAnnotationPresent( javax.persistence.Table.class ) ) {
			javax.persistence.Table tabAnn = clazzToProcess.getAnnotation( javax.persistence.Table.class );
			//得到@Table注解里面的table等属性信息
			//table:@Table注解的类与数据库映射的表
			table = tabAnn.name();
			schema = tabAnn.schema();
			catalog = tabAnn.catalog();
			//唯一键约束
			uniqueConstraints = TableBinder.buildUniqueConstraintHolders( tabAnn.uniqueConstraints() );
		}
		
		//使用Join继承策略时，父类和子类共有的部分会共存在一张表
		Ejb3JoinColumn[] inheritanceJoinedColumns = makeInheritanceJoinColumns(
				clazzToProcess, mappings, inheritanceState, superEntity
		);
		//使用单表继承策略时，父类和子类数据共用一张表，使用DiscriminatorColumn
		//对父类或子类的数据进行识别
		Ejb3DiscriminatorColumn discriminatorColumn = null;
		if ( InheritanceType.SINGLE_TABLE.equals( inheritanceState.getType() ) ) {
			discriminatorColumn = processDiscriminatorProperties(
					clazzToProcess, mappings, inheritanceState, entityBinder
			);
		}
		
		entityBinder.setProxy( null );
		
		//绑定实体，将annotatedClass的信息注入persistentClass中
		entityBinder.bindEntity();
		
		if(inheritanceState.hasTable()){
			//为persistentClass注入table信息
			entityBinder.bindTable(
					schema,
					catalog,
					table,
					uniqueConstraints,
					inheritanceState.hasDenormalizedTable()?superEntity.getTable():null);
		}else if(clazzToProcess.isAnnotationPresent(Table.class)){
			log.warn("Illegal use of @Table in a subclass of a SINGLE_TABLE hierarchy");
		}
		
		PropertyHolder propertyHolder = PropertyHolderBuilder.buildPropertyHolder(
				clazzToProcess,
				persistentClass,
				entityBinder, mappings, inheritanceStatePerClass
		);
		
		if(InheritanceType.JOINED.equals(inheritanceState.getType()) && inheritanceState.hasParents()){
			final JoinedSubclass jsc = ( JoinedSubclass ) persistentClass;
			SimpleValue key = new DependantValue( mappings, jsc.getTable(), jsc.getIdentifier() );
		    jsc.setKey(key);
		    SecondPass sp = new JoinedSubclassFkSecondPass( jsc, inheritanceJoinedColumns, key, mappings );
			mappings.addSecondPass( sp );
			mappings.addSecondPass( new CreateKeySecondPass( jsc ) );
		}else if(InheritanceType.SINGLE_TABLE.equals(inheritanceState.getType())){
			if(! inheritanceState.hasParents()){
				if(inheritanceState.hasSiblings() || !discriminatorColumn.isImplicit()){
					bindDiscriminatorToPersistentClass(
							( RootClass ) persistentClass,
							discriminatorColumn,
							entityBinder.getSecondaryTables(),
							propertyHolder,
							mappings
					);
					entityBinder.bindDiscriminatorValue();
				}
			}
		}else if(InheritanceType.TABLE_PER_CLASS.equals(inheritanceState.getType())){
			
		}
		
		//获取ID生成器
		HashMap<String, IdGenerator> classGenerators = buildLocalGenerators( clazzToProcess, mappings );
	
		// 获取注解类中的属性，并转化成PropertyData
		final InheritanceState.ElementsToProcess elementsToProcess = inheritanceState.getElementsToProcess();
		inheritanceState.postProcess( persistentClass, entityBinder );
		
		final boolean subclassAndSingleTableStrategy = inheritanceState.getType() == InheritanceType.SINGLE_TABLE
				&& inheritanceState.hasParents();
		Set<String> idPropertiesIfIdClass = new HashSet<String>();
	
		entityBinder.setWrapIdsInEmbeddedComponents(elementsToProcess.getIdPropertyCount()>1);
		
		processIdPropertiesIfNotAlready(
				inheritanceStatePerClass,
				mappings,
				persistentClass,
				entityBinder,
				propertyHolder,
				classGenerators,
				elementsToProcess,
				subclassAndSingleTableStrategy,
				idPropertiesIfIdClass
				);
		
		//继承关系
		if(!inheritanceState.hasParents()){
			final RootClass rootClass = (RootClass) persistentClass;
			mappings.addSecondPass(new CreateKeySecondPass(rootClass));
		}
		else{
			superEntity.addSubClass((Subclass)persistentClass);
		}
		
		mappings.addClass(persistentClass);
		
		mappings.addSecondPass( new SecondaryTableSecondPass( entityBinder, propertyHolder, clazzToProcess ) );
	}
	
	private static Ejb3JoinColumn[] makeInheritanceJoinColumns(
			XClass clazzToProcess,
			Mappings mappings,
			InheritanceState inheritanceState,
			PersistentClass superEntity) {
		Ejb3JoinColumn[] inheritanceJoinedColumns = null;
		final boolean hasJoinedColumns = inheritanceState.hasParents()
				&& InheritanceType.JOINED.equals( inheritanceState.getType() );
		if(hasJoinedColumns){
			PrimaryKeyJoinColumns jcsAnn = clazzToProcess.getAnnotation( PrimaryKeyJoinColumns.class );
			boolean explicitInheritanceJoinedColumns = jcsAnn != null && jcsAnn.value().length != 0;
			if(explicitInheritanceJoinedColumns){
			}
			else{
				PrimaryKeyJoinColumn jcAnn = clazzToProcess.getAnnotation( PrimaryKeyJoinColumn.class );
				inheritanceJoinedColumns = new Ejb3JoinColumn[1];
				inheritanceJoinedColumns[0] = Ejb3JoinColumn.buildJoinColumn(
						jcAnn, null, superEntity.getIdentifier(),
						( Map<String, Join> ) null, ( PropertyHolder ) null, mappings
				);
			}
			log.info("Subclass joined column  created");
		}
		return inheritanceJoinedColumns;
	}
	

	private static void bindDiscriminatorToPersistentClass(
			RootClass rootClass,
			Ejb3DiscriminatorColumn discriminatorColumn,
			Map<String, Join> secondaryTables, 
			PropertyHolder propertyHolder,
			Mappings mappings) {
		if ( rootClass.getDiscriminator() == null ) {
			if ( discriminatorColumn == null ) {
				throw new AssertionFailure( "discriminator column should have been built" );
			}
			discriminatorColumn.setJoins( secondaryTables );
			discriminatorColumn.setPropertyHolder( propertyHolder );
			SimpleValue discrim = new SimpleValue( mappings, rootClass.getTable() );
			rootClass.setDiscriminator( discrim );
			discriminatorColumn.linkWithValue( discrim );
			discrim.setTypeName( discriminatorColumn.getDiscriminatorTypeName() );
			rootClass.setPolymorphic( true );
		}
		
	}

	private static Ejb3DiscriminatorColumn processDiscriminatorProperties(
			XClass clazzToProcess, Mappings mappings,
			InheritanceState inheritanceState, EntityBinder entityBinder) {

		Ejb3DiscriminatorColumn discriminatorColumn = null;
		DiscriminatorColumn discAnn = clazzToProcess.getAnnotation(DiscriminatorColumn.class);
		DiscriminatorType discriminatorType = discAnn!=null?
				discAnn.discriminatorType():
				DiscriminatorType.STRING;
		if(!inheritanceState.hasParents()){
			discriminatorColumn = Ejb3DiscriminatorColumn.buildDiscriminatorColumn(discriminatorType, discAnn, mappings);
		}
		if(discAnn!=null && inheritanceState.hasParents()){
			log.warn("Discriminator column has to be defined in the root entity, it will be ignored in subclass:"+clazzToProcess.getName());
		}
		//获取识别列的值
		String discrimValue = clazzToProcess.isAnnotationPresent(DiscriminatorValue.class)?
				clazzToProcess.getAnnotation(DiscriminatorValue.class).value()
				:null;
		entityBinder.setDiscriminatorValue(discrimValue);
		
		return discriminatorColumn;
	}

	private static void processIdPropertiesIfNotAlready(
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			Mappings mappings, 
			PersistentClass persistentClass,
			EntityBinder entityBinder, 
			PropertyHolder propertyHolder,
			HashMap<String, IdGenerator> classGenerators,
			InheritanceState.ElementsToProcess elementsToProcess,
			boolean subclassAndSingleTableStrategy,
			Set<String> idPropertiesIfIdClass) {
		
		Set<String> missingIdProperties = new HashSet<String>(idPropertiesIfIdClass);
		for(PropertyData propertyAnnotatedElement : elementsToProcess.getElements()){
			String propertyName = propertyAnnotatedElement.getPropertyName();
			if(!idPropertiesIfIdClass.contains(propertyName)){
				processElementAnnotations(propertyHolder,
						subclassAndSingleTableStrategy?Nullability.FORCED_NULL:Nullability.NO_CONSTRAINT, 
						propertyAnnotatedElement, 
						classGenerators, 
						entityBinder, 
						false, 
						false,
						false, 
						mappings, inheritanceStatePerClass);
			}
			else
			{
				missingIdProperties.remove(propertyName);
			}
		}
	}

	private static HashMap<String, IdGenerator> buildLocalGenerators(
			XAnnotatedElement annElt, Mappings mappings) {
		HashMap<String, IdGenerator> generators = new HashMap<String, IdGenerator>();
		TableGenerator tabGen = annElt.getAnnotation( TableGenerator.class );
		SequenceGenerator seqGen = annElt.getAnnotation( SequenceGenerator.class );
		//GenericGenerator genGen = annElt.getAnnotation( GenericGenerator.class );
		if ( tabGen != null ) {
			//IdGenerator idGen = buildIdGenerator( tabGen, mappings );
			//generators.put( idGen.getName(), idGen );
		}
		if ( seqGen != null ) {
			//IdGenerator idGen = buildIdGenerator( seqGen, mappings );
			//generators.put( idGen.getName(), idGen );
		}
		
		return generators;
	}

	/*
	 * 解析属性的注解
	 */
	private static void processElementAnnotations(
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			HashMap<String, IdGenerator> classGenerators,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			boolean isComponentEmbedded,
			boolean inSecondPass,
			Mappings mappings,
			Map<XClass, InheritanceState> inheritanceStatePerClass)throws MappingException{
		
		log.debug("Processing annotations of "+propertyHolder.getEntityName()+"."+inferredData.getPropertyName());
		
		final XProperty property = inferredData.getProperty();
		//从属性的注解信息中解析出相关的数据库列信息
		ColumnsBuilder columnsBuilder = new ColumnsBuilder(propertyHolder, nullability, property, 
				inferredData, entityBinder, mappings).extractMetadata();
	    Ejb3Column[] columns = columnsBuilder.getColumns();
	    Ejb3JoinColumn[] joinColumns = columnsBuilder.getJoinColumns();
	    
	    PropertyBinder propertyBinder = new PropertyBinder();
	    propertyBinder.setName( inferredData.getPropertyName() );
		propertyBinder.setReturnedClassName( inferredData.getTypeName() );
		propertyBinder.setAccessType( inferredData.getDefaultAccess() );
		propertyBinder.setHolder( propertyHolder );
		propertyBinder.setProperty( property );
		propertyBinder.setReturnedClass( inferredData.getPropertyClass() );
		propertyBinder.setMappings( mappings );
		if(isIdentifierMapper){
			propertyBinder.setInsertable( false );
			propertyBinder.setUpdatable( false );
		}
		propertyBinder.setDeclaringClass( inferredData.getDeclaringClass() );
		propertyBinder.setEntityBinder( entityBinder );
		propertyBinder.setInheritanceStatePerClass( inheritanceStatePerClass );
		
		boolean isId = !entityBinder.isIgnoreIdAnnotations() && property.isAnnotationPresent(Id.class);
		propertyBinder.setId(isId);
		
		if(property.isAnnotationPresent(ManyToOne.class)){
			//check validity
			if ( property.isAnnotationPresent( Column.class )) {
				throw new AnnotationException(
						"@Column(s) not allowed on a @ManyToOne property: "
								+ BinderHelper.getPath( propertyHolder, inferredData )
				);
			}
			bindManyToOne(
					joinColumns,
					ToOneBinder.getTargetEntity( inferredData, mappings ),
					propertyHolder,
					inferredData, 
					false, 
					isIdentifierMapper,
					inSecondPass, 
					propertyBinder, 
					mappings
			);
		}
		else if(property.isAnnotationPresent( OneToOne.class ) ){
			
		}
		else if ( property.isAnnotationPresent( OneToMany.class )
				|| property.isAnnotationPresent( ManyToMany.class) ){
			OneToMany oneToManyAnn = property.getAnnotation( OneToMany.class );
			ManyToMany manyToManyAnn = property.getAnnotation( ManyToMany.class );
			
			final IndexColumn indexColumn;
			
			indexColumn = IndexColumn.buildColumnFromAnnotation(
					null,
					propertyHolder,
					inferredData,
					mappings
			);
			
			CollectionBinder collectionBinder = CollectionBinder.getCollectionBinder(
					propertyHolder.getEntityName(),
					property,
					false,
					false,
					mappings
			);
			collectionBinder.setIndexColumn( indexColumn );
			collectionBinder.setPropertyName( inferredData.getPropertyName() );
			collectionBinder.setPropertyHolder( propertyHolder );
			boolean ignoreNotFound = false;
			collectionBinder.setIgnoreNotFound( ignoreNotFound );
			collectionBinder.setCollectionType( inferredData.getProperty().getElementClass() );
			collectionBinder.setMappings( mappings );
			collectionBinder.setAccessType( inferredData.getDefaultAccess() );
			
			Ejb3Column[] elementColumns;
			boolean isJPA2ForValueMapping = property.isAnnotationPresent( ElementCollection.class );
			PropertyData virtualProperty = isJPA2ForValueMapping ? inferredData : new WrappedInferredData(
					inferredData, "element"
			);
			if(property.isAnnotationPresent(Column.class)){
				Column ann = property.getAnnotation( Column.class );
				elementColumns = Ejb3Column.buildColumnFromAnnotation(
						new Column[] { ann },
						nullability,
						propertyHolder,
						virtualProperty,
						mappings
				);
			}
			else{
				elementColumns = Ejb3Column.buildColumnFromAnnotation(
						null,
						nullability,
						propertyHolder,
						virtualProperty,
						mappings
				);
			}
			
			collectionBinder.setElementColumns( elementColumns );
			collectionBinder.setProperty( property );
			
			if ( oneToManyAnn != null && manyToManyAnn != null ) {
				throw new AnnotationException(
						"@OneToMany and @ManyToMany on the same property is not allowed: "
								+ propertyHolder.getEntityName() + "." + inferredData.getPropertyName()
				);
			}
			String mappedBy = null;
			if ( oneToManyAnn != null ) {
				collectionBinder.setFkJoinColumns( joinColumns );
				mappedBy = oneToManyAnn.mappedBy();
				collectionBinder.setTargetEntity(
						mappings.getReflectionManager().toXClass( oneToManyAnn.targetEntity() )
				);
				collectionBinder.setCascadeStrategy(
						getCascadeStrategy(oneToManyAnn.cascade(), false
				));
				collectionBinder.setOneToMany( true );
			}
			else if ( manyToManyAnn != null ) {
				mappedBy = manyToManyAnn.mappedBy();
				collectionBinder.setTargetEntity(
						mappings.getReflectionManager().toXClass( manyToManyAnn.targetEntity() )
				);
				collectionBinder.setCascadeStrategy(
						getCascadeStrategy(
								manyToManyAnn.cascade(), false
						)
				);
				collectionBinder.setOneToMany( false );
			}
			collectionBinder.setMappedBy( mappedBy );
			
			bindJoinedTableAssociation(
					property, mappings, entityBinder, collectionBinder, propertyHolder, inferredData, mappedBy
			);
			
			boolean onDeleteCascade = false;
			collectionBinder.setCascadeDeleteEnabled( onDeleteCascade );
			if ( isIdentifierMapper ) {
				collectionBinder.setInsertable( false );
				collectionBinder.setUpdatable( false );
			}
			collectionBinder.setInheritanceStatePerClass( inheritanceStatePerClass );
			collectionBinder.setDeclaringClass( inferredData.getDeclaringClass() );
			collectionBinder.bind();
		}
		else if ( !isId || !entityBinder.isIgnoreIdAnnotations() ) {
			boolean isOverridden = false;
			if(isId || propertyHolder.isOrWithinEmbeddedId()||propertyHolder.isInIdClass()){
				final PropertyData overridingProperty = BinderHelper.getPropertyOverriddenByMapperOrMapsId(isId, propertyHolder, property.getName(), mappings);
			    if(overridingProperty!=null){
			    	
			    }
			}
			boolean optional = true;
			boolean lazy = false;
			if ( isId || ( !optional && nullability != Nullability.FORCED_NULL ) ) {
				for ( Ejb3Column col : columns ) {
					col.forceNotNull();
				}
			}
			
			propertyBinder.setLazy(lazy);
			//设置数据库列的信息
			propertyBinder.setColumns(columns);
			
			//???
			propertyBinder.makePropertyValueAndBind();
			
			if(isId){
				final SimpleValue value = (SimpleValue)propertyBinder.getValue();
			    if(!isOverridden){
			    	processId(
							propertyHolder,
							inferredData,
							value,
							classGenerators,
							isIdentifierMapper,
							mappings
					);
			    }
			}	
		}		
	}

	private static void bindJoinedTableAssociation(
			XProperty property,
			Mappings mappings, 
			EntityBinder entityBinder,
			CollectionBinder collectionBinder, 
			PropertyHolder propertyHolder,
			PropertyData inferredData, 
			String mappedBy) {
		TableBinder associationTableBinder = new TableBinder();
		JoinColumn[] annJoins;
		JoinColumn[] annInverseJoins;
		JoinTable assocTable = propertyHolder.getJoinTable( property );
		
		if(assocTable != null){
			final String catalog = assocTable.catalog();
			final String schema = assocTable.schema();
			final String tableName = assocTable.name();
			final UniqueConstraint[] uniqueConstraints = assocTable.uniqueConstraints();
			final JoinColumn[] joins = assocTable.joinColumns();
			final JoinColumn[] inverseJoins = assocTable.inverseJoinColumns();
			
			collectionBinder.setExplicitAssociationTable( true );
			
			if(!BinderHelper.isEmptyAnnotationValue(schema)){
				associationTableBinder.setSchema(schema);
			}
			
			if(!BinderHelper.isEmptyAnnotationValue(catalog)){
				associationTableBinder.setCatalog(catalog);
			}
			
			if(!BinderHelper.isEmptyAnnotationValue(tableName)){
				associationTableBinder.setName(tableName);
			}
			
			associationTableBinder.setUniqueConstraints(uniqueConstraints);
			
			annJoins = joins.length == 0 ? null : joins;
			annInverseJoins = inverseJoins == null || inverseJoins.length == 0 ? null : inverseJoins;
			
		}
		else{
			annJoins = null;
			annInverseJoins = null;
		}
		
		Ejb3JoinColumn[] joinColumns = Ejb3JoinColumn.buildJoinTableJoinColumns(
				annJoins,  entityBinder.getSecondaryTables(),propertyHolder, inferredData.getPropertyName(), mappedBy,
				mappings
		);
		
		Ejb3JoinColumn[] inverseJoinColumns = Ejb3JoinColumn.buildJoinTableJoinColumns(
				annInverseJoins, entityBinder.getSecondaryTables(), propertyHolder, inferredData.getPropertyName(),
				mappedBy, mappings
		);
		
		associationTableBinder.setMappings( mappings );
		collectionBinder.setTableBinder( associationTableBinder );
		collectionBinder.setJoinColumns( joinColumns );
		collectionBinder.setInverseJoinColumns( inverseJoinColumns );
	}

	private static String getCascadeStrategy(
			CascadeType[] ejbCascades,
			boolean forcePersist) {
		EnumSet<CascadeType> zormCascadeSet = convertToZormCascadeType( ejbCascades );

		StringBuilder cascade = new StringBuilder();
		for ( CascadeType aHibernateCascadeSet : zormCascadeSet ) {
			switch ( aHibernateCascadeSet ) {
				case ALL:
					cascade.append( "," ).append( "all" );
					break;		
			}
		}
		return cascade.length() > 0 ?
				cascade.substring( 1 ) :
				"none";
	}
	
	private static EnumSet<CascadeType> convertToZormCascadeType(CascadeType[] ejbCascades) {
		EnumSet<CascadeType> zormCascadeSet = EnumSet.noneOf( CascadeType.class );
		if ( ejbCascades != null && ejbCascades.length > 0 ) {
			for ( CascadeType cascade : ejbCascades ) {
				switch ( cascade ) {
					case ALL:
						zormCascadeSet.add( CascadeType.ALL );
						break;
				}
			}
		}

		return zormCascadeSet;
	}
	
	private static void bindManyToOne(
			Ejb3JoinColumn[] columns,
			XClass targetEntity,
			PropertyHolder propertyHolder, 
			PropertyData inferredData,
			boolean unique,
			boolean isIdentifierMapper,
			boolean inSecondPass,
			PropertyBinder propertyBinder, 
			Mappings mappings) {
		com.zorm.mapping.ManyToOne value = new com.zorm.mapping.ManyToOne( mappings, columns[0].getTable());
		if(unique){
			value.markAsLogicalOneToOne();
		}
		value.setReferencedEntityName( ToOneBinder.getReferenceEntityName( inferredData, targetEntity, mappings ) );
		final XProperty property = inferredData.getProperty();
		defineFetchingStrategy( value, property );
		value.setIgnoreNotFound( false );
		value.setCascadeDeleteEnabled( false );
		value.setTypeName( inferredData.getClassOrElementName() );
		final String propertyName = inferredData.getPropertyName();
		value.setTypeUsingReflection( propertyHolder.getClassName(), propertyName );
		String path = propertyHolder.getPath() + "." + propertyName;
		FkSecondPass secondPass = new ToOneFkSecondPass(
				value, columns,
				false, 
				propertyHolder.getEntityOwnerClassName(),
				path, mappings
		);
		if ( inSecondPass ) {
			secondPass.doSecondPass( mappings.getClasses() );
		}
		else {
			mappings.addSecondPass(secondPass);
		}
		
		propertyBinder.setName( propertyName );
		propertyBinder.setValue( value );
		if ( isIdentifierMapper ) {
			propertyBinder.setInsertable( false );
			propertyBinder.setUpdatable( false );
		}
		else {
			propertyBinder.setInsertable( columns[0].isInsertable() );
			propertyBinder.setUpdatable( columns[0].isUpdatable() );
		}
		
		propertyBinder.setColumns( columns );
		propertyBinder.setAccessType( inferredData.getDefaultAccess() );
		propertyBinder.setCascade( "all" );
		propertyBinder.setProperty( property );
		propertyBinder.setXToMany( true );
		propertyBinder.makePropertyAndBind();
	}

	private static void defineFetchingStrategy(
			ToOne value, XProperty property) {
		ManyToOne manyToOne = property.getAnnotation( ManyToOne.class );
		FetchType fetchType;
		if ( manyToOne != null ) {
			fetchType = manyToOne.fetch();
		}
		else {
			throw new AssertionFailure(
					"Define fetch strategy on a property not annotated with @OneToMany nor @OneToOne"
			);
		}
		value.setLazy( fetchType == FetchType.LAZY );
		value.setUnwrapProxy( false );
		value.setFetchMode( getFetchMode( fetchType ) );
	}
	
	public static FetchMode getFetchMode(FetchType fetch) {
		if ( fetch == FetchType.EAGER ) {
			return FetchMode.JOIN;
		}
		else {
			return FetchMode.SELECT;
		}
	}

	private static void processId(PropertyHolder propertyHolder,
			PropertyData inferredData, SimpleValue idValue,
			HashMap<String, IdGenerator> classGenerators,
			boolean isIdentifierMapper, Mappings mappings) {

		if ( isIdentifierMapper ) {
			throw new AnnotationException(
					"@IdClass class should not have @Id nor @EmbeddedId properties: "
							+ BinderHelper.getPath( propertyHolder, inferredData )
			);
		}
		
		XProperty property = inferredData.getProperty();
		HashMap<String, IdGenerator> localGenerators = ( HashMap<String, IdGenerator> ) classGenerators.clone();
		localGenerators.putAll( buildLocalGenerators( property, mappings ) );
		GeneratedValue generatedValue = property.getAnnotation( GeneratedValue.class );
		String generatorType = generatedValue != null ?
				generatorType( generatedValue.strategy(), mappings ) :
				"assigned";
		String generatorName = generatedValue!=null?
				generatedValue.generator():
				BinderHelper.ANNOTATION_STRING_DEFAULT;
		
		BinderHelper.makeIdGenerator( idValue, generatorType, generatorName, mappings, localGenerators );
	}

	private static String generatorType(GenerationType generatorEnum,
			Mappings mappings) {

        switch(generatorEnum){
        case IDENTITY:
        	return "identity";
		default:
        }
        throw new AssertionFailure( "Unknown GeneratorType: " + generatorEnum );
	}

	private static PersistentClass makePersistentClass(
			InheritanceState inheritanceState, PersistentClass superEntity) {
		PersistentClass persistentClass;
		if(!inheritanceState.hasParents()){
			persistentClass = new RootClass();
		}
		else if(InheritanceType.SINGLE_TABLE.equals(inheritanceState.getType())){
			persistentClass = new SingleTableSubclass( superEntity );
		}else if(InheritanceType.JOINED.equals(inheritanceState.getType())){
			persistentClass = new JoinedSubclass( superEntity );
		}else if(InheritanceType.TABLE_PER_CLASS.equals(inheritanceState.getType())){
			persistentClass = new UnionSubclass(superEntity);
		}
		else{
			throw new AssertionFailure( "Unknown inheritance type: " + inheritanceState.getType() );
		}
		return persistentClass;
	}

	private static PersistentClass getSuperEntity(XClass clazzToProcess,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			Mappings mappings, InheritanceState inheritanceState) {
		
		InheritanceState superEntityState = InheritanceState.getInheritanceStateOfSuperEntity(clazzToProcess,inheritanceStatePerClass);
		PersistentClass superEntity = superEntityState!=null?
				mappings.getClass(superEntityState.getClazz().getName()):null;
				
		if(superEntity==null){
			if(inheritanceState.hasParents()){
				throw new AssertionFailure(
						"Subclass has to be binded after it's mother class: "
								+ superEntityState.getClazz().getName()
				);
			}
		}
		return superEntity;
		
	}

	private static boolean isEntityClassType(XClass clazzToProcess,
			AnnotatedClassType classType) {
		if ( AnnotatedClassType.EMBEDDABLE_SUPERCLASS.equals( classType ) //will be processed by their subentities
				|| AnnotatedClassType.NONE.equals( classType ) //to be ignored
				|| AnnotatedClassType.EMBEDDABLE.equals( classType ) //allow embeddable element declaration
				){
			return  false;
		}
		if ( !classType.equals( AnnotatedClassType.ENTITY ) ) {
			throw new AnnotationException(
					"Annotated class should have a @javax.persistence.Entity, @javax.persistence.Embeddable or @javax.persistence.EmbeddedSuperclass annotation: " + clazzToProcess
							.getName()
			);
		}
		return true;
	}

	public static Map<XClass, InheritanceState> buildInheritanceStates(
			List<XClass> orderedClasses, Mappings mappings) {
		Map<XClass, InheritanceState> inheritanceStatePerClass = new HashMap<XClass, InheritanceState>(
				orderedClasses.size()
		);
		for(XClass clazz : orderedClasses){
			InheritanceState superclassState = InheritanceState.getSuperclassInheritanceState(
					clazz, inheritanceStatePerClass
			);
			//实体类的继承状态，默认是SINGEL_TABLE
			InheritanceState state = new InheritanceState( clazz, inheritanceStatePerClass, mappings );
		
			if ( superclassState != null ) {
				superclassState.setHasSiblings( true );
				InheritanceState superEntityState = InheritanceState.getInheritanceStateOfSuperEntity(
						clazz, inheritanceStatePerClass
				);
				state.setHasParents( superEntityState != null );
				final boolean nonDefault = state.getType() != null && !InheritanceType.SINGLE_TABLE
						.equals( state.getType() );
				if ( superclassState.getType() != null ) {
					final boolean mixingStrategy = state.getType() != null && !state.getType()
							.equals( superclassState.getType() );
					if ( nonDefault && mixingStrategy ) {
					}
					state.setType( superclassState.getType() );
				}
			}
			inheritanceStatePerClass.put( clazz, state );
		}
		return inheritanceStatePerClass;
	}

	static int addElementsOfClass(
			List<PropertyData> elements,
			AccessType defaultAccessType,
			PropertyContainer propertyContainer,
			Mappings mappings) {
		int idPropertyCounter = 0;
		AccessType accessType = defaultAccessType;
        //获取实体类的属性集合
		Collection<XProperty> properties = propertyContainer.getProperties( accessType );
		for ( XProperty p : properties ) {
			final int currentIdPropertyCounter = addProperty(propertyContainer, p, elements, 
					accessType.getType(), mappings
			);
			idPropertyCounter += currentIdPropertyCounter;
		}
		return idPropertyCounter;
	}

	private static int addProperty(
			PropertyContainer propertyContainer,
			XProperty property,
			List<PropertyData> annElts,
			String propertyAccessor,
			Mappings mappings) {
		final XClass declaringClass = propertyContainer.getDeclaringClass();
		final XClass entity = propertyContainer.getEntityAtStake();
		int idPropertyCounter = 0;
		PropertyData propertyAnnotatedElement = new PropertyInferredData(
				declaringClass, property, propertyAccessor,
				mappings.getReflectionManager()
		);

		final XAnnotatedElement element = propertyAnnotatedElement.getProperty();
		if ( element.isAnnotationPresent( Id.class )) {
			annElts.add( 0, propertyAnnotatedElement );
			if ( mappings.isSpecjProprietarySyntaxEnabled() ) {
				if ( element.isAnnotationPresent( Id.class ) && element.isAnnotationPresent( Column.class ) ) {
					String columnName = element.getAnnotation( Column.class ).name();
					for ( XProperty prop : declaringClass.getDeclaredProperties( AccessType.FIELD.getType() ) ) {
						if ( !prop.isAnnotationPresent( MapsId.class ) ) {
							/**
							 * The detection of a configured individual JoinColumn differs between Annotation
							 * and XML configuration processing.
							 */
							boolean isRequiredAnnotationPresent = false;
							JoinColumns groupAnnotation = prop.getAnnotation( JoinColumns.class );
							if ( (prop.isAnnotationPresent( JoinColumn.class )
									&& prop.getAnnotation( JoinColumn.class ).name().equals( columnName )) ) {
								isRequiredAnnotationPresent = true;
							}
							else if ( prop.isAnnotationPresent( JoinColumns.class ) ) {
								for ( JoinColumn columnAnnotation : groupAnnotation.value() ) {
									if ( columnName.equals( columnAnnotation.name() ) ) {
										isRequiredAnnotationPresent = true;
										break;
									}
								}
							}
							if ( isRequiredAnnotationPresent ) {
								//create a PropertyData fpr the specJ property holding the mapping
								PropertyData specJPropertyData = new PropertyInferredData(
										declaringClass,
										prop,
										propertyAccessor,
										mappings.getReflectionManager()
								);
								mappings.addPropertyAnnotatedWithMapsIdSpecj(
										entity,
										specJPropertyData,
										element.toString()
								);
							}
						}
					}
				}
			}

			if ( element.isAnnotationPresent( ManyToOne.class ) || element.isAnnotationPresent( OneToOne.class ) ) {
				mappings.addToOneAndIdProperty( entity, propertyAnnotatedElement );
			}
			idPropertyCounter++;
		}
		else {
			annElts.add( propertyAnnotatedElement );
		}
		if ( element.isAnnotationPresent( MapsId.class ) ) {
			mappings.addPropertyAnnotatedWithMapsId( entity, propertyAnnotatedElement );
		}

		return idPropertyCounter;
	}

	public static boolean isDefault(XClass clazz, Mappings mappings) {
		return mappings.getReflectionManager().equals( clazz, void.class );
	}
}
