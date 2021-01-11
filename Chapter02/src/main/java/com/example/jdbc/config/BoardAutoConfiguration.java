package com.example.jdbc.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.jdbc.util.JDBCConnectionMananger;

@Configuration
@EnableConfigurationProperties(JDBCConnectionManagerProperties.class)
public class BoardAutoConfiguration {
	
	@Autowired
	private JDBCConnectionManagerProperties properties;
	
//	@Bean
//	public JDBCConnectionMananger getJDBCCOnnectionManager() {
//		JDBCConnectionMananger manager = new JDBCConnectionMananger();
//		manager.setDriverClass("oracle.jdbc.driver.OracleDriver");
//		manager.setUrl("jdbc:oracle:thin:@localhost:1521:XE");
//		manager.setUsername("hr");
//		manager.setPassword("hr");
//		return manager;
//	}
	
	//@Bean
	//@ConditionalOnMissingBean
//	public JDBCConnectionMananger getJDBCCOnnectionManager() {
//		JDBCConnectionMananger manager = new JDBCConnectionMananger();
//		manager.setDriverClass("org.h2.Driver");
//		manager.setUrl("jdbc:h2:tcp://localhost/~/test");
//		manager.setUsername("sa");
//		manager.setPassword("");
//		return manager;
//	}
	
	@Bean
	@ConditionalOnMissingBean
	public JDBCConnectionMananger getJDBCCOnnectionManager() {
		JDBCConnectionMananger manager = new JDBCConnectionMananger();
		manager.setDriverClass(properties.getDriverClass());
		manager.setUrl(properties.getUrl());
		manager.setUsername(properties.getUsername());
		manager.setPassword(properties.getPassword());
		return manager;
	}
}
