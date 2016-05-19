package com.zorm.type;

import java.util.LinkedHashSet;

public class OrderedSetType extends SetType {

	private static final long serialVersionUID = -4047631185177307564L;

	public OrderedSetType(TypeFactory.TypeScope typeScope, String role, String propertyRef) {
		super( typeScope, role, propertyRef );
	}
	
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize > 0
				? new LinkedHashSet( anticipatedSize )
				: new LinkedHashSet();
	}

}
