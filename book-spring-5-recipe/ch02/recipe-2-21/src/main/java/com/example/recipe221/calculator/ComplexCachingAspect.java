package com.example.recipe221.calculator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.EnableLoadTimeWeaving;

@Aspect
@EnableAspectJAutoProxy
@EnableLoadTimeWeaving
public class ComplexCachingAspect {

    private Map<String, Complex> cache = new ConcurrentHashMap<>();

    public void setCache(Map<String, Complex> cache) {
        this.cache.clear();
        this.cache.putAll(cache);
    }

    @Around("call(public Complex.new(int, int)) && args(a,b)")
    public Object cacheAround(ProceedingJoinPoint joinPoint, int a, int b) throws Throwable {

        String key = a + "," + b;
        Complex complex = cache.get(key);

        if (complex == null) {
            System.out.println("Cache MISS for (" + key + ")");
            complex = (Complex) joinPoint.proceed();
            cache.put(key, complex);
        } else {
            System.out.println("Cache HIT for (" + key + ")");
        }

        return complex;
    }

}
