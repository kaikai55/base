package com.shenlan.base.config

import com.fasterxml.jackson.databind.DeserializationFeature
import java.text.SimpleDateFormat
import com.fasterxml.jackson.databind.ObjectMapper
import com.shenlan.base.mybatis.interceptor.PageInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.TransactionManagementConfigurer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import javax.sql.DataSource


/**
 * Created by Administrator on 2018/10/25.
 */
open class CustomObjectMapper : ObjectMapper() {
    init {
        //设置日期转换yyyy-MM-dd HH:mm:ss
        dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }
}

@Configuration
class AppConfig {

    @Bean
    fun objectMapper(): ObjectMapper {
        return CustomObjectMapper()
    }

    @Bean
    fun pageInterceptor(): PageInterceptor {
        return PageInterceptor()
    }
}

@Configuration
@EnableTransactionManagement
class MybatisConfiguration : TransactionManagementConfigurer {
    @Autowired
    lateinit var dataSource: DataSource

    override fun annotationDrivenTransactionManager(): PlatformTransactionManager {
        return DataSourceTransactionManager(dataSource)
    }
}