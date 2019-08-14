package com.shenlan.base.config

import com.shenlan.Result
import com.shenlan.base.util.getUser
import com.shenlan.base.util.log
import com.shenlan.base.util.toJsonString
import java.io.File
import java.nio.file.Paths

import javax.servlet.ServletContext

import org.springframework.boot.web.server.WebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory
import org.springframework.boot.web.server.MimeMappings
import org.springframework.http.MediaType
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.charset.StandardCharsets
import java.io.UnsupportedEncodingException
import java.net.URLDecoder.decode
import org.springframework.http.HttpMethod
import org.springframework.beans.factory.BeanInitializationException
import javax.annotation.PostConstruct

import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.authentication.AbstractAuthenticationTargetUrlRequestHandler
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * Configuration of web application with Servlet 3.0 APIs.
 */
@Configuration
class WebConfig(var env: Environment) : ServletContextInitializer, WebServerFactoryCustomizer<WebServerFactory>, WebMvcConfigurer {
    override fun onStartup(servletContext: ServletContext?) {
        log.info("Web application configuration, using profiles: {}", env.activeProfiles as Array<Any>)
    }

    override fun customize(server: WebServerFactory) {
        setMimeMappings(server)
        setLocationForStaticAssets(server)
    }

    private fun setMimeMappings(server: WebServerFactory) {
        if (server is ConfigurableServletWebServerFactory) {
            val mappings = MimeMappings(MimeMappings.DEFAULT)
            mappings.add("html", MediaType.TEXT_HTML_VALUE + ";charset=" + StandardCharsets.UTF_8.name().toLowerCase())
            mappings.add("json", MediaType.TEXT_HTML_VALUE + ";charset=" + StandardCharsets.UTF_8.name().toLowerCase())
            server.setMimeMappings(mappings)
        }
    }

    private fun setLocationForStaticAssets(server: WebServerFactory) {
        if (server is ConfigurableServletWebServerFactory) {
            val root: File
            val prefixPath = resolvePathPrefix()
            root = File(prefixPath + "www/")
            if (root.exists() && root.isDirectory) {
                server.setDocumentRoot(root)
            }
        }
    }

    private fun resolvePathPrefix(): String {
        var fullExecutablePath: String
        try {
            fullExecutablePath = decode(this.javaClass.getResource("").path, StandardCharsets.UTF_8.name())
        } catch (e: UnsupportedEncodingException) {
            /* try without decoding if this ever happens */
            fullExecutablePath = this.javaClass.getResource("").path
        }

        val rootPath = Paths.get(".").toUri().normalize().path
        val extractedPath = fullExecutablePath.replace(rootPath, "")
        val extractionEndIndex = extractedPath.indexOf("")
        return if (extractionEndIndex <= 0) "" else extractedPath.substring(0, extractionEndIndex)
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        log.info("add uploadPath:{}", "file:" + AppPro.uploadPath)
        registry.addResourceHandler("/uploadpath/**").addResourceLocations("file:" + AppPro.uploadPath)
    }
}


class AjaxAuthenticationSuccessHandler : SimpleUrlAuthenticationSuccessHandler() {
    override fun onAuthenticationSuccess(request: HttpServletRequest?, response: HttpServletResponse?, authentication: Authentication?) {
        response?.characterEncoding = "UTF-8"
        response?.writer?.println(Result.getSuccess(getUser()).toJsonString)
    }
}

class AjaxAuthenticationFailureHandler : SimpleUrlAuthenticationFailureHandler() {
    override fun onAuthenticationFailure(request: HttpServletRequest?, response: HttpServletResponse?, exception: AuthenticationException?) {
        response?.characterEncoding = "UTF-8"
        response?.writer?.println(Result.getError("用户名或密码错误!").toJsonString)
    }
}

class AjaxLogoutSuccessHandler : AbstractAuthenticationTargetUrlRequestHandler(), LogoutSuccessHandler {
    override fun onLogoutSuccess(request: HttpServletRequest?, response: HttpServletResponse?, authentication: Authentication?) {
        response?.status = HttpServletResponse.SC_OK
    }
}

class Http401UnauthorizedEntryPoint : AuthenticationEntryPoint {
    override fun commence(request: HttpServletRequest?, response: HttpServletResponse?, authException: AuthenticationException?) {
//        response?.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access Denied");
        response?.characterEncoding = "UTF-8"
        response?.writer?.println(Result.getError("拒绝访问").toJsonString)
    }
}

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
class SecurityConfiguration(private val authenticationManagerBuilder: AuthenticationManagerBuilder,
                            private val userDetailsService: UserDetailsService) : WebSecurityConfigurerAdapter() {

    @PostConstruct
    fun init() {
        try {
            authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(BCryptPasswordEncoder())
        } catch (e: Exception) {
            throw BeanInitializationException("Security configuration failed", e)
        }

    }

    @Throws(Exception::class)
    override fun configure(web: WebSecurity) {
        web.ignoring()
                .antMatchers(HttpMethod.OPTIONS, "/**")
                .antMatchers("/assets/**/*.{js,html}")
                .antMatchers("/i18n/**")
                .antMatchers("/api/file/**")
                .antMatchers("/api/maintain/**")
    }

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http
                .csrf().disable()
//                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//                .and()
                .exceptionHandling()
                .authenticationEntryPoint(Http401UnauthorizedEntryPoint()) // 用来配置没登录调用 要权限的方法 api/account 不配置的话默认会redirect /login
                .and()
                .formLogin()
                .loginProcessingUrl("/api/authentication")
                .successHandler(AjaxAuthenticationSuccessHandler())
                .failureHandler(AjaxAuthenticationFailureHandler())
                .usernameParameter("j_username")
                .passwordParameter("j_password")
                .permitAll()
                .and()
                .logout()
                .logoutUrl("/api/logout")
                .logoutSuccessHandler(AjaxLogoutSuccessHandler())
                .permitAll()
                .and()
                .headers()
                .frameOptions()
                .disable()
                .and()
                .authorizeRequests()
                .antMatchers("/api/csrf").permitAll()
                .antMatchers("/api/dictionarydetail/**").permitAll()
                .antMatchers("/api/**").permitAll()
        //.antMatchers("/api/**").authenticated()
//                .and()
//                .sessionManagement().maximumSessions(1).expiredUrl("/#/login")
    }
}

class AppUser(username: String?, password: String?, authorities: MutableCollection<out GrantedAuthority>? = ArrayList()) : User(username, password, authorities) {
    var name = ""
    var id = "1"
}

@Component("userDetailsService")
class DomainUserDetailsService : UserDetailsService {
    override fun loadUserByUsername(username: String?): UserDetails {
        log.info("username:{}", username)
        return AppUser(username = username, password = BCryptPasswordEncoder().encode("1"))
    }
}

