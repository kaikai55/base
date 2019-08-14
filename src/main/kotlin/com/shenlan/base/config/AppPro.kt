package com.shenlan.base.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.io.File


@ConfigurationProperties(prefix = "application", ignoreUnknownFields = true)
@Component
class AppPro{

     fun setActive(value: String) {
          AppPro.active = value
     }

     companion object {
          var active: String = "dev"//getBean(AppPro::class.java).active
          val uploadPathFolder = "gco_upload"
          val uploadPath = System.getProperty("user.dir") + File.separator + uploadPathFolder + File.separator
     }
}