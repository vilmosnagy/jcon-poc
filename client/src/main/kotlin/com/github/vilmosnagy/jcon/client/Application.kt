package com.github.vilmosnagy.jcon.client

import java.util.*


fun main(args: Array<String>) {
    sendClassOverTheNetwork(SamplePojo::class.java)
}

fun sendClassOverTheNetwork(clazz: Class<*>) {
    val bytes: ByteArray = clazz.getResourceAsStream("${clazz.simpleName}.class").use {
        it.readBytes()
    }
    val encoded = Base64.getEncoder().encode(bytes)
    val encodedAsString = String(encoded)
    println(encodedAsString)
}