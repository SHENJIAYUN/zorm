package com.jia;

import org.xml.sax.SAXException;

import com.zorm.config.Configuration;
import com.zorm.service.ServiceRegistry;
import com.zorm.service.ServiceRegistryBuilder;
import com.zorm.session.Session;
import com.zorm.session.SessionFactory;

public class SessionUtil {

	public static Session getSession() throws SAXException{
		Configuration conf = new Configuration();
		conf.configure();
		ServiceRegistry serviceRegistry = new ServiceRegistryBuilder()
				.applySetting(conf.getProperties()).buildServiceRegistry();
		SessionFactory sessionFactory = conf
				.buildSessionFactory(serviceRegistry);
		Session session = sessionFactory.openSession();
		return session;
	}
}
