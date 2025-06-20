package com.example.recipe591.reactive.court;

import org.springframework.web.server.adapter.AbstractReactiveWebInitializer;

public class WebFluxInitializer extends AbstractReactiveWebInitializer {

    @Override
    protected Class<?>[] getConfigClasses() {
        return new Class<?>[] {WebFluxConfiguration.class};
    }
}
