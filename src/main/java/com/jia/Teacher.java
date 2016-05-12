package com.jia;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Entity
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS) 
//@DiscriminatorValue("teacher")
@Table(name="teachers",catalog="orm" )
public class Teacher extends User {
	private static final long serialVersionUID = -6274533035133194905L;
	
	private String title;

	@Column(name="title")
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

}
