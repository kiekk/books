package com.example.recipe631.social;

import com.example.recipe631.social.config.SocialConfig;
import com.example.recipe631.social.config.WebConfig;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class SocialWebApplicationInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[]{SocialConfig.class};
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[] {WebConfig.class, };
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] {"/"};
    }

}
