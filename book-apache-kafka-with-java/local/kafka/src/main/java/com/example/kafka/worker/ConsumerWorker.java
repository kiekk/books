package com.example.kafka.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerWorker implements Runnable {

    private final static Logger logger = LoggerFactory.getLogger(ConsumerWorker.class);
    private final String recordValue;

    public ConsumerWorker(String recordValue) {
        this.recordValue = recordValue;
    }

    @Override
    public void run() {
        logger.info("thread:{}\trecord:{}", Thread.currentThread().getName(), recordValue);
    }
}