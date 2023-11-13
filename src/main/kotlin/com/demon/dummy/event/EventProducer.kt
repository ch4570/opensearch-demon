package com.demon.dummy.event

import com.demon.dummy.domain.DummyDto
import jakarta.annotation.PostConstruct
import org.opensearch.action.ingest.PutPipelineRequest
import org.opensearch.client.RequestOptions
import org.opensearch.client.RestHighLevelClient
import org.opensearch.client.indices.CreateIndexRequest
import org.opensearch.common.bytes.BytesArray
import org.opensearch.common.settings.Settings
import org.opensearch.common.xcontent.XContentType
import org.opensearch.core.xcontent.MediaType
import org.springframework.stereotype.Component

@Component
class EventProducer(
    private val fileEventHandler: FileEventHandler,
    private val restHighLevelClient: RestHighLevelClient,
) {

    @PostConstruct
    fun createEvent() {
        Thread.sleep(30000L)
        createPipeLine()
        createIndex()
        fileEventHandler.sendRequesting(DummyDto("linuxsys"))
        fileEventHandler.sendRequesting(DummyDto("linuxauth"))
    }

    fun createIndex() {

        val jsonData = """
            {
              "number_of_shards": 3,
              "number_of_replicas": 2
            }
        """.trimIndent()

        val request = CreateIndexRequest("category-log-linuxauth")
        request.settings(Settings.builder().loadFromSource(jsonData, XContentType.JSON))

        // Execute the request and get the response
        restHighLevelClient.indices().create(request, RequestOptions.DEFAULT)
    }

    fun createPipeLine() {
        val pipelineName = "timestamp-processor"
        val pipelineDefinition = """
                {
                  "description": "Add timestamp to documents",
                  "processors": [
                    {
                      "date": {
                        "field": "_ingest.timestamp",
                        "target_field": "@timestamp",
                        "formats": ["ISO8601"]
                      }
                    },
                    {
                      "script": {
                        "source": "ZonedDateTime originalTime = ZonedDateTime.parse(ctx['@timestamp']); ZonedDateTime koreaTime = originalTime.withZoneSameInstant(ZoneId.of('Asia/Seoul')); ctx['@timestamp'] = koreaTime.format(DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss'));",
                        "lang": "painless"
                      }
                    }
                  ]
                }
                """.trimIndent()

        val pipelineBytes = BytesArray(pipelineDefinition)
        val mediaType = XContentType.JSON as MediaType
        val pipelineRequest = PutPipelineRequest(
            pipelineName, pipelineBytes, mediaType)

        restHighLevelClient.ingest().putPipeline(
            pipelineRequest, RequestOptions.DEFAULT
        )

    }
}