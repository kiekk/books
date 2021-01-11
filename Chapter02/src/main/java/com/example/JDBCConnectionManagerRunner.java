package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import com.example.jdbc.util.JDBCConnectionMananger;

@Service
public class JDBCConnectionManagerRunner implements ApplicationRunner {
	@Autowired
	private JDBCConnectionMananger connectionManager;
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		System.out.println("커넥션 매니져 : " + connectionManager.toString());
	}
}
