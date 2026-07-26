package com.prevpaper.gateway.config;

import feign.codec.Decoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class FeignConfig {
    @Bean
    public HttpMessageConverter<?> jacksonConverter() {
        return new MappingJackson2HttpMessageConverter();
    }
    /**
     * By asking for ObjectProvider<HttpMessageConverter<?>> in the parameters,
     * Spring will find all available converters (including Jackson) and
     * provide them in the exact format FeignHttpMessageConverters needs.
     */
    @Bean
    public FeignHttpMessageConverters feignHttpMessageConverters(
            ObjectProvider<HttpMessageConverter<?>> converters,
            ObjectProvider<HttpMessageConverterCustomizer> customizers) {

        return new FeignHttpMessageConverters(converters, customizers);
    }

    /**
     * SpringDecoder now accepts the ObjectProvider<FeignHttpMessageConverters>
     * which is automatically wrapped by Spring from the bean defined above.
     */
    @Bean
    public Decoder feignDecoder(ObjectProvider<FeignHttpMessageConverters> feignConvertersProvider) {
        return new ResponseEntityDecoder(new SpringDecoder(feignConvertersProvider));
    }
}

//
//package com.prevpaper.gateway.config;
//
//import com.fasterxml.jackson.databind.DeserializationFeature;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import feign.codec.Decoder;
//import feign.codec.Encoder;
//import org.springframework.beans.factory.ObjectFactory;
//import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
//import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
//import org.springframework.cloud.openfeign.support.SpringDecoder;
//import org.springframework.cloud.openfeign.support.SpringEncoder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.converter.HttpMessageConverter;
//import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
//
//@Configuration
//public class FeignConfig {
//
//    @Bean
//    public Decoder feignDecoder() {
//        // 1. Create a clean, fully-configured Jackson ObjectMapper
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.registerModule(new JavaTimeModule());
//        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//
//        // 2. Wrap it into a single non-null Jackson HTTP Message Converter
//        HttpMessageConverter<?> jacksonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
//
//        // 3. Create a clean HttpMessageConverters object with ONLY valid converters (no null elements)
//        ObjectFactory<HttpMessageConverters> objectFactory = () -> new HttpMessageConverters(jacksonConverter);
//
//        // 4. Return SpringDecoder wrapped in ResponseEntityDecoder
//        return new ResponseEntityDecoder(new SpringDecoder(objectFactory));
//    }
//
//    @Bean
//    public Encoder feignEncoder() {
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.registerModule(new JavaTimeModule());
//
//        HttpMessageConverter<?> jacksonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
//        ObjectFactory<HttpMessageConverters> objectFactory = () -> new HttpMessageConverters(jacksonConverter);
//
//        return new SpringEncoder(objectFactory);
//    }
//}