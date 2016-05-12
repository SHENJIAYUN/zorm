package com.zorm.property;

import com.zorm.exception.PropertyNotFoundException;

public interface PropertyAccessor {

	public Getter getGetter(Class mappedClass, String name);
	public Setter getSetter(Class theClass, String propertyName);
}
