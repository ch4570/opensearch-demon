package com.demon.dummy.event

import com.demon.dummy.domain.DummyData
import com.demon.dummy.domain.DummyDto
import com.demon.dummy.domain.Host
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.opensearch.action.index.IndexRequest
import org.opensearch.action.ingest.PutPipelineRequest
import org.opensearch.client.RequestOptions
import org.opensearch.client.RestHighLevelClient
import org.opensearch.client.indices.CreateIndexRequest
import org.opensearch.common.bytes.BytesArray
import org.opensearch.common.settings.Settings
import org.opensearch.common.xcontent.XContentType
import org.opensearch.core.xcontent.MediaType
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EventProducer(
    private val fileEventHandler: FileEventHandler,
    private val restHighLevelClient: RestHighLevelClient,
    private val objectMapper: ObjectMapper
) {

    @PostConstruct
    fun createEvent() {
        Thread.sleep(30000L)
        createPipeLine()
        createIndex()
        fileEventHandler.sendRequesting(DummyDto("was"))
        fileEventHandler.sendRequesting(DummyDto("app"))
        insertAuditLog()
    }

    fun createIndex() {

        val jsonData = """
            {
              "number_of_shards": 3,
              "number_of_replicas": 2
            }
        """.trimIndent()

        val wasRequest = CreateIndexRequest("oke-log-was")
        val appRequest = CreateIndexRequest("oke-log-app")
        val iGateRequest = CreateIndexRequest("oke-log-igate")
        wasRequest.settings(Settings.builder().loadFromSource(jsonData, XContentType.JSON))
        appRequest.settings(Settings.builder().loadFromSource(jsonData, XContentType.JSON))
        iGateRequest.settings(Settings.builder().loadFromSource(jsonData, XContentType.JSON))

        // Execute the request and get the response
        restHighLevelClient.indices().create(wasRequest, RequestOptions.DEFAULT)
        restHighLevelClient.indices().create(appRequest, RequestOptions.DEFAULT)
        restHighLevelClient.indices().create(iGateRequest, RequestOptions.DEFAULT)
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

    @Async
    fun insertAuditLog() {



        val infoList = listOf(
            "type=USER_LOGIN msg=audit(1636368000.123:456): pid=1234 uid=1000 auid=1001 ses=2 subj=unconfined_u:unconfined_r:unconfined_t:s0-s0:c0.c1023 msg='op=login id=jsmith exe=/usr/bin/sshd hostname=example.com addr=192.168.1.100 terminal=sshd res=success",
            "type=SYSTEM_BOOT msg=audit(1636368100.456:789): pid=5678 uid=0 auid=4294967295 ses=4294967295 subj=system_u:system_r:init_t:s0 msg='succeeded' comm='systemd' exe='/usr/lib/systemd/systemd",
            "type=USER_ADD msg=audit(1636368200.789:1011): pid=9876 uid=0 auid=1000 ses=3 subj=system_u:admin_r:admin_t:s0-s0:c0.c1023 msg='op=add-user id=jdoe exe=/usr/sbin/useradd hostname=? addr=? terminal=? res=success",
            "type=SYSCALL msg=audit(1636368300.1011:1213): arch=c000003e syscall=2 success=yes exit=3 a0=7fff1bf1305a a1=0 a2=1b6 a3=0 items=2 ppid=1234 pid=5678 auid=1000 uid=0 gid=0 euid=0 suid=0 fsuid=0 egid=0 sgid=0 fsgid=0 tty=pts1 ses=2 comm=\"vi\" exe=\"/usr/bin/vi\" subj=unconfined_u:unconfined_r:unconfined_t:s0-s0:c0.c1023 key=(null)",
            "type=AVC msg=audit(1636368400.1213:1415): avc:  granted  { name_connect } for  pid=1234 comm=\"curl\" dest=80 scontext=unconfined_u:unconfined_r:unconfined_t:s0-s0:c0.c1023 tcontext=system_u:object_r:http_port_t:s0 tclass=tcp_socket permissive=0"
        )

        val warnList = listOf(
            "type=AVC msg=audit(1636368500.1415:1617): avc:  denied  { read } for  pid=9876 comm=\"example\" name=\"sensitive_file.txt\" dev=sda1 ino=12345 scontext=unconfined_u:unconfined_r:unconfined_t:s0-s0:c0.c1023 tcontext=unconfined_u:object_r:sensitive_content_t:s0 tclass=file permissive=0",
            "type=CWD msg=audit(1636368600.1617:1819):  cwd=\"/home/jdoe\" type=PATH msg=audit(1636368600.1617:1819): item=0 name=\"important_file.txt\" inode=67890 dev=sda2 mode=0100644 ouid=1000 ogid=1000 rdev=00:00 obj=unconfined_u:object_r:user_home_t:s0 objtype=NORMAL type=PROCTITLE msg=audit(1636368600.1617:1819): proctitle=\"-/bin/rm\"",
            "type=SYSCALL msg=audit(1636368700.1819:2021): arch=c000003e syscall=9 success=no exit=-13 a0=0 a1=1000 a2=1b6 a3=0 items=2 ppid=1234 pid=5678 auid=1000 uid=0 gid=0 euid=0 suid=0 fsuid=0 egid=0 sgid=0 fsgid=0 tty=pts1 ses=2 comm=\"chmod\" exe=\"/usr/bin/chmod\" subj=unconfined_u:unconfined_r:unconfined_t:s0-s0:c0.c1023 key=(null)",
            "type=EXECVE msg=audit(1636368800.2021:2223): argc=3 a0=\"/bin/bash\" a1=\"-i\" a2=\"-l\" msg='audit(1636368800.2021:2223): exe=\"/bin/bash\" <unknown error>'",
            "type=USER_LOCK msg=audit(1636368900.2223:2425): pid=9876 uid=0 auid=1001 ses=3 subj=unconfined_u:unconfined_r:unconfined_t:s0-s0:c0.c1023 msg='op=lock id=jdoe exe=/usr/sbin/usermod hostname=? addr=? terminal=? res=success'"
        )

        val errorList = listOf(
            "type=EXECVE msg=audit(1636369000.2425:2627): argc=3 a0=\"/bin/ls\" a1=\"-l\" a2=\"/nonexistent_directory\" msg='audit(1636369000.2425:2627): exe=\"/bin/ls\" <unknown error>'",
            "type=CWD msg=audit(1636369100.2627:2829):  cwd=\"/home/jsmith\" type=PATH msg=audit(1636369100.2627:2829): item=0 name=\"confidential_data.txt\" inode=54321 dev=sda2 mode=0100644 ouid=1000 ogid=1000 rdev=00:00 obj=unconfined_u:object_r:user_home_t:s0 objtype=NORMAL type=PROCTITLE msg=audit(1636369100.2627:2829): proctitle=\"-/bin/cat\"",
            "type=CWD msg=audit(1636369200.2829:3031):  cwd=\"/tmp\" type=PATH msg=audit(1636369200.2829:3031): item=0 name=\"important_file.txt\" inode=67890 dev=sda2 mode=0100644 ouid=1000 ogid=1000 rdev=00:00 obj=system_u:object_r:tmp_t:s0 objtype=NORMAL type=PROCTITLE msg=audit(1636369200.2829:3031): proctitle=\"-/bin/echo\"",
            "type=AVC msg=audit(1636369300.3031:3233): avc:  denied  { name_connect } for  pid=1234 comm=\"curl\" dest=8080 scontext=unconfined_u:unconfined_r:unconfined_t:s0-s0:c0.c1023 tcontext=system_u:object_r:http_port_t:s0 tclass=tcp_socket permissive=0",
            "type=SYSCALL msg=audit(1636369400.3233:3435): arch=c000003e syscall=267 success=no exit=-38 a0=7fff1bf1305a a1=0 a2=1b6 a3=0 items=2 ppid=1234 pid=5678 auid=1000 uid=0 gid=0 euid=0 suid=0 fsuid=0 egid=0 sgid=0 fsgid=0 tty=pts1 ses=2 comm=\"example\" exe=\"/usr/bin/example\" subj=unconfined_u:unconfined_r:unconfined_t:s0-s0:c0.c1023 key=(null)"
        )

        for (i: Int in 1..100000) {
            val regionList = listOf("Seoul", "Busan", "Anyang", "Cheongna")
            val port = (1000..60000).random()
            val ip1 = (10..254).random()
            val ip2 = (10..254).random()
            val ip3 = (10..254).random()
            val ip4 = (10..254).random()
            val regionIndex = (0..3).random()
            val logIndex = (0..4).random()

            val vmIndex = (2311..18923).random()


            if (i % 2 == 0) {
                val host = Host(
                    port = "$port",
                    ip = "${ip1}.${ip2}.${ip3}.${ip4}",
                    name = "vm-${vmIndex}"
                )

                val dummyData = DummyData(
                    log = errorList[logIndex],
                    host = host,
                    region = regionList[regionIndex],
                    level = "error",
                    guid = UUID.randomUUID().toString(),
                    category = "igate"
                )

                sendDataToOpenSearch(objectMapper.writeValueAsString(dummyData))
            } else if (i % 3 == 0) {
                val host = Host(
                    port = "$port",
                    ip = "${ip1}.${ip2}.${ip3}.${ip4}",
                    name = "vm-${vmIndex}"
                )

                val dummyData = DummyData(
                    log = warnList[logIndex],
                    host = host,
                    region = regionList[regionIndex],
                    level = "warn",
                    guid = UUID.randomUUID().toString(),
                    category = "igate"
                )

                sendDataToOpenSearch(objectMapper.writeValueAsString(dummyData))
            } else {
                val host = Host(
                    port = "$port",
                    ip = "${ip1}.${ip2}.${ip3}.${ip4}",
                    name = "vm-${vmIndex}"
                )

                val dummyData = DummyData(
                    log = infoList[logIndex],
                    host = host,
                    region = regionList[regionIndex],
                    level = "info",
                    guid = UUID.randomUUID().toString(),
                    category = "igate"
                )

                sendDataToOpenSearch(objectMapper.writeValueAsString(dummyData))
            }
        }

    }

    fun sendDataToOpenSearch(jsonData: String) {
        val indexRequest = IndexRequest("oke-log-igate")
            .source(jsonData, XContentType.JSON)
            .setPipeline("timestamp-processor")

        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT)
    }
}