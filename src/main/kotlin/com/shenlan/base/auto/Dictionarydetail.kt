package com.shenlan.base.auto

import com.shenlan.BaseModel
import com.shenlan.Result
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Select
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.GetMapping


/**
 * Created by Administrator on 2018/10/25.
 */
class Dict : BaseModel() {

     var dictCode: String? = null // 字典类型代码

     var serialNumber: Int = 0 // 序号

     var itemCode: String? = null // 字典项代码

     var itemName: String? = null // 字典项名称

     var pitemCode: String? = null // 父字典项代码
}

@Mapper
interface DictMapper {
    @Select("select DictCode, SerialNumber, ItemCode, ItemName,  PItemCode from TBL_DICTIONARYDETAIL")
    fun getAll(): List<Dict>
}

@RestController
@RequestMapping("/api/dictionarydetail")
class DictResource(var mapper: DictMapper) {
    @GetMapping("/getAll")
    fun getAll(): Result {
        return Result.getSuccess(mapper.getAll())
    }
}