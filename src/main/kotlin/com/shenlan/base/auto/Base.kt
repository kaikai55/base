package com.shenlan

import com.shenlan.base.config.CustomObjectMapper
import com.shenlan.base.mybatis.interceptor.PageInterceptor
import com.shenlan.base.util.notEmpty
import com.shenlan.base.util.isEmpty
import com.shenlan.base.util.uuid
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import javax.validation.Valid

//import com.validate.validate
val objectMapper = CustomObjectMapper()

open class BaseModel {
    var id: String = ""
    override fun toString(): String {
        return objectMapper.writeValueAsString(this)
    }
}

open class BaseSearch(var currentPage: Int = -1, var pageRecord: Int = -1) {
    var startTime: String = ""
    var endTime: String = ""
}

interface BaseMapper<L : BaseModel> {
    fun getList(search: BaseSearch = BaseSearch()): List<L>
    fun getInfo(id: String): L?
    fun insert(model: BaseModel): Int
    fun delete(id: String): Int

    fun getListByPid(id: String): List<L>
    fun insertList(modelList: List<BaseModel>): Int

    @Update("begin \${sql} ; end;")
    fun updateSql(@Param("sql")sql: String)

    @Select("\${sql}")
    fun getSql(@Param("sql")sql: String): Any

    @Select("\${sql}")
    fun getSqlList(@Param("sql")sql: String): List<Any>

//    fun deleteChildren(ids: String): Int
    //    fun update(model: BaseModel): Int
//    fun updateList(modelList: List<BaseModel>): Int
}

class Result {
    var rlt = 0 // 0成功 1失败
    var info = "success"
    var datas: Any? = null

    constructor() {
        // empty
    }

    constructor(rlt: Int, info: String) {
        this.rlt = rlt
        this.info = info
    }

    constructor(rlt: Int, info: String, datas: Any) {
        this.rlt = rlt
        this.info = info
        this.datas = datas
    }

    constructor(datas: Any?) {
        this.datas = datas
    }

    override fun toString(): String {
        return objectMapper.writeValueAsString(this)
    }

    companion object {
        val SUCCESS = 0
        val FAIL = 1
        val success = Result(0, "success")

        fun getSuccess(datas: Any?): Result {
            return Result(datas)
        }

        fun getError(info: String): Result {
            return Result(1, info)
        }

        fun getError(info: String, datas: Any): Result {
            return Result(1, info, datas)
        }
    }

}

open class BaseService<T : BaseModel, M : BaseMapper<T>>(var mapper: M) {

    open fun getListByPid(id: String): List<T> {
        var list = mapper.getListByPid(id)
        list.forEach { getChildren(it) }
        return list
    }

    open fun getList(page: BaseSearch): Result {
        val result: Any?
        if (page.currentPage > 0 && page.pageRecord > 0) {
            PageInterceptor.startPage(page.currentPage, page.pageRecord)
            mapper.getList(page)
            result = PageInterceptor.endPage()
        } else {
            result = mapper.getList(page)
        }
        return Result.getSuccess(result)
    }

    open var getChildren = { _: T -> }
    open fun getInfo(id: String): Result {
        val model = mapper.getInfo(id)
        if (model != null)
            getChildren(model)
        return Result.getSuccess(model)
    }

    open var insertChildren = { _: T -> }
    open var validate = { _: T -> "" }

    @Transactional
    open fun save(model: T): Result {
        var error = validate(model)
        if (error.notEmpty()) {
            return Result.getError(error)
        }
        if (model.id.notEmpty()) {
            delete(model.id)
        }
        if (model.id.isEmpty()) model.id = uuid()
        if (1 == mapper.insert(model)) {
            insertChildren(model)
        }
        return Result.getSuccess(model.id)
    }

    open var deleteChildren = { _: T -> }
    @Transactional
    open fun delete(id: String): Result {
        var datas = getInfo(id).datas
        if (datas != null) {
            deleteChildren(datas as T)
        }
        return Result.getSuccess(mapper.delete(id))
    }
}

open class BaseResource<T : BaseSearch, G : BaseModel, M : BaseMapper<G>, F : BaseService<G, M>>(var service: F) {

    @PostMapping("/getList")
    fun getList(@Valid @RequestBody search: T) = service.getList(search)

    @PostMapping("/save")
    fun save(@Valid @RequestBody model: G) = service.save(model)

    @GetMapping("/delete/{id}")
    fun delete(@PathVariable id: String) = service.delete(id)

    @GetMapping("/getInfo/{id}")
    fun getInfo(@PathVariable id: String) = service.getInfo(id)
}

