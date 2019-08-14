package com.shenlan.base.auto

import com.shenlan.*
import org.apache.ibatis.annotations.Delete
import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Select
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.PostMapping
import com.shenlan.base.config.AppPro
import com.shenlan.base.util.log
import com.shenlan.base.util.toJsonString
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class Content : BaseModel {
    var fullName: String? = null
    var storeName: String? = null
    var filePath: String? = null
    var fileType: String? = null
    var fileSize: Int? = null

    constructor()
    constructor(fullName: String? = null, storeName: String? = null, filePath: String? = null, fileType: String? = null, fileSize: Int? = null) : super() {
        this.fullName = fullName
        this.storeName = storeName
        this.filePath = filePath
        this.fileType = fileType
        this.fileSize = fileSize
    }
}

class ContentSearch : BaseSearch()

@Mapper
interface ContentMapper : BaseMapper<Content> {
    @Select(value = ["<script>", "select id,fullName,storeName,filePath,fileType,fileSize from tbl_content", "</script>"])
    override fun getList(search: BaseSearch): List<Content>

    @Select("select id,fullName,storeName,filePath,fileType,fileSize from tbl_content where =#{arg0}")
    override fun getListByPid(id: String): List<Content>

    @Select("select id,fullName,storeName,filePath,fileType,fileSize from tbl_content where id = #{arg0}")
    override fun getInfo(id: String): Content

    @Insert("insert into tbl_content (id,fullName,storeName,filePath,fileType,fileSize) values (#{id,jdbcType=VARCHAR},#{fullName,jdbcType=VARCHAR},#{storeName,jdbcType=VARCHAR},#{filePath,jdbcType=VARCHAR},#{fileType,jdbcType=VARCHAR},#{fileSize,jdbcType=VARCHAR})")
    override fun insert(model: BaseModel): Int

    @Delete("delete from tbl_content where id = #{arg0}")
    override fun delete(id: String): Int
}

@Service
class ContentService(mapper: ContentMapper) : BaseService<Content, ContentMapper>(mapper)

@RestController
@RequestMapping("/api/Content")
class ContentResource(service: ContentService) : BaseResource<ContentSearch, Content, ContentMapper, ContentService>(service) {
    @PostMapping("/upload")
    fun upload(@RequestParam("file") file: MultipartFile): Result {
        val fileName = file.originalFilename!!
        val fileType = fileName.substring(fileName.lastIndexOf("."))
        var content = Content(fileType = fileType, fullName = fileName.replace(fileType, ""), filePath = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE), storeName = System.currentTimeMillis().toString() + fileType)
        val dir = File(AppPro.uploadPath + content.filePath)
        if (!dir.exists()) dir.mkdirs()
        File(AppPro.uploadPath + content.filePath + File.separator + content.storeName).writeBytes(file.bytes)
        log.info("upload file: {}", content.toJsonString)
        service.save(content)
        return Result.getSuccess(content)
    }
}

