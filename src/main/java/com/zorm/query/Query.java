package com.zorm.query;

import java.util.List;

public interface Query {
	//获取sql语句
	public String getQueryString();
	public List list();
	public int executeUpdate() ;
	public Query setComment(String comment);
	public Query setString(int position, String val);
	public Query setInteger(int position, int val);
	public Query setDouble(String name, double val);
}
