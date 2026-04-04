package com.prevpaper.gateway.config;

import org.springframework.web.filter.CorsFilter;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouterFunction<ServerResponse> universityServiceRoutes() {
        return GatewayRouterFunctions.route("university-service")
                // Nesting all university paths
                .nest(RequestPredicates.path("/api/v1/get/**")
                                .or(RequestPredicates.path("/api/v1/global-admin/**"))
                                .or(RequestPredicates.path("/api/v1/university-rep/**"))
                                .or(RequestPredicates.path("/api/v1/department-rep/**")),
                        builder -> builder
                                // Crucial: You must call route() or GET() inside the nest
                                .route(RequestPredicates.all(), HandlerFunctions.http())
                                .filter(lb("UNIVERSITY-SERVICE")))
                .before(request -> {
                    System.out.println("Routing request to UNIVERSITY-SERVICE: " + request.path());
                    return request;
                })
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> authServiceRoute() {
        return GatewayRouterFunctions.route("auth-service")
                .route(RequestPredicates.path("/api/v1/auth/**"), HandlerFunctions.http())
                .filter(lb("AUTH-SERVICE"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> notificationServiceRoute() {
        return GatewayRouterFunctions.route("notification-service")
                .route(RequestPredicates.path("/api/v1/notifications/**"), HandlerFunctions.http())
                .filter(lb("NOTIFICATION-SERVICE"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> userServiceRoute() {
        return GatewayRouterFunctions.route("user-service")
                .route(RequestPredicates.path("/api/v1/users/**"), HandlerFunctions.http())
                .filter(lb("USER-SERVICE"))
                .build();
    }

//    @Bean
//    public WebMvcConfigurer corsConfigurer() {
//        return new WebMvcConfigurer() {
//            @Override
//            public void addCorsMappings(CorsRegistry registry) {
//                registry.addMapping("/**")
//                        .allowedOrigins("http://localhost:5173")
//                        .allowedMethods("*")
//                        .allowedHeaders("*")
//                        .allowCredentials(true);
//            }
//        };
//    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173")); // React
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}