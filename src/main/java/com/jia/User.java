package com.jia;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Table;

import com.zorm.annotations.ManyToMany;
import com.zorm.annotations.OneToMany;

@Entity
@Table(name="users",catalog="orm" )
public class User implements Serializable{
	private static final long serialVersionUID = 1L;
	
    private Integer id;
	private String name;
	private Set<Role> roles;
//	private Collection<Post> posts;
    
    public User() {}

    @Id
	@Column(name="user_id")
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name="user_name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@ManyToMany()
	@JoinTable(
			name="user_role",
			joinColumns=@JoinColumn(name="user_id"),
			inverseJoinColumns=@JoinColumn(name="role_id")
			)
	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}
	
//	@OneToMany(mappedBy="user")
//	public Collection<Post> getPosts() {
//		return posts;
//	}
//
//	public void setPosts(Collection<Post> posts) {
//		this.posts = posts;
//	}

}

