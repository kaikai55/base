package com.shenlan.base

import com.shenlan.base.util.log
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ConfigurableApplicationContext
import java.net.InetAddress
import java.util.*


object DefaultProfileUtil {
    fun addDefaultProfile(app: SpringApplication) {
        val defProperties = HashMap<String, Any>()
        defProperties["spring.profiles.default"] = "dev"
        app.setDefaultProperties(defProperties)
    }
}

@SpringBootApplication
class Application {
    companion object {
        var context: ConfigurableApplicationContext? = null
    }
}

fun main(args: Array<String>) {
    val app = SpringApplication(Application::class.java)
    DefaultProfileUtil.addDefaultProfile(app)

    val applicationContext = app.run("")
    Application.context = applicationContext
    val env = applicationContext.environment
    val protocol = "http"
    "".log.info("\n  ----------------------------------------------------------\n\t" +
            "Application '{}' is running! Access URLs:\n\t" +
            "Local: \t\t{}://localhost:{}\n\t" +
            "External: \t{}://{}:{}\n\t" +
            "Profile(s): \t{}\n----------------------------------------------------------",
            env.getProperty("spring.application.name"),
            protocol,
            env.getProperty("server.port"),
            protocol,
            InetAddress.getLocalHost().hostAddress,
            env.getProperty("server.port"),
            env.activeProfiles)
}