package com.shenlan.base.util

import com.shenlan.base.config.AppUser
import com.shenlan.base.config.CustomObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import kotlin.reflect.full.functions

fun time(action: () -> Unit) {
    var a = System.currentTimeMillis()
    action()
    println((System.currentTimeMillis() - a).toString() + "ms")
}

//fun methodlog(action: () -> Unit, name: String = "") {
//    "".log.info("begin method:${name}")
//    action()
//    "".log.info("end method:${name}")
//}
val customObjectMapper = CustomObjectMapper()
val Any.log: Logger
    get() = LoggerFactory.getLogger(this.javaClass)

val Any.toJsonString: String
    get() = customObjectMapper.writeValueAsString(this)

fun Any.println(): Unit {
    println(this)
}

fun Any.pj(): Unit {
    println(this.toJsonString)
}

fun String?.notEmpty(): Boolean {
    return this != null && this.length != 0
}

fun String?.isEmpty(): Boolean {
    return this == null || this.length == 0
}

fun getUser(): AppUser {
    var auth = SecurityContextHolder.getContext().authentication
    if (auth != null && auth.principal is AppUser) {
        return auth.principal as AppUser
    }
    return AppUser("-", "-")
}

fun uuid(): String {
    return UUID.randomUUID().toString().replace("-", "")
}


fun errorlog(e: Throwable) {
    var trace = StringWriter()
    e.printStackTrace(PrintWriter(trace))
    "".log.error(trace.toString())
}

fun <T> getBean(requiredType: Class<T>) = SpringUtil.context.getBean(requiredType)
fun getBean(requiredType: String) = SpringUtil.context.getBean(requiredType)

/**
 * 只需传递对象 即可调用 该对象的Service的Save
 */
fun _save(any: Any): Any? {
    val bean = getBean(any::class.simpleName!!.toLowerCase() + "Service")
    return bean::class.functions.first { it.name == "save" }.call(bean, any)
}

@Service
class SpringUtil : ApplicationContextAware {
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        context = applicationContext
    }

    companion object {
        lateinit var context: ApplicationContext
    }
}
