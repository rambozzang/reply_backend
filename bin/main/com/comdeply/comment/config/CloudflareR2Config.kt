package com.comdeply.comment.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

@Configuration
class CloudflareR2Config {

    @Value("\${cloudflare.r2.access-key-id}")
    private lateinit var accessKeyId: String

    @Value("\${cloudflare.r2.secret-access-key}")
    private lateinit var secretAccessKey: String

    @Value("\${cloudflare.r2.endpoint}")
    private lateinit var endpoint: String

    @Value("\${cloudflare.r2.region}")
    private lateinit var region: String

    @Bean
    fun cloudflareR2Client(): S3Client {
        val credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey)

        return S3Client.builder()
            .region(Region.of(region))
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .httpClient(UrlConnectionHttpClient.builder().build())
            .build()
    }
}
