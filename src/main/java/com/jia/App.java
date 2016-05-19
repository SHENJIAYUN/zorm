package com.jia;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

import com.zorm.config.Configuration;
import com.zorm.query.Query;
import com.zorm.service.ServiceRegistry;
import com.zorm.service.ServiceRegistryBuilder;
import com.zorm.session.Session;
import com.zorm.session.SessionFactory;
import com.zorm.transaction.Transaction;
import com.zorm.util.SessionUtil;

/**
 * ZORM Test
 *
 */
public class App {
	private static Log log = LogFactory.getLog(App.class);

	public void log() {
		String str = "str";
		log.debug("Debug info.");
		log.debug("info" + str);
		log.info("Warn info.");
		log.warn("Warn info");
		log.error("Error info");
		log.fatal("Fatal info");
	}

	public static void config() throws DocumentException {
		Document doc = App.read("src/main/java/config.xml");
		Element root = doc.getRootElement();
		System.out.println(root.element("session-factory").getName());
		Element session_factory = root.element("session-factory");
		Iterator<Element> elements = session_factory.elementIterator();
		while (elements.hasNext()) {
			Element e = elements.next();
			if ("mapping".equals(e.getName())) {
				Attribute classAttribute = e.attribute("class");
				System.out.println(classAttribute.getName() + ":"
						+ classAttribute.getValue());
			}
		}
	}

	public static void delete(User user) throws SAXException {
		Transaction tx = null;
		Configuration conf = new Configuration();
		conf.configure();
		ServiceRegistry serviceRegistry = new ServiceRegistryBuilder()
				.applySetting(conf.getProperties()).buildServiceRegistry();
		SessionFactory sessionFactory = conf
				.buildSessionFactory(serviceRegistry);
		Session session = sessionFactory.openSession();
		tx = session.beginTransaction();
		session.delete(user);
		tx.commit();
	}

	private static Document read(String fileName) throws DocumentException {
		SAXReader reader = new SAXReader();
		Document document = reader.read(new File(fileName));
		return document;
	}

	public static void loadXClass(String className) {
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		try {
			Class clazz = classLoader.loadClass("com.jia.zorm.User");
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				System.out.println(field.getName());
			}
		} catch (Exception e) {
			System.out.println("error");
		}
	}

	public static void add(User user) throws SAXException {
		Configuration conf = new Configuration();
		// 读取配置文件
		conf.configure();
		ServiceRegistry serviceRegistry = new ServiceRegistryBuilder()
				.applySetting(conf.getProperties()).buildServiceRegistry();
		SessionFactory sessionFactory = conf
				.buildSessionFactory(serviceRegistry);
		Session session = sessionFactory.openSession();
		session.beginTransaction();
		session.save(user);
		session.getTransaction().commit();
	}

	public static User find(User user) throws SAXException {
		Transaction tx = null;
		Configuration conf = new Configuration();
		conf.configure();
		ServiceRegistry serviceRegistry = new ServiceRegistryBuilder()
				.applySetting(conf.getProperties()).buildServiceRegistry();
		SessionFactory sessionFactory = conf
				.buildSessionFactory(serviceRegistry);
		Session session = sessionFactory.openSession();
		tx = session.beginTransaction();
		User newUser = (User) session.find(User.class, 1);
		tx.commit();
		return newUser;
	}

	public static void update() throws SAXException {
		Transaction tx = null;
		Configuration conf = new Configuration();
		conf.configure();
		ServiceRegistry serviceRegistry = new ServiceRegistryBuilder()
				.applySetting(conf.getProperties()).buildServiceRegistry();
		SessionFactory sessionFactory = conf
				.buildSessionFactory(serviceRegistry);
		Session session = sessionFactory.openSession();
		tx = session.beginTransaction();
		User u = (User) session.find(User.class, 1);
		System.out.println(u.getName());
		u.setName("JIA");
		session.update(u);

		tx.commit();
	}

	public static void rollback() {

		Transaction tx = null;
		try {
			Configuration conf = new Configuration();
			conf.configure();
			ServiceRegistry serviceRegistry = new ServiceRegistryBuilder()
					.applySetting(conf.getProperties()).buildServiceRegistry();
			SessionFactory sessionFactory = conf
					.buildSessionFactory(serviceRegistry);
			Session session = sessionFactory.openSession();
			tx = session.beginTransaction();
			// session.save(u1);
			// session.save(u2);
			tx.commit();
		} catch (Exception e) {
			System.out.println("rollback");
			tx.rollback();
			e.printStackTrace();
		}
	}

	public static User createUser() {
		User user = new User();
		user.setId(2);
		user.setName("JIA");

		List<Post> posts = new ArrayList<Post>();

		for (int i = 2; i < 4; i++) {
			Post post = new Post();
			post.setPostId(i);
			post.setPostContent("PostContent " + i);
			// post.setUser(user);
			posts.add(post);
			// user.setPosts(posts);
		}
		return user;
	}

	public static List findUsers() throws SAXException {
		Transaction tx = null;
		Configuration conf = new Configuration();
		conf.configure();
		ServiceRegistry serviceRegistry = new ServiceRegistryBuilder()
				.applySetting(conf.getProperties()).buildServiceRegistry();
		SessionFactory sessionFactory = conf
				.buildSessionFactory(serviceRegistry);
		Session session = sessionFactory.openSession();
		tx = session.beginTransaction();
		String findStr = "from User";
		Query query = session.createQuery(findStr);
		List<User> users = query.list();
		tx.commit();

		return users;
	}

	public static void updateUsers() throws SAXException {
		Transaction tx = null;
		Configuration conf = new Configuration();
		conf.configure();
		ServiceRegistry serviceRegistry = new ServiceRegistryBuilder()
				.applySetting(conf.getProperties()).buildServiceRegistry();
		SessionFactory sessionFactory = conf
				.buildSessionFactory(serviceRegistry);
		Session session = sessionFactory.openSession();
		tx = session.beginTransaction();
		String hql = "update User set age=25 where id>=1 ";
		Query query = session.createQuery(hql);
		query.executeUpdate();
		tx.commit();
	}

	public static void deletePost(User user, Serializable id) {

		Transaction tx = null;
		try {
			Configuration conf = new Configuration();
			conf.configure();
			ServiceRegistry serviceRegistry = new ServiceRegistryBuilder()
					.applySetting(conf.getProperties()).buildServiceRegistry();
			SessionFactory sessionFactory = conf
					.buildSessionFactory(serviceRegistry);
			Session session = sessionFactory.openSession();
			// 创建数据库连接Connection
			tx = session.beginTransaction();

			// List posts = (List)user.getPosts();
			// for(int i=0;i<posts.size();i++){
			// Post post = (Post)posts.get(i);
			// if(post.getPostId() == id){
			// session.delete(post);
			// }
			// }

			tx.commit();
			session.close();
		} catch (Exception e) {
			tx.rollback();
			e.printStackTrace();
		}
	}

	public static void addPost(User user, Post post) {

		Transaction tx = null;
		try {
			Configuration conf = new Configuration();
			conf.configure();
			ServiceRegistry serviceRegistry = new ServiceRegistryBuilder()
					.applySetting(conf.getProperties()).buildServiceRegistry();
			SessionFactory sessionFactory = conf
					.buildSessionFactory(serviceRegistry);
			Session session = sessionFactory.openSession();
			// 创建数据库连接Connection
			tx = session.beginTransaction();

			// List posts = (List)user.getPosts();
			boolean isExist = false;
			// for(int i=0;i<posts.size();i++){
			// Post tmpPost = (Post)posts.get(i);
			// if(tmpPost.getPostId() == post.getPostId()){
			// isExist = true;
			// }
			// }
			if (!isExist) {
				session.save(post);
			} else {
				throw new Exception("The Post of id: " + post.getPostId()
						+ " has Existed");
			}

			tx.commit();
		} catch (Exception e) {
			tx.rollback();
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws DocumentException,SAXException {
		User user = new User();
		user.setId((int)(Math.random()*100+1));
		user.setName("JIA");
		
		List<Post> posts = new ArrayList<>();
		Post post = new Post();
		post.setPostId((int)(Math.random()*100+1));
		post.setPostContent("post");
		post.setUser(user);
		posts.add(post);
		
//		user.setPosts(posts);
		
		Session session = SessionUtil.getSession();
		Transaction tx = null;
		try{
			tx = session.beginTransaction();
			session.save(user);
			tx.commit();
		}catch(Exception e){
			e.printStackTrace();
			tx.rollback();
		}
	}
}
