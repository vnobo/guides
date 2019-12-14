package com.alexbob.sockets.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;
import org.springframework.session.web.http.HttpSessionIdResolver;

@Configuration
@EnableRedisHttpSession
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .httpBasic()
                    .and()
                .authorizeRequests()
                .antMatchers("/socket/**").permitAll()
                .anyRequest().authenticated()
                    .and()
                    .headers()
                    .frameOptions().sameOrigin()
                    .and()
                .csrf()
                    .ignoringAntMatchers( "/socket/**")
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .and();
               /* .sessionManagement()
                    .maximumSessions(2)
                    .sessionRegistry(sessionRegistry());*/
    }


    /**
     *  增加头部 X-Auth-Token 认证支持
     * @return HttpSessionIdResolver
     */
    @Bean
    public HttpSessionIdResolver httpSessionIdResolver() {
        return HeaderHttpSessionIdResolver.xAuthToken();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        manager.createUser(User.withUsername("user").password("password").roles("USER").build());
        return manager;
    }

}
