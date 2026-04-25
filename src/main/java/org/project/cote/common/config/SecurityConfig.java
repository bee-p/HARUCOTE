package org.project.cote.common.config;

import lombok.RequiredArgsConstructor;
import org.project.cote.common.security.jwt.JwtAuthenticationEntryPoint;
import org.project.cote.common.security.jwt.JwtAuthenticationFilter;
import org.project.cote.common.security.oauth2.CustomOAuth2UserService;
import org.project.cote.common.security.oauth2.OAuth2FailureHandler;
import org.project.cote.common.security.oauth2.OAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 인증/인가 정책.
 *
 * <p>세션 정책: {@code IF_REQUIRED} — OAuth2 인증 요청 상태(state, redirect_uri 등)는
 * Spring Security 가 기본 {@code HttpSessionOAuth2AuthorizationRequestRepository} 로 세션에 저장한다.
 * OAuth2 dance 가 끝난 뒤에는 JWT 만 사용하므로 일반 API 요청은 사실상 stateless.</p>
 *
 * <p>인증 실패는 {@link JwtAuthenticationEntryPoint} 가 401 JSON 응답으로,
 * OAuth2 로그인 실패는 {@link OAuth2FailureHandler} 가 fragment redirect 로 처리.</p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/error",
            "/api/v1/problems/**",
            "/oauth2/**",
            "/login/oauth2/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oauth2SuccessHandler;
    private final OAuth2FailureHandler oauth2FailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handler ->
                        handler.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
