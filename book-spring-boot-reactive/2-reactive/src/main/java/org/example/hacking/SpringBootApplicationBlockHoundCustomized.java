package org.example.hacking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.thymeleaf.TemplateEngine;
import reactor.blockhound.BlockHound;

@SpringBootApplication
public class SpringBootApplicationBlockHoundCustomized {

    // 블록하운드는 그 자체로는 아무 일도 하지 않지만,
    // 애플리케이션에 적절하게 설정되면 자바 에이전트 API를 이용해서 블로킹 메서드를 검출하고
    // 해당 스레드가 블로킹 메서드 호출을 허용하는지 검사할 수 있다.
    public static void main(String[] args) {
        BlockHound.builder()
                .allowBlockingCallsInside(
                        TemplateEngine.class.getCanonicalName(), "process")
                .install();

        SpringApplication.run(SpringBootApplicationBlockHoundCustomized.class, args);
    }

}
