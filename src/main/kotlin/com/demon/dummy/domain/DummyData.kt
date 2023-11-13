package com.demon.dummy.domain

class DummyData(
        val log: String,
        val host: Host,
        val region: String,
        val grade: String
) {

}

class Host(
        val port: String,
        val ip: String,
        val name: String
) {

}