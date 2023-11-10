package com.demon.dummy.event

import com.demon.dummy.domain.DummyData
import com.demon.dummy.domain.DummyDto
import com.demon.dummy.domain.Host
import com.fasterxml.jackson.databind.ObjectMapper
import org.opensearch.action.index.IndexRequest
import org.opensearch.client.RequestOptions
import org.opensearch.client.RestHighLevelClient
import org.opensearch.common.xcontent.XContentType
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.lang.IllegalArgumentException


@Component
class FileEventHandler(
        private val restHighLevelClient: RestHighLevelClient,
        private val objectMapper: ObjectMapper
) {

    @Async
    @EventListener
    fun sendRequesting(dummyDto: DummyDto) {
        dummyDto.filePart.content().map { buffer ->
            buffer.asInputStream().reader().useLines { lines ->
                lines.forEach { line -> sendData(line, dummyDto.option) }
            }
        }.subscribe()

    }

    private fun sendData(line: String, option: String) {
        val host = Host(
                port = "9080",
                ip = "127.0.0.1",
                name = "vm-0871"
        )

        val dummy = DummyData(
                log = line,
                host = host,
                region = "seoul"
        )

        val jsonData = objectMapper.writeValueAsString(dummy)

        val indexRequest = createIndexRequest(option, jsonData)

        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT)
    }

    fun createIndexRequest(option: String, jsonData: String) : IndexRequest {
        return when(option) {
            "linuxsys" -> {
                IndexRequest("category-log-linuxsys")
                    .source(jsonData, XContentType.JSON)
                    .setPipeline("timestamp-processor")
            }
            "linuxauth" -> {
                IndexRequest("category-log-linuxauth")
                    .source(jsonData, XContentType.JSON)
                    .setPipeline("timestamp-processor")
            }
            else -> throw IllegalArgumentException()
        }
    }

}