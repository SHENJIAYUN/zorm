package com.jia.test;

import java.util.HashSet;
import java.util.Set;

import org.xml.sax.SAXException;

import com.jia.Role;
import com.jia.User;
import com.zorm.session.Session;
import com.zorm.transaction.Transaction;
import com.zorm.util.SessionUtil;


public class ManyToManyTest {

	public static User getUser(){
		User user = new User();
		user.setId((int)(1+Math.random()*100));
		user.setName("JIA");
		return user;
	}
	
	public static Role getRole(){
		Role role = new Role();
		role.setId((int)(1+Math.random()*100));
		role.setName("Role");
		return role;
	}
	
	public static void main(String[] args) throws SAXException {
		Session session = SessionUtil.getSession();
		Transaction tx = session.beginTransaction();
		try {
			User user = getUser();
			System.out.println("User:"+user.getId()+","+user.getName());
			Role role = getRole();
			System.out.println("Role:"+role.getId()+","+role.getName());
			Set<Role> roles = new HashSet<>();
			roles.add(role);
			user.setRoles(roles);
			
			session.save(user);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		}
	}

}
