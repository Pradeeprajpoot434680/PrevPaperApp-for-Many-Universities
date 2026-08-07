package com.prevpaper.gateway.config;

import feign.codec.Decoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;

@Configuration
public class FeignConfig {

    /**
     * Feign decoder backed by the Spring-managed HTTP message converters
     * (Boot-configured Jackson with JavaTimeModule and FAIL_ON_UNKNOWN_PROPERTIES=false).
     * Uses the SpringDecoder(ObjectFactory<HttpMessageConverters>) constructor, which is
     * the API available in Spring Cloud OpenFeign 4.3.x (Spring Cloud 2025.0.0).
     */
    @SuppressWarnings("deprecation")
    @Bean
    public Decoder feignDecoder(ObjectProvider<HttpMessageConverter<?>> converters) {
        HttpMessageConverter<?>[] converterArray =
                converters.orderedStream().toArray(HttpMessageConverter<?>[]::new);
        HttpMessageConverters messageConverters = new HttpMessageConverters(converterArray);
        ObjectFactory<HttpMessageConverters> objectFactory = () -> messageConverters;
        return new ResponseEntityDecoder(new SpringDecoder(objectFactory));
    }
}
