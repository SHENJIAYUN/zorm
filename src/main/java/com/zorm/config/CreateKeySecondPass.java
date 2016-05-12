package com.zorm.config;

import java.util.Map;

import com.zorm.exception.MappingException;
import com.zorm.mapping.JoinedSubclass;
import com.zorm.mapping.RootClass;

public class CreateKeySecondPass implements SecondPass {
	private RootClass rootClass;
	private JoinedSubclass joinedSubClass;

	public CreateKeySecondPass(RootClass rootClass) {
		this.rootClass = rootClass;
	}

	public CreateKeySecondPass(JoinedSubclass joinedSubClass) {
		this.joinedSubClass = joinedSubClass;
	}

	public void doSecondPass(Map persistentClasses) throws MappingException {
		if ( rootClass != null ) {
			rootClass.createPrimaryKey();
		}
		else if ( joinedSubClass != null ) {
			joinedSubClass.createPrimaryKey();
		}
		else {
			throw new AssertionError( "rootClass and joinedSubClass are null" );
		}
	}
}

