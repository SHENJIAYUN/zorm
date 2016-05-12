package com.jia;

import java.io.Serializable;
import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import com.zorm.annotations.OneToMany;

@Entity
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS) 
//@DiscriminatorColumn(name="discriminator")
//@DiscriminatorValue("user")
@Table(name="users",catalog="orm" )
public class User implements Serializable{
	private static final long serialVersionUID = 1L;
	
    private Integer id;
	private String name;
//	private Integer age;
//	private String sex;
//	private Collection<Post> posts;
    
    public User() {}

    @Id
	@Column(name="id")
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name="name",length=10)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

//	@Column(name="age")
//	public Integer getAge() {
//		return age;
//	}
//
//	public void setAge(Integer age) {
//		this.age = age;
//	}
//
//	@Column(name="sex")
//	public String getSex() {
//		return sex;
//	}
//
//	public void setSex(String sex) {
//		this.sex = sex;
//	}
	
	

//	@OneToMany(mappedBy="user")
//	public Collection<Post> getPosts() {
//		return posts;
//	}
//
//	public void setPosts(Collection<Post> posts) {
//		this.posts = posts;
//	}
    
}

