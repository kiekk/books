package com.example.jdbc.util;

import java.sql.Connection;
import java.sql.DriverManager;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class JDBCConnectionMananger {
	private String driverClass;
	private String url;
	private String username;
	private String password;
	
	public Connection getConnection() {
		try {
			Class.forName(driverClass);
			return DriverManager.getConnection(url, username, password);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
