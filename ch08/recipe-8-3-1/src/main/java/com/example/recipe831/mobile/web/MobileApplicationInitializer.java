package com.example.recipe831.mobile.web;

import com.example.recipe831.mobile.web.config.MobileConfiguration;
import org.springframework.mobile.device.DeviceResolverRequestFilter;
import org.springframework.mobile.device.site.SitePreferenceRequestFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.Filter;

public class MobileApplicationInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return null;
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[] { MobileConfiguration.class };
    }

    @Override
    protected Filter[] getServletFilters() {
        return new Filter[] {
                new DeviceResolverRequestFilter(),
                new SitePreferenceRequestFilter()};
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] {"/"};
    }
}
