package com.demon.dummy.handler

import com.demon.dummy.domain.DummyDto
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse

@Component
class DummyHandler(
        private val eventPublisher: ApplicationEventPublisher
) {




}
