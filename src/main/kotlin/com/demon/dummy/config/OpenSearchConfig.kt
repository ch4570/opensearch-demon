package com.demon.dummy.config

import com.demon.dummy.properties.OpenSearchProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.ssl.SSLContexts
import org.opensearch.client.RestClient
import org.opensearch.client.RestHighLevelClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenSearchConfig(
    private val properties: OpenSearchProperties
) {

    @Bean
    fun objectMapper() = ObjectMapper()

    @Bean
    fun restHighLevelClient() : RestHighLevelClient {
        val credentialProvider = BasicCredentialsProvider()
        credentialProvider.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(properties.username, properties.password))

        val sslBuilder = SSLContexts.custom()
                .loadTrustMaterial(null) { _, _ -> true }
        val sslContext = sslBuilder.build()

        return RestHighLevelClient(
                RestClient.builder(
                        HttpHost(properties.hostname, properties.port, properties.scheme))
                        .setHttpClientConfigCallback { httpClientBuilder ->
                            httpClientBuilder
                                    .setSSLContext(sslContext)
                                    .setDefaultCredentialsProvider(credentialProvider)
                                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        }
                        .setRequestConfigCallback { requestConfigBuilder ->
                            requestConfigBuilder
                                    .setConnectionRequestTimeout(5000)
                                    .setSocketTimeout(120000)
                        }
        )
    }
}