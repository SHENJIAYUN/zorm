package com.zorm.session;

import java.io.Serializable;

import com.zorm.query.Query;
import com.zorm.query.SQLQuery;
import com.zorm.transaction.Transaction;

/*
 * Session共享的方法
 */
public interface SharedSessionContract extends Serializable{
  public String getTenantIdentifier();
  public Transaction beginTransaction();
  public Transaction getTransaction();
  public Query getNamedQuery(String queryName);
  //通过HQL查询语句返回Query实例
  public Query createQuery(String queryString);
  //通过SQL查询语句返回SQLQuery实例
  public SQLQuery createSQLQuery(String queryString);
  
}
