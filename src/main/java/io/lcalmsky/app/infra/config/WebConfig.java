package io.lcalmsky.app.infra.config;

import io.lcalmsky.app.modules.notification.infra.interceptor.NotificationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.StaticResourceLocation;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final NotificationInterceptor notificationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> staticResourcesPath = Stream.of(StaticResourceLocation.values())
                .flatMap(StaticResourceLocation::getPatterns)
                .collect(Collectors.toList());
        staticResourcesPath.add("/node_modules/**");
        registry.addInterceptor(notificationInterceptor)
                .excludePathPatterns(staticResourcesPath);
    }
}

// Interceptor에서 redirect를 제외시켰으므로 static location만 추가로 제외시켜주면 된다.
// Interceptor를 등록하면서 excludePathPattern을 이용해 static location일 때 Interceptor가 동작하지 않게 해준다.
