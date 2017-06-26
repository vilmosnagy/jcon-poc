package com.github.vilmosnagy.jcon.server

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.annotation.PostConstruct

@ComponentScan(basePackages = arrayOf("com.github.vilmosnagy"))
@SpringBootApplication
class ServerApplication

fun main(args: Array<String>) {
    SpringApplication.run(ServerApplication::class.java, *args)
}

class ClassLoaderFromMemory(
        parent: ClassLoader
) : ClassLoader(parent) {

    override fun findClass(name: String): Class<*> {
        val clazz = Hack.classContentHolder.getClass(name)
        return defineClass(name, clazz, 0, clazz?.size ?: 0)
    }
}

object Hack {
    lateinit var classContentHolder: ClassContentHolder
}

@Scope(value = SCOPE_SINGLETON)
@Repository
class ClassContentHolder {

    private val classContents = mutableMapOf<String, ByteArray>()

    @PostConstruct
    private fun postConstruct() {
        Hack.classContentHolder = this
    }

    fun registerClass(className: String, classContent: ByteArray) {
        classContents += className to classContent
    }

    fun getClass(className: String): ByteArray? {
        return classContents[className]
    }
}

data class Pojo(
        val value: String
)

class ClassNameAndContent {
    lateinit var className: String
    lateinit var classContent: String
}

@RequestMapping("/")
@RestController
class ClassUploaderContent
@Autowired constructor(
        val classContentHolder: ClassContentHolder
) {

    @ResponseBody
    @RequestMapping(
            value = "/post-class",
            method = arrayOf(RequestMethod.POST)
    )
    fun postClass(
            @RequestBody classNameAndContent: ClassNameAndContent
    ): Pojo {
        val classAsBytes = Base64.getDecoder().decode(classNameAndContent.classContent)
        classContentHolder.registerClass(classNameAndContent.className, classAsBytes)
        return Pojo("OK")
    }

    @ResponseBody
    @RequestMapping(
            value = "/post-class",
            method = arrayOf(RequestMethod.GET)
    )
    fun get() = Pojo(ClassLoaderFromMemory(this::class.java.classLoader)
            .loadClass("com.github.vilmosnagy.jcon.client.SamplePojo")
            .getConstructor(String::class.java, Int::class.java)
            .newInstance("FooBar", 5)
            .toString())
}


/**
 * After calling the post-class endpoint with the following request body:
 *
 * ```
 * {
 *   "className": "com.github.vilmosnagy.jcon.client.SamplePojo",
 *   "classContent": "yv66vgAAADIAZwEALGNvbS9naXRodWIvdmlsbW9zbmFneS9qY29uL2NsaWVudC9TYW1wbGVQb2pvBwABAQAQamF2YS9sYW5nL09iamVjdAcAAwEAFGphdmEvaW8vU2VyaWFsaXphYmxlBwAFAQAGY29uY2F0AQAUKClMamF2YS9sYW5nL1N0cmluZzsBACNMb3JnL2pldGJyYWlucy9hbm5vdGF0aW9ucy9Ob3ROdWxsOwEAF2phdmEvbGFuZy9TdHJpbmdCdWlsZGVyBwAKAQAGPGluaXQ+AQADKClWDAAMAA0KAAsADgEABnN0cmluZwEAEkxqYXZhL2xhbmcvU3RyaW5nOwwAEAARCQACABIBAAZhcHBlbmQBAC0oTGphdmEvbGFuZy9TdHJpbmc7KUxqYXZhL2xhbmcvU3RyaW5nQnVpbGRlcjsMABQAFQoACwAWAQADaW50AQABSQwAGAAZCQACABoBABwoSSlMamF2YS9sYW5nL1N0cmluZ0J1aWxkZXI7DAAUABwKAAsAHQEACHRvU3RyaW5nDAAfAAgKAAsAIAEANFN0cmluZ0J1aWxkZXIoKQogICAgICAgIOKApiAgICAgICAgICAgICAgLnRvU3RyaW5nKCkIACIBAB5rb3RsaW4vanZtL2ludGVybmFsL0ludHJpbnNpY3MHACQBAB1jaGVja0V4cHJlc3Npb25WYWx1ZUlzTm90TnVsbAEAJyhMamF2YS9sYW5nL09iamVjdDtMamF2YS9sYW5nL1N0cmluZzspVgwAJgAnCgAlACgBAAR0aGlzAQAuTGNvbS9naXRodWIvdmlsbW9zbmFneS9qY29uL2NsaWVudC9TYW1wbGVQb2pvOwEACWdldFN0cmluZwEABmdldEludAEAAygpSQEAFihMamF2YS9sYW5nL1N0cmluZztJKVYIABABABdjaGVja1BhcmFtZXRlcklzTm90TnVsbAwAMQAnCgAlADIKAAQADgEACmNvbXBvbmVudDEBAApjb21wb25lbnQyAQAEY29weQEAQyhMamF2YS9sYW5nL1N0cmluZztJKUxjb20vZ2l0aHViL3ZpbG1vc25hZ3kvamNvbi9jbGllbnQvU2FtcGxlUG9qbzsMAAwALwoAAgA5AQAMY29weSRkZWZhdWx0AQCEKExjb20vZ2l0aHViL3ZpbG1vc25hZ3kvamNvbi9jbGllbnQvU2FtcGxlUG9qbztMamF2YS9sYW5nL1N0cmluZztJSUxqYXZhL2xhbmcvT2JqZWN0OylMY29tL2dpdGh1Yi92aWxtb3NuYWd5L2pjb24vY2xpZW50L1NhbXBsZVBvam87DAA3ADgKAAIAPQEAElNhbXBsZVBvam8oc3RyaW5nPQgAPwEABiwgaW50PQgAQQEAASkIAEMBAAhoYXNoQ29kZQwARQAuCgAEAEYBABBqYXZhL2xhbmcvU3RyaW5nBwBIAQAGZXF1YWxzAQAVKExqYXZhL2xhbmcvT2JqZWN0OylaAQAIYXJlRXF1YWwBACcoTGphdmEvbGFuZy9PYmplY3Q7TGphdmEvbGFuZy9PYmplY3Q7KVoMAEwATQoAJQBOAQARTGtvdGxpbi9NZXRhZGF0YTsBAAJtdgMAAAABAwAAAAYBAAJidgMAAAAAAQABawEAAmQxAQDtwIAmCgIYAgoCGAIKwIAKAhAOCsCACgIQCAoCCAoKAhALCsCACgIQwIAKAggDCMKGCBjAgDICMAFCFRIGEAIaAjADEgYQBBoCMAXCogYCEAZKCRALGgIwA0jDhgNKCRAMGgIwBUjDhgNKBhANGgIwA0odEA4aAjDAgDIICAIQAhoCMAMyCAgCEAQaAjAFSMOGAUoTEA8aAjAQMggQERoEGAEwEkjDlgNKCRATGgIwBUjDlgFKCRAUGgIwA0jDlgFSERAEGgIwBcKiBggKwIAaBAgHEAhSERACGgIwA8KiBggKwIAaBAgJEArCqAYVAQACZDIBABZMamF2YS9pby9TZXJpYWxpemFibGU7AQAAAQAFb3RoZXIBAAtjbGllbnRfbWFpbgEADVNhbXBsZVBvam8ua3QBABtSdW50aW1lSW52aXNpYmxlQW5ub3RhdGlvbnMBAARDb2RlAQASTG9jYWxWYXJpYWJsZVRhYmxlAQAPTGluZU51bWJlclRhYmxlAQAkUnVudGltZUludmlzaWJsZVBhcmFtZXRlckFubm90YXRpb25zAQANU3RhY2tNYXBUYWJsZQEAClNvdXJjZUZpbGUBABlSdW50aW1lVmlzaWJsZUFubm90YXRpb25zADEAAgAEAAEABgACABIAEAARAAEAXwAAAAYAAQAJAAAAEgAYABkAAAALABEABwAIAAIAYAAAAG0AAwABAAAAH7sAC1m3AA8qtAATtgAXKrQAG7YAHrYAIVkSI7gAKbAAAAACAGEAAAAMAAEAAAAfACoAKwAAAGIAAAAqAAoAAAAKAAAADQAAAAoAAAAMAAAACgAAAAsAAAAKAAcACwAOAAwAFQANAF8AAAAGAAEACQAAABEALAAIAAIAYAAAAC8AAQABAAAABSq0ABOwAAAAAgBhAAAADAABAAAABQAqACsAAABiAAAABgABAAAABgBfAAAABgABAAkAAAARAC0ALgABAGAAAAAvAAEAAQAAAAUqtAAbrAAAAAIAYQAAAAwAAQAAAAUAKgArAAAAYgAAAAYAAQAAAAcAAQAMAC8AAgBgAAAAUwACAAMAAAAVKxIwuAAzKrcANCortQATKhy1ABuxAAAAAgBhAAAAIAADAAAAFQAqACsAAAAAABUAEAARAAEAAAAVABgAGQACAGIAAAAGAAEABgAFAGMAAAAJAgABAAkAAAAAABEANQAIAAIAYAAAACMAAQABAAAABSq0ABOwAAAAAQBhAAAADAABAAAABQAqACsAAABfAAAABgABAAkAAAARADYALgABAGAAAAAjAAEAAQAAAAUqtAAbrAAAAAEAYQAAAAwAAQAAAAUAKgArAAAAEQA3ADgAAwBgAAAAQgAEAAMAAAAQKxIwuAAzuwACWSsctwA6sAAAAAEAYQAAACAAAwAAABAAKgArAAAAAAAQABAAEQABAAAAEAAYABkAAgBfAAAABgABAAkAAABjAAAACQIAAQAJAAAAABBJADsAPAACAGAAAAAzAAMABQAAAB0dBH6ZAAgqtAATTB0FfpkACCq0ABs9KisctgA+sAAAAAEAZAAAAAQAAgsKAF8AAAAGAAEACQAAAAEAHwAIAAEAYAAAADQAAgABAAAAKLsAC1m3AA8SQLYAFyq0ABO2ABcSQrYAFyq0ABu2AB4SRLYAF7YAIbAAAAAAAAEARQAuAAEAYAAAADMAAgABAAAAGSq0ABNZxgAJtgBHpwAFVwMQH2gqtAAbYKwAAAABAGQAAAAIAAJOBwBJQQEAAQBKAEsAAQBgAAAAVgACAAMAAAA2KiulADArwQACmQArK8AAAk0qtAATLLQAE7gAT5kAGCq0ABsstAAboAAHBKcABAOZAAUErAOsAAAAAQBkAAAADgAE/AAuBwACQAH6AAIBAAIAZQAAAAIAXgBmAAAAdgABAFAABQBRWwADSQBSSQBSSQBTAFRbAANJAFJJAFVJAFIAVkkAUgBXWwABcwBYAFlbABZzACtzAFpzABBzAFtzABhzAFtzAC9zAC1zAC5zACxzAAhzADVzADZzAAdzADdzAEpzAFtzAFxzAFtzAEVzAB9zAF0="
 * }
 * ```
 *
 * The next code sample will run without any problem:
 *
 * ```
 * ClassLoaderFromMemory(this::class.java.classLoader)
 *       .loadClass("com.github.vilmosnagy.jcon.client.SamplePojo")
 *       .getConstructor(String::class.java, Int::class.java)
 *       .newInstance("FooBar", 5)
 *       .toString()
 * ```
 *
 * */