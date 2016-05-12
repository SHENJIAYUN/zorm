package com.jia;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import com.zorm.annotations.ManyToOne;

@Entity
@Table(name = "post", catalog = "orm")
public class Post {
	private Integer postId;
	private String postContent;
	private User user;

	@Id
	@Column(name="post_id")
	public Integer getPostId() {
		return postId;
	}

	public void setPostId(Integer postId) {
		this.postId = postId;
	}

	@Column(name="post_content")
	public String getPostContent() {
		return postContent;
	}

	public void setPostContent(String postContent) {
		this.postContent = postContent;
	}

	@ManyToOne()
	@JoinColumn(name="user_id")    //外键列
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

}
