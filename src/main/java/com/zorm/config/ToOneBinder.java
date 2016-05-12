package com.zorm.config;

import com.zorm.annotations.ManyToOne;
import com.zorm.annotations.reflection.XClass;
import com.zorm.annotations.reflection.XProperty;
import com.zorm.exception.AssertionFailure;

public class ToOneBinder {

	public static String getReferenceEntityName(PropertyData propertyData, XClass targetEntity, Mappings mappings) {
		if ( AnnotationBinder.isDefault( targetEntity, mappings ) ) {
			return propertyData.getClassOrElementName();
		}
		else {
			return targetEntity.getName();
		}
	}
	
	public static XClass getTargetEntity(PropertyData propertyData, Mappings mappings) {
		XProperty property = propertyData.getProperty();
		return mappings.getReflectionManager().toXClass( getTargetEntityClass( property ) );
	}

	private static Class<?> getTargetEntityClass(XProperty property) {
		final ManyToOne mTo = property.getAnnotation( ManyToOne.class );
		if (mTo != null) {
			return void.class;
		}
//		final OneToOne oTo = property.getAnnotation( OneToOne.class );
//		if (oTo != null) {
//			return oTo.targetEntity();
//		}
		throw new AssertionFailure("Unexpected discovery of a targetEntity: " + property.getName() );
	}
}
