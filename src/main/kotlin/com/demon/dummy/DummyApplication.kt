package com.demon.dummy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@SpringBootApplication
class DummyApplication

fun main(args: Array<String>) {
	runApplication<DummyApplication>(*args)
}
