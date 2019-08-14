package com.shenlan.base.auto

import com.alibaba.fastjson.JSONObject
import com.shenlan.Result
import com.shenlan.base.Application
import com.shenlan.base.util.log
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api")
class UserResource {
    @GetMapping("/csrf")
    fun csrf(): Result = Result.success
}

@RestController
@RequestMapping("/api/operator")
class OperatorResource {
    @GetMapping("getStatus")
    fun getStatus(): Result = Result.success

    //关闭系统
    var password = "{\"password\":\"ddd\"}"
    @PostMapping("shutdown")
    fun shutdown(@RequestBody json: JSONObject) {
        log.info("shutdown: {}", json.toString())
        if (json.toString().equals(password)) {
            Application.context?.close()
        }
    }

    @GetMapping("test")
    fun test(): Result {
        return Result.getSuccess(System.getProperty("user.dir") + "  " + System.getProperty("user.home"))
    }
}