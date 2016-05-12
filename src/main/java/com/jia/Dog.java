package com.jia;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@PrimaryKeyJoinColumn(name="dogId")
@Table(name="dogs_join",catalog="orm" )
public class Dog extends Animal{
  private String DogName;
  
  @Column(name="dogName")
  public String getDogName() {
	return DogName;
  }
  
  public void setDogName(String dogName) {
	DogName = dogName;
  }
}
