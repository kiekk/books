package com.example.studyspringwebflow.config;

import com.example.studyspringwebflow.interceptor.CommonDataHandlerInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.executor.FlowExecutor;
import org.springframework.webflow.mvc.servlet.FlowHandlerAdapter;
import org.springframework.webflow.mvc.servlet.FlowHandlerMapping;

@Configuration
@ImportResource("classpath:/spring/webflow-config.xml")
@RequiredArgsConstructor
public class WebFlowConfiguration {
    private final FlowExecutor flowExecutor;
    private final FlowDefinitionRegistry flowDefinitionRegistry;
    private final CommonDataHandlerInterceptor commonDataHandlerInterceptor;
    private final LocaleChangeInterceptor localeChangeInterceptor;

    @Bean
    public FlowHandlerAdapter flowHandlerAdapter() {
        FlowHandlerAdapter flowHandlerAdapter = new FlowHandlerAdapter();
        flowHandlerAdapter.setFlowExecutor(flowExecutor);
        return flowHandlerAdapter;
    }

    @Bean
    public FlowHandlerMapping flowHandlerMapping() {
        FlowHandlerMapping flowHandlerMapping = new FlowHandlerMapping();
        flowHandlerMapping.setInterceptors(commonDataHandlerInterceptor, localeChangeInterceptor);
        flowHandlerMapping.setFlowRegistry(flowDefinitionRegistry);
        return flowHandlerMapping;
    }
}
