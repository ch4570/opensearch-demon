package com.demon.dummy.properties

import org.springframework.boot.context.properties.ConfigurationProperties



@ConfigurationProperties("opensearch")
data class OpenSearchProperties(
    var username: String,
    var password: String,
    var hostname: String,
    var port: Int,
    var scheme: String
) {

}