package com.jia.test;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jia.User;
import com.zorm.query.Query;
import com.zorm.session.Session;
import com.zorm.transaction.Transaction;
import com.zorm.util.SessionUtil;

public class RelationTest {

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
	    User user = (User)session.find(User.class, 66);
		tx.commit();
//		assertTrue(user.getPosts().size() == 1);
	}
	
	@AfterClass
	public static void afterClass(){
		session.close();
	}
}
