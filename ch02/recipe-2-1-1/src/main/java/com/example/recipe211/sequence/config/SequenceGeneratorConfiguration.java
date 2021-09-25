package com.example.recipe211.sequence.config;

import com.example.recipe211.sequence.SequenceGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SequenceGeneratorConfiguration {

    @Bean   // 메소드명으로 Bean 등록
    //@Bean(name = "my1")   // "my1" 이름으로 Bean 등록, 메소드명은 무시
    public SequenceGenerator sequenceGenerator() {

        SequenceGenerator seqgen = new SequenceGenerator();
        seqgen.setPrefix("30");
        seqgen.setSuffix("A");
        seqgen.setInitial(100000);
        return seqgen;
    }
}