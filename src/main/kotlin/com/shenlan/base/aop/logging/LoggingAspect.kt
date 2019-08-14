package com.shenlan.base.aop.logging

import java.util.Arrays

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut


import com.shenlan.base.util.log
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy


@Configuration
@EnableAspectJAutoProxy
class LoggingAspectConfiguration {

    @Bean
    fun loggingAspect(): LoggingAspect {
        return LoggingAspect()
    }
}

@Aspect
open class LoggingAspect {

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    fun loggingPointcut() {
    }

    /**
     * Advice that logs when a method is entered and exited.
     */
    @Around("loggingPointcut()")
    fun logAround(joinPoint: ProceedingJoinPoint) {
        val className = joinPoint.signature.declaringTypeName
        val methodName = joinPoint.signature.name
        log.info("Enter: {}.{}() with argument[s] = {}", className, methodName, Arrays.toString(joinPoint.args))
        val a = System.currentTimeMillis()
        val result = joinPoint.proceed()
        log.info("Exit: {}.{}() with result = {} Time = {}ms", className, methodName, result, System.currentTimeMillis() - a)
    }

}
