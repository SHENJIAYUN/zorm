package com.zorm.config;

import com.zorm.exception.MappingException;

public interface SecondPass {
	void doSecondPass(java.util.Map persistentClasses)
			throws MappingException;
}
