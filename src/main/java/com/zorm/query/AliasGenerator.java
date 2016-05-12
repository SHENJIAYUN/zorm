package com.zorm.query;

import com.zorm.util.StringHelper;

public class AliasGenerator {
	private int next = 0;

	private int nextCount() {
		return next++;
	}

	public String createName(String name) {
		return StringHelper.generateAlias( name, nextCount() );
	}
}