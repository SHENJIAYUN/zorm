package com.zorm.config;

import java.util.Map;

import com.zorm.config.annotations.SimpleValueBinder;
import com.zorm.exception.MappingException;

public class SetSimpleValueTypeSecondPass implements SecondPass{
	SimpleValueBinder binder;

	public SetSimpleValueTypeSecondPass(SimpleValueBinder val) {
		binder = val;
	}

	public void doSecondPass(Map persistentClasses) throws MappingException {
		binder.fillSimpleValue();
	}
}
