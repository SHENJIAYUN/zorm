package com.zorm.id;

import com.zorm.service.Service;

public interface MutableIdentifierGeneratorFactory extends IdentifierGeneratorFactory, Service{
	public void register(String strategy, Class generatorClass);
}
