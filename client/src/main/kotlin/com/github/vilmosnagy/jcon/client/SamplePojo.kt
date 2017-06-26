package com.github.vilmosnagy.jcon.client

import java.io.Serializable

data class SamplePojo(
    val string: String,
    val int: Int
): Serializable {
    fun concat(): String {
        return StringBuilder()
                .append(string)
                .append(int)
                .toString()
    }
}