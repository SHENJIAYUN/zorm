package com.zorm.config.annotations;

import java.util.Map;

import com.zorm.annotations.reflection.XClass;
import com.zorm.annotations.reflection.XProperty;
import com.zorm.config.AccessType;
import com.zorm.config.Ejb3Column;
import com.zorm.config.InheritanceState;
import com.zorm.config.Mappings;
import com.zorm.config.PropertyHolder;
import com.zorm.mapping.KeyValue;
import com.zorm.mapping.Property;
import com.zorm.mapping.RootClass;
import com.zorm.mapping.SimpleValue;
import com.zorm.mapping.Value;

@SuppressWarnings("unused")
public class PropertyBinder {

	private String name;
	private String returnedClassName;
	private boolean lazy;
	private AccessType accessType;
	private Ejb3Column[] columns;
	private PropertyHolder holder;
	private Mappings mappings;
	private Value value;
	private boolean insertable = true;
	private boolean updatable = true;
	private String cascade;
	private SimpleValueBinder simpleValueBinder;
	private XClass declaringClass;
	private boolean declaringClassSet;
	private boolean embedded;
	private EntityBinder entityBinder;
	private boolean isXtoMany;
	private String referencedEntityName;
	
	private XProperty property;
	private XClass returnedClass;
	private boolean isId;
	private Map<XClass, InheritanceState> inheritanceStatePerClass;
	private Property mappingProperty;

	private boolean isXToMany;
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setReturnedClassName(String returnedClassName) {
		this.returnedClassName = returnedClassName;
	}
	
	public void setAccessType(AccessType accessType) {
		this.accessType = accessType;
	}
	
	public void setHolder(PropertyHolder holder) {
		this.holder = holder;
	}
	
	public void setProperty(XProperty property) {
		this.property = property;
	}
	
	public void setReturnedClass(XClass returnedClass) {
		this.returnedClass = returnedClass;
	}
	
	public void setMappings(Mappings mappings) {
		this.mappings = mappings;
	}
	
	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}
	
	public void setUpdatable(boolean updatable) {
		this.updatable = updatable;
	}
	
	public void setDeclaringClass(XClass declaringClass) {
		this.declaringClass = declaringClass;
		this.declaringClassSet = true;
	}
	
	public void setEntityBinder(EntityBinder entityBinder) {
		this.entityBinder = entityBinder;
	}
	
	public void setInheritanceStatePerClass(
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		this.inheritanceStatePerClass = inheritanceStatePerClass;
	}

	public void setId(boolean isId) {
		this.isId = isId;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public void setColumns(Ejb3Column[] columns) {
		insertable = columns[0].isInsertable();
		updatable = columns[0].isUpdateable();
		this.columns = columns;
	}

	public Property makePropertyValueAndBind() {
		return bind(makePropertyAndValue());
	}
	
	private Property bind(Property prop) {
		if (isId) {
			final RootClass rootClass = ( RootClass ) holder.getPersistentClass();
			rootClass.setIdentifier( ( KeyValue ) getValue() );
			if (embedded) {
				rootClass.setEmbeddedIdentifier( true );
			}
			else {
				rootClass.setIdentifierProperty( prop );
				rootClass.setDeclaredIdentifierProperty( prop );
			}
		}
		else {
			holder.addProperty( prop, columns, declaringClass );
		}
		return prop;
	}

	private Property makePropertyAndValue() {
		String containerClassName = holder==null?null:holder.getClassName();
		simpleValueBinder = new SimpleValueBinder();
		simpleValueBinder.setMappings(mappings);
		simpleValueBinder.setPropertyName(name);
		simpleValueBinder.setReturnedClassName( returnedClassName );
		simpleValueBinder.setColumns( columns );
		simpleValueBinder.setPersistentClassName( containerClassName );
		simpleValueBinder.setType( property, returnedClass, containerClassName );
		simpleValueBinder.setMappings( mappings );
		simpleValueBinder.setAccessType( accessType );
		SimpleValue propertyValue = simpleValueBinder.make();
		setValue(propertyValue);
		return makeProperty();
	}

	public Property makeProperty() {
		Property prop = new Property();
		prop.setName( name );
		prop.setNodeName( name );
		prop.setValue( value );
		prop.setLazy( lazy );
		prop.setCascade( cascade );
		prop.setPropertyAccessorName( accessType.getType() );
		prop.setLob(false);
		prop.setInsertable( insertable );
		prop.setUpdateable( updatable );
		
		prop.setOptimisticLocked( true );
		
		this.mappingProperty = prop;
		return prop;
	}

	public void setValue(Value value) {
		this.value = value;
	}

	public Value getValue() {
		return value;
	}

	public void setCascade(String cascadeStrategyg) {
		this.cascade = cascadeStrategyg;
	}

	public void setXToMany(boolean xToMany) {
		this.isXToMany = xToMany;
	}

	public Property makePropertyAndBind() {
		return bind( makeProperty() );
	}
}
