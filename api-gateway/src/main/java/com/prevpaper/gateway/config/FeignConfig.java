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