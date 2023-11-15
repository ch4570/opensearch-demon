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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.IllegalArgumentException
import java.util.UUID


@Component
class FileEventHandler(
        private val restHighLevelClient: RestHighLevelClient,
        private val objectMapper: ObjectMapper
) {

    @Async
    @EventListener
    fun sendRequesting(dummyDto: DummyDto) {

        for (j: Int in 1..10) {
            for (i: Int in 1..2) {
                try {
                    val option = dummyDto.option
                    val resourcePath = if (option.equals("was")) "sys-log${i}.log"
                    else "auth-log${i}.log"

                    // 리소스 파일을 읽어오기
                    val inputStream = javaClass.classLoader.getResourceAsStream(resourcePath)

                    if (inputStream != null) {
                        // BufferedReader를 사용하여 파일의 각 줄을 읽어오기
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        var line: String? = reader.readLine()

                        while (line != null) {
                            // 한 줄씩 데이터 전송
                            sendDataToOpenSearch(option = dummyDto.option, line = line)
                            line = reader.readLine()
                        }

                        // 리더를 닫아주기
                        reader.close()
                    } else {
                        println("리소스를 찾을 수 없습니다: $resourcePath")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }

    }

    fun makeIndexAndSendData(line: String, level: String, option: String) {
        val dummyData = createDummyData(line, level, option)
        val jsonData = objectMapper.writeValueAsString(dummyData)
        val indexRequest = createIndexRequest(option, jsonData)
        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT)
    }

    fun sendDataToOpenSearch(option: String, line: String) {
        when (option) {
            "was" -> {
                val grade =
                    if (line.contains("minjae-HP-ProDesk-600-G2-DM") ||
                        line.contains("hjnam-server")
                    ) "info"
                    else if (line.contains("systemd")) "warn"
                    else if (line.contains("UFW BLOCK")) "error"
                    else null

                if (grade != null) {

                    makeIndexAndSendData(line, grade, option)
                }
            }

            "app" -> {
                val level =
                    if (line.contains("error")) "error"
                    else if (line.contains("FAILED") || line.contains("banner exchange")) "warn"
                    else "info"

                makeIndexAndSendData(line = line, level = level, option = option)
            }

            else -> throw IllegalArgumentException()

        }
    }


    fun createDummyData(line: String, level: String, option: String): DummyData {
        val regionList = listOf("Seoul", "Busan", "Anyang", "Cheongna")
        val port = (1000..60000).random()
        val ip1 = (10..254).random()
        val ip2 = (10..254).random()
        val ip3 = (10..254).random()
        val ip4 = (10..254).random()
        val regionIndex = (0..3).random()

        val vmIndex = (2311..18923).random()

        val host = Host(
            port = "$port",
            ip = "${ip1}.${ip2}.${ip3}.${ip4}",
            name = "vm-${vmIndex}"
        )

        return DummyData(
            log = line,
            host = host,
            region = regionList[regionIndex],
            level = level,
            guid = UUID.randomUUID().toString(),
            category = option
        )
    }


    fun createIndexRequest(option: String, jsonData: String): IndexRequest {
        return when (option) {
            "was" -> {
                IndexRequest("oke-log-was")
                    .source(jsonData, XContentType.JSON)
                    .setPipeline("timestamp-processor")
            }

            "app" -> {
                IndexRequest("oke-log-app")
                    .source(jsonData, XContentType.JSON)
                    .setPipeline("timestamp-processor")
            }

            else -> throw IllegalArgumentException()
        }
    }

}