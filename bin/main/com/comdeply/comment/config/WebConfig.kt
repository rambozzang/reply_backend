package com.comdeply.comment.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val domainValidationInterceptor: DomainValidationInterceptor
) : WebMvcConfigurer {
    @Value("\${app.cors.allowed-origins}")
    private lateinit var allowedOrigins: String

    @Value("\${app.cors.allowed-methods}")
    private lateinit var allowedMethods: String

    @Value("\${app.cors.allowed-headers}")
    private lateinit var allowedHeaders: String

    @Value("\${app.cors.allow-credentials}")
    private var allowCredentials: Boolean = true

    @Value("\${app.cors.max-age}")
    private var maxAge: Long = 3600

    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**")
            .allowedOriginPatterns(allowedOrigins)
            .allowedMethods(*allowedMethods.split(",").toTypedArray())
            .allowedHeaders(allowedHeaders)
            .allowCredentials(allowCredentials)
            .maxAge(maxAge)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(domainValidationInterceptor)
    }

    @Bean
    fun restTemplate(): RestTemplate {
        val restTemplate = RestTemplate()
        
        // 추가 설정
        restTemplate.messageConverters.removeIf { it is org.springframework.http.converter.json.MappingJackson2HttpMessageConverter }
        
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        
        val converter = org.springframework.http.converter.json.MappingJackson2HttpMessageConverter()
        converter.objectMapper = objectMapper
        restTemplate.messageConverters.add(converter)
        
        return restTemplate
    }
}
