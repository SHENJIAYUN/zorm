package com.zorm.mapping;

import java.io.Serializable;
import java.util.Iterator;

import com.zorm.FetchMode;
import com.zorm.engine.Mapping;
import com.zorm.exception.MappingException;
import com.zorm.type.Type;

public interface Value extends Serializable{
  public int getColumnSpan();
  public Iterator getColumnIterator();
  public Type getType() throws MappingException;
  public FetchMode getFetchMode();
  public Table getTable();
  public boolean hasFormula();
  public boolean isAlternateUniqueKey();
  public boolean isNullable();
  public boolean[] getColumnUpdateability();
  public boolean[] getColumnInsertability();
  public void createForeignKey() throws MappingException;
  public boolean isSimpleValue();
  public boolean isValid(Mapping mapping)throws MappingException;
  public void setTypeUsingReflection(String className,String propertyName)throws MappingException;
 // public Object accept(ValueVisitor visitor);
}
