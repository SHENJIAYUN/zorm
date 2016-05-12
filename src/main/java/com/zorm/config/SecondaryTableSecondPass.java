package com.zorm.config;

import java.util.Map;

import com.zorm.annotations.reflection.XAnnotatedElement;
import com.zorm.config.annotations.EntityBinder;
import com.zorm.exception.MappingException;

public class SecondaryTableSecondPass implements SecondPass {

	private EntityBinder entityBinder;
	private PropertyHolder propertyHolder;
	private XAnnotatedElement annotatedClass;

	public SecondaryTableSecondPass(EntityBinder entityBinder, PropertyHolder propertyHolder, XAnnotatedElement annotatedClass) {
		this.entityBinder = entityBinder;
		this.propertyHolder = propertyHolder;
		this.annotatedClass = annotatedClass;
	}

	public void doSecondPass(Map persistentClasses) throws MappingException {
		entityBinder.finalSecondaryTableBinding( propertyHolder );
		
	}

}
