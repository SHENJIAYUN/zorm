package com.zorm.query;

import java.util.List;

import com.zorm.entity.ResultTransformer;

public abstract class BasicTransformerAdapter implements ResultTransformer {
	public Object transformTuple(Object[] tuple, String[] aliases) {
		return tuple;
	}

	public List transformList(List list) {
		return list;
	}
}
