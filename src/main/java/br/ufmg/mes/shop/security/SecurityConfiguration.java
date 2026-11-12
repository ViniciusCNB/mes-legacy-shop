package br.ufmg.mes.shop.security;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final UserDetailsService users;
    private final ApiSecurityHandlers errors;

    public SecurityConfiguration(UserDetailsService users, ApiSecurityHandlers errors) {
        this.users = users;
        this.errors = errors;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(users).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .requestCache().disable()
                .formLogin().disable()
                .logout().disable()
                .csrf().csrfTokenRepository(new CookieCsrfTokenRepository())
                .and()
                .httpBasic().authenticationEntryPoint(errors)
                .and()
                .exceptionHandling().authenticationEntryPoint(errors).accessDeniedHandler(errors)
                .and()
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, "/actuator/health", "/api/products", "/api/products/*").permitAll()
                .antMatchers(HttpMethod.GET, "/api/me", "/api/csrf").authenticated()
                .antMatchers(HttpMethod.POST, "/api/orders").hasRole("CUSTOMER")
                .antMatchers(HttpMethod.GET, "/api/orders").hasRole("CUSTOMER")
                .antMatchers(HttpMethod.GET, "/api/orders/*").hasAnyRole("CUSTOMER", "ADMIN")
                .antMatchers(HttpMethod.POST, "/api/orders/*/cancel").hasAnyRole("CUSTOMER", "ADMIN")
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .antMatchers("/error").permitAll()
                .anyRequest().denyAll();
    }
}
