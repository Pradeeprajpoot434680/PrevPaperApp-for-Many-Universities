
//
//package com.prevpaper.gateway.config;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.web.filter.CorsFilter;
//import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
//import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
//import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//import org.springframework.web.servlet.function.RequestPredicates;
//import org.springframework.web.servlet.function.RouterFunction;
//import org.springframework.web.servlet.function.ServerResponse;
//
//import java.net.URI;
//import java.util.List;
//
//@Configuration
//@Slf4j
//public class GatewayRoutesConfig {
//
//    private final String universityUrl = System.getenv().getOrDefault("UNIVERSITY_SERVICE_URL", "http://university-service:8082");
//    private final String authUrl = System.getenv().getOrDefault("AUTH_SERVICE_URL", "http://auth-service:8081");
//    private final String notificationUrl = System.getenv().getOrDefault("NOTIFICATION_SERVICE_URL", "http://notification-service:8085");
//    private final String userUrl = System.getenv().getOrDefault("USER_SERVICE_URL", "http://user-service:8086");
//    private final String contentUrl = System.getenv().getOrDefault("CONTENT_SERVICE_URL", "http://content-service:8090");
//
//    @Bean
//    public RouterFunction<ServerResponse> universityServiceRoutes() {
//        return GatewayRouterFunctions.route("university-service")
//                .nest(RequestPredicates.path("/api/v1/get/**")
//                                .or(RequestPredicates.path("/api/v1/global-admin/**"))
//                                .or(RequestPredicates.path("/api/v1/university-rep/**"))
//                                .or(RequestPredicates.path("/api/v1/department-rep/**"))
//                                .or(RequestPredicates.path("/api/v1/program-rep/**"))
//                                .or(RequestPredicates.path("/api/v1/session-rep/**")),
//                        builder -> builder
//                                .route(RequestPredicates.all(), HandlerFunctions.http()))
//                .before(request -> {
//                    log.info("Gateway routing to UNIVERSITY-SERVICE target={}", universityUrl);
//                    MvcUtils.setRequestUrl(request, URI.create(universityUrl + request.path()));
//                    return request;
//                })
//                .build();
//    }
//
//    @Bean
//    public RouterFunction<ServerResponse> authServiceRoute() {
//        return GatewayRouterFunctions.route("auth-service")
//                .route(RequestPredicates.path("/api/v1/auth/**"), HandlerFunctions.http())
//                .before(request -> {
//                    log.info("Gateway routing to AUTH-SERVICE target={}", authUrl);
//                    MvcUtils.setRequestUrl(request, URI.create(authUrl + request.path()));
//                    return request;
//                })
//                .build();
//    }
//
//    @Bean
//    public RouterFunction<ServerResponse> notificationServiceRoute() {
//        return GatewayRouterFunctions.route("notification-service")
//                .route(RequestPredicates.path("/api/v1/notifications/**"), HandlerFunctions.http())
//                .before(request -> {
//                    log.info("Gateway routing to NOTIFICATION-SERVICE target={}", notificationUrl);
//                    MvcUtils.setRequestUrl(request, URI.create(notificationUrl + request.path()));
//                    return request;
//                })
//                .build();
//    }
//
//    @Bean
//    public RouterFunction<ServerResponse> userServiceRoute() {
//        return GatewayRouterFunctions.route("user-service")
//                .route(RequestPredicates.path("/api/v1/users/**"), HandlerFunctions.http())
//                .before(request -> {
//                    log.info("Gateway routing to USER-SERVICE target={}", userUrl);
//                    MvcUtils.setRequestUrl(request, URI.create(userUrl + request.path()));
//                    return request;
//                })
//                .build();
//    }
//
//    @Bean
//    public RouterFunction<ServerResponse> contentServiceRoute() {
//        return GatewayRouterFunctions.route("content-service")
//                .route(RequestPredicates.path("/api/v1/content/**"), HandlerFunctions.http())
//                .before(request -> {
//                    log.info("Gateway routing to CONTENT-SERVICE target={}", contentUrl);
//                    MvcUtils.setRequestUrl(request, URI.create(contentUrl + request.path()));
//                    return request;
//                })
//                .build();
//    }
//
//    @Bean
//    public CorsFilter corsFilter() {
//        CorsConfiguration config = new CorsConfiguration();
//        config.setAllowedOrigins(List.of("http://localhost:5173"));
//        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
//        config.setAllowedHeaders(List.of("Authorization", "Content-Type","X-Requested-With"));
//        config.setAllowCredentials(true);
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//        return new CorsFilter(source);
//    }
//}



package com.prevpaper.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.CorsFilter;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.util.List;

@Configuration
@Slf4j
public class GatewayRoutesConfig {

    // 🟢 FIXED FALLBACKS: Default to localhost ports so it runs natively out-of-the-box locally
    private final String universityUrl = System.getenv().getOrDefault("UNIVERSITY_SERVICE_URL", "http://localhost:8082");
    private final String authUrl = System.getenv().getOrDefault("AUTH_SERVICE_URL", "http://localhost:8081");
    private final String notificationUrl = System.getenv().getOrDefault("NOTIFICATION_SERVICE_URL", "http://localhost:8085");
    private final String userUrl = System.getenv().getOrDefault("USER_SERVICE_URL", "http://localhost:8086");
    private final String contentUrl = System.getenv().getOrDefault("CONTENT_SERVICE_URL", "http://localhost:8090");

    @Bean
    public RouterFunction<ServerResponse> universityServiceRoutes() {
        return GatewayRouterFunctions.route("university-service")
                .nest(RequestPredicates.path("/api/v1/get/**")
                                .or(RequestPredicates.path("/api/v1/global-admin/**"))
                                .or(RequestPredicates.path("/api/v1/university-rep/**"))
                                .or(RequestPredicates.path("/api/v1/department-rep/**"))
                                .or(RequestPredicates.path("/api/v1/program-rep/**"))
                                .or(RequestPredicates.path("/api/v1/session-rep/**")),
                        builder -> builder
                                .route(RequestPredicates.all(), HandlerFunctions.http()))
                .before(request -> {
                    log.info("Gateway routing request path [{}] to UNIVERSITY-SERVICE base target={}", request.path(), universityUrl);
                    // 🟢 FIXED: Set only the base target URI. The framework will cleanly append the path once.
                    MvcUtils.setRequestUrl(request, URI.create(universityUrl));
                    return request;
                })
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> authServiceRoute() {
        return GatewayRouterFunctions.route("auth-service")
                .route(RequestPredicates.path("/api/v1/auth/**"), HandlerFunctions.http())
                .before(request -> {
                    log.info("Gateway routing request path [{}] to AUTH-SERVICE base target={}", request.path(), authUrl);
                    MvcUtils.setRequestUrl(request, URI.create(authUrl));
                    return request;
                })
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> notificationServiceRoute() {
        return GatewayRouterFunctions.route("notification-service")
                .route(RequestPredicates.path("/api/v1/notifications/**"), HandlerFunctions.http())
                .before(request -> {
                    log.info("Gateway routing request path [{}] to NOTIFICATION-SERVICE base target={}", request.path(), notificationUrl);
                    MvcUtils.setRequestUrl(request, URI.create(notificationUrl));
                    return request;
                })
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> userServiceRoute() {
        return GatewayRouterFunctions.route("user-service")
                .route(RequestPredicates.path("/api/v1/users/**"), HandlerFunctions.http())
                .before(request -> {
                    log.info("Gateway routing request path [{}] to USER-SERVICE base target={}", request.path(), userUrl);
                    MvcUtils.setRequestUrl(request, URI.create(userUrl));
                    return request;
                })
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> contentServiceRoute() {
        return GatewayRouterFunctions.route("content-service")
                .route(RequestPredicates.path("/api/v1/content/**"), HandlerFunctions.http())
                .before(request -> {
                    log.info("Gateway routing request path [{}] to CONTENT-SERVICE base target={}", request.path(), contentUrl);
                    MvcUtils.setRequestUrl(request, URI.create(contentUrl));
                    return request;
                })
                .build();
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type","X-Requested-With"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}