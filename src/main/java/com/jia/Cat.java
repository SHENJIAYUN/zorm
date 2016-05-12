package com.jia;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@PrimaryKeyJoinColumn(name="catId")
@Table(name="cats_join",catalog="orm" )
public class Cat extends Animal{
  private String catName;
  
  @Column(name="catName")
  public String getCatName() {
	return catName;
  }
  
  public void setCatName(String catName) {
	this.catName = catName;
  }
}
