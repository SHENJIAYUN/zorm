package com.zorm.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MetaAttribute implements Serializable{
  private String name;
  private List values = new ArrayList();
  
  public MetaAttribute(String name) {
     this.name = name;
  }
  
  public String getName(){
	  return name;
  }
  
  public List getValues(){
	  return Collections.unmodifiableList(values);
  }
  
  public void addValue(String value){
	  values.add(value);
  }
  
  public String getValue(){
	  if(values.size()!=1){
		  throw new IllegalStateException("no unique value");
	  }
	  return (String)values.get(0);
  }
  
  public boolean isMultiValued(){
	  return values.size()>1;
  }
  
  public String toString() {
		return "[" + name + "=" + values + "]";
	}
}
