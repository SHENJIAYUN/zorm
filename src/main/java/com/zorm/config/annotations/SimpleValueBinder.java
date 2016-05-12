package com.zorm.config.annotations;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.annotations.reflection.XClass;
import com.zorm.annotations.reflection.XProperty;
import com.zorm.config.AccessType;
import com.zorm.config.BinderHelper;
import com.zorm.config.Ejb3Column;
import com.zorm.config.Mappings;
import com.zorm.config.SetSimpleValueTypeSecondPass;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.MappingException;
import com.zorm.mapping.SimpleValue;
import com.zorm.mapping.Table;
import com.zorm.type.TypeDef;
import com.zorm.util.ReflectHelper;

public class SimpleValueBinder {

	private static final Log log = LogFactory.getLog(SimpleValueBinder.class);
	private String propertyName;
	private String returnedClassName;
	private Ejb3Column[] columns;
	private String persistentClassName;
	private String explicitType = "";
	private String defaultType = "";
	private Properties typeParameters = new Properties();
	private Mappings mappings;
	private Table table;
	private SimpleValue simpleValue;
	private boolean isVersion;
	private String timeStampVersionType;
	//is a Map key
	private boolean key;
//	private String referencedEntityName;
	private XProperty xproperty;
	private AccessType accessType;
	
	public void setMappings(Mappings mappings) {
		this.mappings = mappings;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setReturnedClassName(String returnedClassName) {
		this.returnedClassName = returnedClassName;
		
		if(defaultType.length()==0){
			defaultType = returnedClassName;
		}
	}

	public void setColumns(Ejb3Column[] columns) {
		this.columns = columns;
	}

	public void setPersistentClassName(String persistentClassName) {
		this.persistentClassName = persistentClassName;
	}

	public void setType(XProperty property, XClass returnedClass,
			String declaringClassName) {
		if(returnedClass==null){
			return;
		}
		XClass returnedClassOrElement = returnedClass;
		boolean isArray = false;
		if(property.isArray()){
			returnedClassOrElement = property.getElementClass();
			isArray = true;
		}
		this.xproperty = property;
		String type = BinderHelper.ANNOTATION_STRING_DEFAULT;

	    if(columns == null){
	    	throw new AssertionFailure( "SimpleValueBinder.setColumns should be set before SimpleValueBinder.setType" );
	    }
	    
	    if(BinderHelper.ANNOTATION_STRING_DEFAULT.equals(type)){
	    	if(returnedClassOrElement.isEnum()){
	    		//将type设置成enum
	    	}
	    }
	    
	    defaultType = BinderHelper.isEmptyAnnotationValue(type)?returnedClassName :type;
	}

//	public void setReferencedEntityName(String referencedEntityName) {
//		this.referencedEntityName = referencedEntityName;
//	}

	public void setAccessType(AccessType accessType) {
		this.accessType = accessType;
	}

	public SimpleValue make() {
		log.info("building SinpleValue for "+propertyName);
		//table为数据库表
		if(table==null){
			table = columns[0].getTable();
		}
		simpleValue = new SimpleValue(mappings, table);
		
		linkWithValue();
		boolean isInSecondPass = mappings.isInSecondPass();
		SetSimpleValueTypeSecondPass secondPass = new SetSimpleValueTypeSecondPass( this );
		if(!isInSecondPass){
			mappings.addSecondPass(secondPass);
		}
		else{
			fillSimpleValue();
		}
		
		return simpleValue;
	}

	public void linkWithValue() {
		for(Ejb3Column column : columns){
			column.linkWithValue(simpleValue);
		}
	}

	public void fillSimpleValue() {
		String type;
		TypeDef typeDef;
		if ( !BinderHelper.isEmptyAnnotationValue( explicitType ) ) {
			type = explicitType;
			typeDef = mappings.getTypeDef( type );
		}
		else{
			TypeDef implicitTypeDef = mappings.getTypeDef( returnedClassName );
			if ( implicitTypeDef != null ) {
				typeDef = implicitTypeDef;
				type = returnedClassName;
			}
			else{
				typeDef = mappings.getTypeDef( defaultType );
				type = defaultType;
			}
		}
		if ( typeDef != null ) {
			type = typeDef.getTypeClass();
			simpleValue.setTypeParameters( typeDef.getParameters() );
		}
		
		if ( typeParameters != null && typeParameters.size() != 0 ) {
			simpleValue.setTypeParameters( typeParameters );
		}
		
		simpleValue.setTypeName( type );
		
		if ( persistentClassName != null ) {
			simpleValue.setTypeUsingReflection( persistentClassName, propertyName );
		}
		
		if ( !simpleValue.isTypeSpecified() && isVersion() ) {
			simpleValue.setTypeName( "integer" );
		}
		
		if ( timeStampVersionType != null ) {
			simpleValue.setTypeName( timeStampVersionType );
		}
		
		if ( simpleValue.getTypeName() != null && simpleValue.getTypeName().length() > 0
				&& simpleValue.getMappings().getTypeResolver().basic( simpleValue.getTypeName() ) == null ) {
			try {
				Class typeClass = ReflectHelper.classForName( simpleValue.getTypeName() );

//				if ( typeClass != null && DynamicParameterizedType.class.isAssignableFrom( typeClass ) ) {
//					Properties parameters = simpleValue.getTypeParameters();
//					if ( parameters == null ) {
//						parameters = new Properties();
//					}
//					parameters.put( DynamicParameterizedType.IS_DYNAMIC, Boolean.toString( true ) );
//					parameters.put( DynamicParameterizedType.RETURNED_CLASS, returnedClassName );
//					parameters.put( DynamicParameterizedType.IS_PRIMARY_KEY, Boolean.toString( key ) );
//
//					parameters.put( DynamicParameterizedType.ENTITY, persistentClassName );
//					parameters.put( DynamicParameterizedType.XPROPERTY, xproperty );
//					parameters.put( DynamicParameterizedType.PROPERTY, xproperty.getName() );
//					parameters.put( DynamicParameterizedType.ACCESS_TYPE, accessType.getType() );
//					simpleValue.setTypeParameters( parameters );
//				}
			}
			catch ( ClassNotFoundException cnfe ) {
				throw new MappingException( "Could not determine type for: " + simpleValue.getTypeName(), cnfe );
			}
		}
	}
	
	public boolean isVersion() {
		return isVersion;
	}
}
