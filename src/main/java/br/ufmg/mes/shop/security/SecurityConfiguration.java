package br.ufmg.mes.shop.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ApiSecurityHandlers errors) throws Exception {
        // Keep the CSRF response token equal to the cookie value and issue the cookie eagerly.
        CsrfTokenRequestAttributeHandler csrfTokens = new CsrfTokenRequestAttributeHandler();
        csrfTokens.setCsrfRequestAttributeName(null);

        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        // Run the CSRF token rotation strategy after successful Basic authentication.
                        .requireExplicitAuthenticationStrategy(false))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(new CookieCsrfTokenRepository())
                        .csrfTokenRequestHandler(csrfTokens))
                .httpBasic(basic -> basic.authenticationEntryPoint(errors))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errors).accessDeniedHandler(errors))
                // Keep authorization independent of MVC path matching, as in Spring Security 5.
                .authorizeHttpRequests(access -> access
                        .requestMatchers(antMatcher(HttpMethod.GET, "/actuator/health"),
                                antMatcher(HttpMethod.GET, "/api/products"), antMatcher(HttpMethod.GET, "/api/products/*"))
                        .permitAll()
                        .requestMatchers(antMatcher(HttpMethod.GET, "/api/me"),
                                antMatcher(HttpMethod.GET, "/api/csrf")).authenticated()
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/orders"),
                                antMatcher(HttpMethod.GET, "/api/orders")).hasRole("CUSTOMER")
                        .requestMatchers(antMatcher(HttpMethod.GET, "/api/orders/*"),
                                antMatcher(HttpMethod.POST, "/api/orders/*/cancel")).hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(antMatcher("/api/admin/**")).hasRole("ADMIN")
                        .requestMatchers(antMatcher("/error")).permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }
}
