package com.zorm.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.zorm.engine.ExecuteUpdateResultCheckStyle;
import com.zorm.engine.Mapping;
import com.zorm.entity.EntityMode;
import com.zorm.exception.MappingException;
import com.zorm.sql.Alias;
import com.zorm.util.EmptyIterator;
import com.zorm.util.JoinedIterator;
import com.zorm.util.ReflectHelper;
import com.zorm.util.SingletonIterator;
import com.zorm.util.StringHelper;

@SuppressWarnings("unused")
public abstract class PersistentClass implements Serializable,Filterable,MetaAttributable{

  private static final long serialVersionUID = -1121438697942700316L;
  private static final Alias PK_ALIAS = new Alias(15,"PK");
  public static final String NULL_DISCRIMINATOR_MAPPING = "null";
  public static final String NOT_NULL_DISCRIMINATOR_MAPPING = "not null";
  
  private String entityName;
  private String className;
  private String proxyInterfaceName;
  private String nodeName;
  private String jpaEntityName;
  private String discriminatorValue;
  private boolean lazy;
  private ArrayList properties = new ArrayList();
  private ArrayList declaredProperties = new ArrayList();
  private final ArrayList subclasses = new ArrayList();
  private final ArrayList subclassProperties = new ArrayList();
  private final ArrayList subclassTables = new ArrayList();
  private boolean dynamicInsert;
  private boolean dynamicUpdate;
  private int batchSize=-1;
  private boolean selectBeforeUpdate;
  private java.util.Map metaAttributes;
  private ArrayList joins = new ArrayList();
  private final ArrayList subclassJoins = new ArrayList();
  private final java.util.List filters = new ArrayList();
  protected final java.util.Set synchronizedTables = new HashSet();
  private String loaderName;
  private Boolean isAbstract;
  private MappedSuperclass superMappedSuperclass;
  
  //Custom SQL
  private String customSQLInsert;
  private boolean customInsertCallable;
  private ExecuteUpdateResultCheckStyle insertCheckStyle;
  private String customSQLUpdate;
	private boolean customUpdateCallable;
	private ExecuteUpdateResultCheckStyle updateCheckStyle;
	private String customSQLDelete;
	private boolean customDeleteCallable;
	private ExecuteUpdateResultCheckStyle deleteCheckStyle;	
	private String temporaryIdTableName;
	private String temporaryIdTableDDL;
	private java.util.Map tuplizerImpls;
	protected int optimisticLockMode;

	
	public abstract int getSubclassId();
	public abstract Iterator getTableClosureIterator();
	public abstract Iterator getKeyClosureIterator();
	public abstract KeyValue getKey();
	public abstract KeyValue getIdentifier() ;
	public abstract boolean hasIdentifierProperty() ;
	public abstract Property getIdentifierProperty() ;
	public abstract Iterator getPropertyClosureIterator();
    public abstract Value getDiscriminator() ;
	public abstract boolean isDiscriminatorInsertable();
	public abstract Table getTable();
	public abstract boolean isInherited() ;
	public abstract int nextSubclassId();
	public abstract Table getRootTable();
	public abstract RootClass getRootClass() ;
	public abstract String getWhere() ;
	public abstract boolean isVersioned() ;
	public abstract boolean isPolymorphic();
	public abstract boolean isExplicitPolymorphism() ;
	public abstract PersistentClass getSuperclass();
	public abstract boolean isJoinedSubclass() ;
	public abstract Property getVersion();
	public abstract Class getEntityPersisterClass();
	public abstract boolean hasEmbeddedIdentifier() ;
	public abstract boolean isMutable() ;
	public abstract  int getOptimisticLockMode();
	
	public void setOptimisticLockMode(int optimisticLockMode) {
		this.optimisticLockMode = optimisticLockMode;
	}
	
	public String getEntityName() {
		return entityName;
	}

	public void setAbstract(Boolean isAbstract) {
		this.isAbstract = isAbstract;
	}
	
	public void setClassName(String className) {
		this.className = className==null ? null : className.intern();
	}
	
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
	
	public String getJpaEntityName() {
		return jpaEntityName;
	}
	
	public void setJpaEntityName(String jpaEntityName) {
		this.jpaEntityName = jpaEntityName;
	}
	
	public void setEntityName(String entityName) {
		this.entityName = entityName==null ? null : entityName.intern();
	}


	public void setDiscriminatorValue(String discriminatorValue) {
 		this.discriminatorValue = discriminatorValue;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public void setProxyInterfaceName(String proxyInterfaceName) {
		this.proxyInterfaceName = proxyInterfaceName;
	}
	
	public void setDynamicInsert(boolean dynamicInsert) {
		this.dynamicInsert = dynamicInsert;
	}

	public void setDynamicUpdate(boolean dynamicUpdate) {
		this.dynamicUpdate = dynamicUpdate;
	}

	

	public Boolean isAbstract() {
		return isAbstract;
	}
	

	public Property getReferencedProperty(String propertyPath) throws MappingException {
		try {
			return getRecursiveProperty( propertyPath, getReferenceablePropertyIterator() );
		}
		catch ( MappingException e ) {
			throw new MappingException(
					"property-ref [" + propertyPath + "] not found on entity [" + getEntityName() + "]", e
			);
		}
	}

	public Iterator getReferenceablePropertyIterator() {
		return getPropertyClosureIterator();
	}
	
	private Property getRecursiveProperty(String propertyPath, Iterator iter) throws MappingException {
		Property property = null;
		StringTokenizer st = new StringTokenizer( propertyPath, ".", false );
		try {
			while ( st.hasMoreElements() ) {
				final String element = ( String ) st.nextElement();
				if ( property == null ) {
					Property identifierProperty = getIdentifierProperty();
					if ( identifierProperty != null && identifierProperty.getName().equals( element ) ) {
						property = identifierProperty;
					}

					if ( property == null ) {
						property = getProperty( element, iter );
					}
				}
			}
		}
		catch ( MappingException e ) {
			throw new MappingException( "property [" + propertyPath + "] not found on entity [" + getEntityName() + "]" );
		}

		return property;
	}
	
	
	public Iterator getPropertyIterator() {
		ArrayList iterators = new ArrayList();
		iterators.add( properties.iterator() );
		for ( int i = 0; i < joins.size(); i++ ) {
			//Join join = ( Join ) joins.get( i );
			//iterators.add( join.getPropertyIterator() );
		}
		return new JoinedIterator( iterators );
	}
	
	private Property getProperty(String propertyName, Iterator iterator) throws MappingException {
		while ( iterator.hasNext() ) {
			Property prop = (Property) iterator.next();
			if ( prop.getName().equals( StringHelper.root(propertyName) ) ) {
				return prop;
			}
		}
		throw new MappingException( "property [" + propertyName + "] not found on entity [" + getEntityName() + "]" );
	}

	public void setSelectBeforeUpdate(boolean selectBeforeUpdate) {
		this.selectBeforeUpdate = selectBeforeUpdate;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public void setSuperMappedSuperclass(
			com.zorm.mapping.MappedSuperclass mappedSuperclass) {
        this.superMappedSuperclass = mappedSuperclass;		
	}

	public String getClassName() {
		return className;
	}

	public void addMappedsuperclassProperty(Property prop) {
		properties.add(prop);
		prop.setPersistentClass(this);
	}

	public void addProperty(Property prop) {
		properties.add(prop);
		declaredProperties.add(prop);
		prop.setPersistentClass(this);
	}
	
	public Iterator getSubclassClosureIterator() {
		ArrayList iters = new ArrayList();
		iters.add( new SingletonIterator(this) );
		Iterator iter = getSubclassIterator();
		while ( iter.hasNext() ) {
			PersistentClass clazz = (PersistentClass)  iter.next();
			iters.add( clazz.getSubclassClosureIterator() );
		}
		return new JoinedIterator(iters);
	}

	public Iterator getSubclassIterator() {
        Iterator[] iterators = new Iterator[subclasses.size()+1];
        Iterator iterator = subclasses.iterator();
        int i=0;
        while(iterator.hasNext()){
        	iterators[i++] = ( (Subclass) iterator.next() ).getSubclassIterator();
        }
        iterators[i] = subclasses.iterator();
        return new JoinedIterator(iterators);
	}

	

	public Table getIdentityTable() {
		return getRootTable();
	}


	public Class getMappedClass() throws MappingException{
        if(className==null) return null;
        try{
        	return ReflectHelper.classForName(className);
        }
        catch(ClassNotFoundException cnfe){
        	throw new MappingException("entity class not found: " + className, cnfe);
        }
	}

	public Class getProxyInterface() {
		if (proxyInterfaceName==null) return null;
		try {
			return ReflectHelper.classForName( proxyInterfaceName );
		}
		catch (ClassNotFoundException cnfe) {
			throw new MappingException("proxy class not found: " + proxyInterfaceName, cnfe);
		}
	}
	
	protected Iterator getDiscriminatorColumnIterator() {
		return EmptyIterator.INSTANCE;
	}
	
	public void validate(Mapping mapping) throws MappingException {
		Iterator iter = getPropertyIterator();
		while ( iter.hasNext() ) {
			Property prop = (Property) iter.next();
			if ( !prop.isValid(mapping) ) {
				throw new MappingException(
						"property mapping has wrong number of columns: " +
						StringHelper.qualify( getEntityName(), prop.getName() ) +
						" type: " +
						prop.getType().getName()
					);
			}
		}
		checkPropertyDuplication();
//		checkColumnDuplication();
	}
	
	private void checkPropertyDuplication() throws MappingException {
		HashSet names = new HashSet();
		Iterator iter = getPropertyIterator();
		while ( iter.hasNext() ) {
			Property prop = (Property) iter.next();
			if ( !names.add( prop.getName() ) ) {
				throw new MappingException( "Duplicate property mapping of " + prop.getName() + " found in " + getEntityName());
			}
		}
	}

	public int getBatchSize() {
		return batchSize;
	}

	public String getLoaderName() {
		return loaderName;
	}


	public Iterator getSubclassPropertyClosureIterator() {
		ArrayList iters = new ArrayList();
		iters.add( getPropertyClosureIterator() );
		iters.add( subclassProperties.iterator() );
		for ( int i=0; i<subclassJoins.size(); i++ ) {
			//Join join = (Join) subclassJoins.get(i);
			//iters.add( join.getPropertyIterator() );
		}
		return new JoinedIterator(iters);
	}

	public String getTemporaryIdTableName() {
		return temporaryIdTableName;
	}
	
	public String getTemporaryIdTableDDL() {
		return temporaryIdTableDDL;
	}

	public int getJoinClosureSpan() {
		return joins.size();
	}

	public String getCustomSQLInsert() {
		return customSQLInsert;
	}

	public String getCustomSQLUpdate() {
		return customSQLUpdate;
	}

	public String getCustomSQLDelete() {
		return customSQLDelete;
	}

	public ExecuteUpdateResultCheckStyle getCustomSQLInsertCheckStyle() {
		return insertCheckStyle;
	}

	public boolean isCustomInsertCallable() {
		return customInsertCallable;
	}

	public boolean isCustomUpdateCallable() {
		return customUpdateCallable;
	}
	
	public ExecuteUpdateResultCheckStyle getCustomSQLUpdateCheckStyle() {
		return updateCheckStyle;
	}
	
	@Override
	public Map getMetaAttributes() {
		return metaAttributes;
	}

	@Override
	public void setMetaAttributes(Map metas) {
		this.metaAttributes = metas;
	}
	
	public java.util.List getFilters() {
		return filters;
	}

	public boolean isCustomDeleteCallable() {
		return customDeleteCallable;
	}

	public ExecuteUpdateResultCheckStyle getCustomSQLDeleteCheckStyle() {
		return deleteCheckStyle;
	}

	public Iterator getSubclassJoinClosureIterator() {
		return new JoinedIterator( getJoinClosureIterator(), subclassJoins.iterator() );
	}

	public Set getSynchronizedTables() {
		return synchronizedTables;
	}

	public Iterator getJoinClosureIterator() {
		return joins.iterator();
	}


	public int getJoinNumber(Property prop) {
		return 0;
	}

	public int getSubclassSpan() {
		int n = subclasses.size();
		Iterator iter = subclasses.iterator();
		while ( iter.hasNext() ) {
			n += ( (Subclass) iter.next() ).getSubclassSpan();
		}
		return n;
	}


	public boolean hasPojoRepresentation() {
		return getClassName()!=null;
	}

	public int getPropertyClosureSpan() {
		int span = properties.size();
		for ( int i=0; i<joins.size(); i++ ) {
			//Join join = (Join) joins.get(i);
			//span += join.getPropertySpan();
		}
		return span;
	}

	

	public boolean isLazy() {
		return lazy;
	}

	public boolean hasSelectBeforeUpdate() {
		return selectBeforeUpdate;
	}

	public boolean useDynamicUpdate() {
		return dynamicUpdate;
	}
	
	public boolean useDynamicInsert(){
		return dynamicInsert;
	}

	

	public boolean hasSubclasses() {
		return subclasses.size() > 0;
	}

	

	public String getTuplizerImplClassName(EntityMode mode) {
		if ( tuplizerImpls == null ) return null;
		return ( String ) tuplizerImpls.get( mode );
	}


	public Iterator getDirectSubclasses() {
		return subclasses.iterator();
	}

	public boolean isDiscriminatorValueNotNull() {
		return NOT_NULL_DISCRIMINATOR_MAPPING.equals( getDiscriminatorValue() );
	}
	
	public String getDiscriminatorValue() {
		return discriminatorValue;
	}

	public boolean isForceDiscriminator() {
		return false;
	}

	public boolean isDiscriminatorValueNull() {
		return NULL_DISCRIMINATOR_MAPPING.equals( getDiscriminatorValue() );
	}

	public Iterator getUnjoinedPropertyIterator() {
		return properties.iterator();
	}

	public void addSubclassProperty(Property prop) {
		subclassProperties.add(prop);
	}

	public void addSubClass(Subclass subclass) {
		PersistentClass superclass = getSuperclass();
		while (superclass!=null) {
			if( subclass.getEntityName().equals( superclass.getEntityName() ) ) {
				throw new MappingException(
					"Circular inheritance mapping detected: " +
					subclass.getEntityName() +
					" will have it self as superclass when extending " +
					getEntityName()
				);
			}
			superclass = superclass.getSuperclass();
		}
		subclasses.add(subclass);
	}

	protected void addSubclassTable(Table table) {
		subclassTables.add(table);
	}

	public Iterator getSubclassTableClosureIterator() {
		return new JoinedIterator( getTableClosureIterator(), subclassTables.iterator() );
	}
	
	public boolean isClassOrSuperclassTable(Table closureTable) {
		return getTable()==closureTable;
	}
	
	public void createPrimaryKey() {
		//Primary key constraint
		PrimaryKey pk = new PrimaryKey();
		Table table = getTable();
		pk.setTable(table);
		pk.setName( PK_ALIAS.toAliasString( table.getName() ) );
		table.setPrimaryKey(pk);

		pk.addColumns( getKey().getColumnIterator() );
	}
	
	public Iterator getJoinIterator() {
		return joins.iterator();
	}
	
	public Property getRecursiveProperty(String propertyPath) throws MappingException {
		try {
			return getRecursiveProperty( propertyPath, getPropertyIterator() );
		}
		catch ( MappingException e ) {
			throw new MappingException(
					"property [" + propertyPath + "] not found on entity [" + getEntityName() + "]", e
			);
		}
	}
	public String getNodeName() {
		return nodeName;
	}
	
}