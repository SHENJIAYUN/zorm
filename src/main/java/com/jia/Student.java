package com.jia;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("student")
public class Student extends User {
	private static final long serialVersionUID = 257358004476198372L;

	private int score;

	@Column(name="score")
	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

}
