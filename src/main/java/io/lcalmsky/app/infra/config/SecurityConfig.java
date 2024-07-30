package io.lcalmsky.app.infra.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserDetailsService userDetailsService;
    private final DataSource dataSource; // 토큰 저장소를 설정하기 위해 주입


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .mvcMatchers("/", "/login", "/sign-up", "/check-email-token",
                        "/email-login", "/check-email-login", "/login-link", "/login-by-email",
                        "/search/study").permitAll()
                .mvcMatchers(HttpMethod.GET, "/profile/*").permitAll()
                .anyRequest().authenticated();
        http.formLogin() // formLogin() 을 설정하면 form 기반 인증을 지원합니다.
                // loginPage를 지정하지 않으면 스프링이 기본으로 로그인 페이지를 생성해준다.
                .loginPage("/login") // 로그인 페이지를 지정
                .permitAll(); // 로그인 페이지에는 인증하지 않아도 접근할 수 있게 해준다.
        http.logout() // logout시 설정을 지원
                .logoutSuccessUrl("/"); // 성공시 루트(/)로 이동하도록 설정
        http.rememberMe() // userDetailsService 와 token 을 관리할 repository 를 설정
                .userDetailsService(userDetailsService)
                .tokenRepository(tokenRepository());

    }

    @Bean
    public PersistentTokenRepository tokenRepository() {
        // 토큰 관리를 위한 repository 구현체를 추가하는데 직접 구현할 필요가 없다. dataSource만 설정하면 된다.
        JdbcTokenRepositoryImpl jdbcTokenRepository = new JdbcTokenRepositoryImpl();
        jdbcTokenRepository.setDataSource(dataSource);
        return jdbcTokenRepository;
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                .mvcMatchers("/node_modules/**");
    }
}



//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
//        MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector).servletPath("/path");
//
//        http
//                .authorizeHttpRequests(authorize -> authorize
//                        .requestMatchers(antMatcher("/"), antMatcher("/sign-up")).permitAll()
//                        .requestMatchers("/h2-console/**").permitAll()
//                        .anyRequest().authenticated());
//
//        return http.build();
//    }
//
//    @Bean
//    public WebSecurityCustomizer webSecurityCustomizer() {
//        return (web -> web.ignoring()
//                .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
//        );
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() { // 기본 인코더를 빈으로 등록
//        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
//    }
//}
