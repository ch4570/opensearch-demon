package com.demon.dummy.event

import com.demon.dummy.domain.DummyData
import com.demon.dummy.domain.Host
import com.fasterxml.jackson.databind.ObjectMapper
import org.opensearch.action.index.IndexRequest
import org.opensearch.client.RequestOptions
import org.opensearch.client.RestHighLevelClient
import org.opensearch.common.xcontent.XContentType
import org.springframework.context.event.EventListener
import org.springframework.http.codec.multipart.FilePart
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component


@Component
class FileEventHandler(
        private val restHighLevelClient: RestHighLevelClient,
        private val objectMapper: ObjectMapper
) {

    @Async
    @EventListener
    fun sendRequesting(filePart: FilePart) : Unit {
        filePart.content().map { buffer ->
            buffer.asInputStream().reader().useLines { lines ->
                lines.forEach { line -> sendData(line) }
            }
        }.subscribe()

    }

    private fun sendData(line: String) {
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

        val indexRequest = IndexRequest("category-log-linuxsys")
                .source(jsonData, XContentType.JSON)
                .setPipeline("timestamp-processor")

        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT)
    }


}