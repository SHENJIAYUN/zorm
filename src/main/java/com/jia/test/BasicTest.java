package com.jia.test;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jia.Animal;
import com.zorm.session.Session;
import com.zorm.transaction.Transaction;
import com.zorm.util.SessionUtil;

public class BasicTest {

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
	public void testSave() {
		Transaction tx = session.beginTransaction();
		Animal animal = new Animal();
		animal.setId(1);
		animal.setName("animal");
		session.save(animal);
		tx.commit();
	}
	
	@Test
	public void testFind(){
		Transaction tx = session.beginTransaction();
		Animal animal = (Animal)session.find(Animal.class, 1);
		tx.commit();
		assertTrue(animal!=null);
	}
	
	@Test
	public void testUpdate(){
		Transaction tx = session.beginTransaction();
		Animal animal = (Animal)session.find(Animal.class, 1);
		animal.setName("one animal");
		session.update(animal);
		tx.commit();
	}
	
	@Test
	public void testDelete(){
		Transaction tx = session.beginTransaction();
		Animal animal = (Animal)session.find(Animal.class, 1);
		session.delete(animal);
		tx.commit();
	}
	
	
	@AfterClass
	public static void afterClass(){
		session.close();
	}

}
