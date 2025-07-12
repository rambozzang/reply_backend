package com.comdeply.comment.portone.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(PortOneProperties::class)
class PortOneConfig

@ConfigurationProperties(prefix = "portone")
data class PortOneProperties(
    val customerCode: String = "",
    val apiKey: String = "",
    val apiSecret: String = "",
    val storeId: String = "",
    val channelKey: String = "",
    val webhookSecret: String = "",
    val baseUrl: String = "https://api.iamport.kr",
    val isTest: Boolean = true
)