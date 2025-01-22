package com.neogroup.timelogs.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DatabasePerformanceMonitor {

    private static final Logger logger = LoggerFactory.getLogger(DatabasePerformanceMonitor.class);
    private static final long SLOW_QUERY_THRESHOLD_MS = 500;

    @Around("execution(* com.neogroup.timelogs.repository..*(..))")
    public Object logSlowQueries(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            if (duration > SLOW_QUERY_THRESHOLD_MS) {
                logger.warn("Slow query detected in method {}: {} ms", joinPoint.getSignature(), duration);
            }
        }
    }
}