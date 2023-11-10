package com.demon.dummy.config

import com.demon.dummy.handler.DummyHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.router

@Configuration
class RouterConfig {

    @Bean
    fun routerFunction(dummyHandler: DummyHandler) = router {
        POST("/api/dummy/{option}", dummyHandler::handleFileUpload)
    }
}