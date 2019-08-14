package com.shenlan.base.web

import com.shenlan.Result
import com.shenlan.base.util.log
import com.shenlan.base.util.errorlog
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.http.HttpStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus


/**
 * Created by Administrator on 2018/11/4.
 */
@ControllerAdvice
class ExceptionTranslator {

    /**
     * hibernate validate 校验异常
     * @param ex
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    fun processValidationError(ex: MethodArgumentNotValidException): Result {
        println("hibernate validate 校验异常")
        var list = ex.bindingResult.fieldErrors.map { it.defaultMessage }.distinct()
        return Result.getError(list.subList(0, if (list.size > 5) 5 else list.size).joinToString("<br/>"))
    }

    @ExceptionHandler(Exception::class)
    @ResponseBody
    fun processException(ex: Exception): Result {
        log.error("Error in ExceptionTranslator")
        errorlog(ex)
        if (ex is HttpMessageConversionException) {
            return Result.getError("数据格式错误!")
        }
        return Result.getError("系统异常!")
    }
}
