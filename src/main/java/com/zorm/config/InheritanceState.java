package com.zorm.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.Access;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;

import com.zorm.annotations.reflection.XAnnotatedElement;
import com.zorm.annotations.reflection.XClass;
import com.zorm.annotations.reflection.XProperty;
import com.zorm.config.InheritanceState.ElementsToProcess;
import com.zorm.config.annotations.EntityBinder;
import com.zorm.exception.AnnotationException;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.Property;

public class InheritanceState {
  private XClass clazz;
  private boolean hasSiblings = false;
  private boolean hasParents = false;
  private InheritanceType type;
  private boolean isEmbeddableSuperClass= false;
  private Map<XClass, InheritanceState> inheritanceStatePerClass;
  private List<XClass> classesToProcessForMappedSuperclass = new ArrayList<XClass>();
  private Mappings mappings;
  private AccessType accessType;
  private ElementsToProcess elementsToProcess;
  private Boolean hasIdClassOrEmbeddedId;
  private boolean isEmbeddableSuperclass;
  
  public InheritanceState(XClass clazz,
		Map<XClass, InheritanceState> inheritanceStatePerClass,
		Mappings mappings) {
	this.setClazz(clazz);
	this.inheritanceStatePerClass = inheritanceStatePerClass;
	this.mappings = mappings;
	extractInheritanceType();
}

  private void extractInheritanceType() {
    XAnnotatedElement element = getClazz();
    Inheritance inhAnn = element.getAnnotation(Inheritance.class );
    MappedSuperclass mappedSuperClass = element.getAnnotation( MappedSuperclass.class );
    if(mappedSuperClass != null){
    	setEmbeddableSuperclass(true);
    	setType(inhAnn==null?null:inhAnn.strategy());
    }
    else{
    	setType(inhAnn==null?InheritanceType.SINGLE_TABLE:inhAnn.strategy());
    }
  }
  
    public void setType(InheritanceType type) {
	  this.type = type;
    }

	public void setEmbeddableSuperclass(boolean embeddableSuperclass) {
	   isEmbeddableSuperClass = embeddableSuperclass;
     }

	public boolean isEmbeddableSuperclass() {
		return isEmbeddableSuperclass;
	}
	
	public XClass getClazz() {
	  return clazz;
    }

	public void setClazz(XClass clazz) {
		this.clazz = clazz;
	}

   static final class ElementsToProcess{
	  private final List<PropertyData> properties;
	  private final int idPropertyCount;
	
	  public List<PropertyData> getProperties() {
		return properties;
	}
	  
	public int getIdPropertyCount() {
		return idPropertyCount;
	}
	  
	private ElementsToProcess(List<PropertyData> properties, int idPropertyCount) {
		this.properties = properties;
		this.idPropertyCount = idPropertyCount;
	}

	public List<PropertyData> getElements() {
		return properties;
	} 
  }

public static InheritanceState getInheritanceStateOfSuperEntity(
		XClass clazz,
		Map<XClass, InheritanceState> states) {

	XClass superclass = clazz;
	do{
		superclass = superclass.getSuperclass();
		InheritanceState currentState = states.get(superclass);
		if(currentState!=null && !currentState.isEmbeddableSuperClass()){
			return currentState;
		}
	}
	while(superclass!=null && !Object.class.getName().equals(superclass.getName()));
	return null;
}

  private boolean isEmbeddableSuperClass() {
	return isEmbeddableSuperClass;
  }

  public boolean hasParents() {
	return hasParents;
  }

  public InheritanceType getType() {
	return type;
  }

 boolean hasTable() {
	return !hasParents()||!InheritanceType.SINGLE_TABLE.equals(getType());
  }

 boolean hasDenormalizedTable() {
	return hasParents()&& InheritanceType.TABLE_PER_CLASS.equals(getType());
 }

 //获取注解元素
public ElementsToProcess getElementsToProcess() {

	if(elementsToProcess == null){
		//获取clazz的继承情况
		InheritanceState inheritanceState = inheritanceStatePerClass.get(clazz);
	    assert !inheritanceState.isEmbeddableSuperClass();
	    //填充classesToProcessForMappedSuperclass
	    getMappedSuperclassesTillNextEntityOrdered();
	    accessType = determineDefaultAccessType();
	    
	    ArrayList<PropertyData> elements = new ArrayList<PropertyData>();
	    int deep = classesToProcessForMappedSuperclass.size();
	    int idPropertyCount = 0;
	    
	    for(int i = 0;i<deep;i++){
	    	// 构造函数中会获取实体类的所有属性
	    	PropertyContainer propertyContainer = new PropertyContainer(classesToProcessForMappedSuperclass.get(i), clazz);
	    	int currentIdPropertyCount = AnnotationBinder.addElementsOfClass(
					elements, accessType, propertyContainer, mappings
			);
			idPropertyCount += currentIdPropertyCount;
	    }
	    
	    if ( idPropertyCount == 0 && !inheritanceState.hasParents() ) {
			throw new AnnotationException( "No identifier specified for entity: " + clazz.getName() );
		}
		elements.trimToSize();
		elementsToProcess = new ElementsToProcess( elements, idPropertyCount );
	}
	return elementsToProcess;
}

  private AccessType determineDefaultAccessType() {
	  for(XClass xclass=clazz;xclass!=null&&!Object.class.getName().equals(xclass);xclass=xclass.getSuperclass()){
		  if(xclass.isAnnotationPresent(Entity.class)){
			  //获取AccessType为property的属性
			  //通过setter或getter方法获取相关属性名
			  for(XProperty prop : xclass.getDeclaredProperties(AccessType.PROPERTY.getType())){
				  if(prop.isAnnotationPresent(Id.class)){
					  return AccessType.PROPERTY;
				  }
			  }
			  for ( XProperty prop : xclass.getDeclaredProperties( AccessType.FIELD.getType() ) ) {
					if ( prop.isAnnotationPresent( Id.class ) ) {
						return AccessType.FIELD;
					}
				}
		  }
	  }
	  throw new AnnotationException( "No identifier specified for entity: " + clazz );
}

private void getMappedSuperclassesTillNextEntityOrdered() {
	XClass currentClassInHierarchy = clazz;
	InheritanceState superclassState;
	do{
	  classesToProcessForMappedSuperclass.add(0,currentClassInHierarchy);	
	  XClass superClass = currentClassInHierarchy;
	  do{
		  superClass = superClass.getSuperclass();
		  superclassState = inheritanceStatePerClass.get(superClass);
	  }
	  while(superClass!=null &&
				!mappings.getReflectionManager().equals(superClass,Object.class) 
				&& superclassState==null);
	  currentClassInHierarchy = superClass;
	}while(superclassState != null && superclassState.isEmbeddableSuperClass());
  }

public void postProcess(PersistentClass persistentClass,
		EntityBinder entityBinder) {
	//获取persistentClass的相关属性
   getElementsToProcess();	
   addMappedSuperClassInMetadata(persistentClass);
   entityBinder.setPropertyAccessType(accessType);
}

  private void addMappedSuperClassInMetadata(PersistentClass persistentClass) {
	  com.zorm.mapping.MappedSuperclass mappedSuperclass = null;
	  final InheritanceState superEntityState =
				InheritanceState.getInheritanceStateOfSuperEntity( clazz, inheritanceStatePerClass );
		PersistentClass superEntity =
				superEntityState != null ?
						mappings.getClass( superEntityState.getClazz().getName() ) :
						null;
		final int lastMappedSuperclass = classesToProcessForMappedSuperclass.size() - 1;
		for ( int index = 0; index < lastMappedSuperclass; index++ ) {
			com.zorm.mapping.MappedSuperclass parentSuperclass = mappedSuperclass;
			final Class<?> type = mappings.getReflectionManager()
					.toClass( classesToProcessForMappedSuperclass.get( index ) );
			//add MAppedSuperclass if not already there
			mappedSuperclass = mappings.getMappedSuperclass( type );
			if ( mappedSuperclass == null ) {
				mappedSuperclass = new com.zorm.mapping.MappedSuperclass( parentSuperclass, superEntity );
				mappedSuperclass.setMappedClass( type );
				mappings.addMappedSuperclass( type, mappedSuperclass );
			}
		}
		if ( mappedSuperclass != null ) {
			persistentClass.setSuperMappedSuperclass( mappedSuperclass );
		}
  
  }

public boolean hasSiblings() {
	return hasSiblings;
  }

  public static InheritanceState getSuperclassInheritanceState(XClass clazz,
		Map<XClass, InheritanceState> states) {
	XClass superclass = clazz;
	do {
		superclass = superclass.getSuperclass();
		InheritanceState currentState = states.get( superclass );
		if ( currentState != null ) {
			return currentState;
		}
	}
	while ( superclass != null && !Object.class.getName().equals( superclass.getName() ) );
	return null;
  }

  public void setHasParents(boolean hasParents) {
	  this.hasParents = hasParents;
  }

  public void setHasSiblings(boolean hasSiblings) {
    this.hasSiblings = hasSiblings;	
  }
}
