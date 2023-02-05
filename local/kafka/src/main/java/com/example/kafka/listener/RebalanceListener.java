package com.example.kafka.listener;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
public class RebalanceListener implements ConsumerRebalanceListener {
    private final static Logger log = LoggerFactory.getLogger(RebalanceListener.class);

    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        log.warn("Partitions are assigned : " + partitions.toString());
    }

    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        log.warn("Partitions are revoked : " + partitions.toString());
    }
}