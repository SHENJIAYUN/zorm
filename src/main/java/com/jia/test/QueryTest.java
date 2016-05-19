package com.jia.test;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jia.Animal;
import com.jia.User;
import com.zorm.query.Query;
import com.zorm.session.Session;
import com.zorm.transaction.Transaction;
import com.zorm.util.SessionUtil;

public class QueryTest {

	private static Session session = null;
	
	@BeforeClass
	public static void beforeClass(){
		try {
			session=SessionUtil.getSession();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testFind(){
		Transaction tx = session.beginTransaction();
		String sql = "from User where id=66";
		Query query = session.createQuery(sql);
	    List<User> users = query.list();
	    User user = users.get(0);
		tx.commit();
//		assertTrue(user.getPosts().size() == 1);
	}
	
	@AfterClass
	public static void afterClass(){
		session.close();
	}

}
