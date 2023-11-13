package com.demon.dummy.event

import com.demon.dummy.domain.DummyDto
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
class EventProducer(
    private val fileEventHandler: FileEventHandler
) {

    @PostConstruct
    fun createEvent() {
        fileEventHandler.sendRequesting(DummyDto("linuxsys"))
        fileEventHandler.sendRequesting(DummyDto("linuxauth"))
    }
}