import com.shenlan.BaseMapper
import org.apache.ibatis.io.Resources
import org.apache.ibatis.session.SqlSession
import org.apache.ibatis.session.SqlSessionFactory
import org.apache.ibatis.session.SqlSessionFactoryBuilder
import org.junit.Before
import org.junit.Test
import java.io.IOException

object MybatisUtil {
    private var sqlSessionFactory: SqlSessionFactory? = null
    val sqlSession: SqlSession
        get() = getSqlSessionFactory()!!.openSession(false)

    fun getSqlSessionFactory(): SqlSessionFactory? {
        if (sqlSessionFactory == null) {
            val resource = "mybatis-config.xml"
            val inputStream = Resources.getResourceAsStream(resource)
            sqlSessionFactory = SqlSessionFactoryBuilder().build(inputStream, "dev")
        }
        return sqlSessionFactory
    }
}

//由于用spring测试的时候 很费时间  这是用来测试mapper的
class MybatisTest {
    lateinit var session: SqlSession
    @Before
    fun before() {
        session = MybatisUtil.sqlSession
//        mapper = getMapper(FlowinstanceMapper::class.java)
    }

    fun <T> getMapper(type: Class<T>): T =session.getMapper(type)

    @Test
    fun test1() {
//        println(mapper.getSql("select SerialNumber from tbl_flowtask ").toString().toInt())
    }
}