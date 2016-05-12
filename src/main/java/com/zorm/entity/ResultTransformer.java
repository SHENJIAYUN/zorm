package com.zorm.entity;

import java.io.Serializable;
import java.util.List;

public interface ResultTransformer extends Serializable{

	public Object transformTuple(Object[] resultRow, String[] resultRowAliases);
	public List transformList(List collection);
}
