package com.shenlan.base.mybatis.interceptor

import org.apache.ibatis.executor.resultset.ResultSetHandler
import org.apache.ibatis.executor.statement.StatementHandler
import org.apache.ibatis.reflection.SystemMetaObject
import org.apache.ibatis.reflection.MetaObject
import org.apache.ibatis.mapping.BoundSql
import org.apache.ibatis.mapping.MappedStatement
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler
import org.apache.ibatis.plugin.*
import org.slf4j.LoggerFactory
import java.sql.*
import java.util.*


/**
 * Created by Administrator on 2018/9/12.
 */
@Intercepts(Signature(type = StatementHandler::class, method = "prepare",args = arrayOf(Connection::class,Integer::class)), Signature(type = ResultSetHandler::class, method = "handleResultSets",args=arrayOf(Statement::class)))
//@Intercepts({ @Signature(type = StatementHandler.class, method = "prepare", ),
//    @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = { Statement.class }) })
open class PageInterceptor : Interceptor {
    private val log = LoggerFactory.getLogger(PageInterceptor::class.java)

    @Throws(Throwable::class)
    override fun intercept(invocation: Invocation): Any? {
        if (localPage.get() == null) {
            return invocation.proceed()
        }
        if (invocation.target is StatementHandler) {
            val statementHandler = getStatementHandler(invocation)
            val metaStatementHandler = SystemMetaObject.forObject(statementHandler)
            val mappedStatement = metaStatementHandler
                    .getValue("delegate.mappedStatement") as MappedStatement
            val boundSql = metaStatementHandler.getValue("delegate.boundSql") as BoundSql
            val page = localPage.get()

            setTotal(invocation, metaStatementHandler, mappedStatement, boundSql, page)

            return setPage(invocation, metaStatementHandler, mappedStatement, boundSql, page)
        } else if (invocation.target is ResultSetHandler) {
            val result = invocation.proceed()
            log.info("result: {}", result)
            localPage.get().result = result
            return result
        }
        return invocation.proceed()
    }

    private fun setTotal(invocation: Invocation, metaStatementHandler: MetaObject, mappedStatement: MappedStatement,
                         boundSql: BoundSql, page: Page) {
        val countSql = getCountSql(boundSql.sql)
        val connection = invocation.args[0] as Connection
        var ps: PreparedStatement? = null
        var total = 0
        var rs: ResultSet? = null
        try {
            ps = connection.prepareStatement(countSql)
            val countBoundSql = BoundSql(mappedStatement.configuration, countSql,
                    boundSql.parameterMappings, boundSql.parameterObject)
            val handler = DefaultParameterHandler(mappedStatement, boundSql.parameterObject,
                    countBoundSql)
            handler.setParameters(ps)
            rs = ps.executeQuery()
            while (rs.next()) {
                total = rs.getInt(1)
            }
            page.recordCount = total.toLong()
            page.pageCount = if (total == 0) 0 else (total - 1) / page.pageRecord + 1
        } catch (e: Exception) {
            log.error("{}", e)
        } finally {
            try {
                if (rs != null) {
                    rs.close()
                }
                if (ps != null) {
                    ps.close()
                }
            } catch (e: SQLException) {
                log.error("{}", e)
            }

        }
    }

    private fun getCountSql(sql: String): String {
        return String.format("select count(*) from (%s) t", sql)
    }

    private fun setPage(invocation: Invocation, metaStatementHandler: MetaObject, mappedStatement: MappedStatement,
                        boundSql: BoundSql, page: Page): PreparedStatement? {
        val pageSql = buildPageSql(boundSql.sql, page)
        metaStatementHandler.setValue("delegate.boundSql.sql", pageSql)
        var ps: PreparedStatement? = null
        try {
            ps = invocation.proceed() as PreparedStatement
            val count = ps.getParameterMetaData().parameterCount
            ps.setInt(count - 1, page.pageOne)
            ps.setInt(count, page.pageTwo)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ps
    }

    private fun getStatementHandler(invocation: Invocation): StatementHandler? {
        val statementHandler = invocation.target as StatementHandler
        var metaStatementHandler = SystemMetaObject.forObject(statementHandler)
        // 分离代理对象链(由于目标类可能被多个拦截器拦截，从而形成多次代理，通过下面的两次循环
        // 可以分离出最原始的的目标类)
        var `object`: Any? = null
        while (metaStatementHandler.hasGetter("h")) {
            `object` = metaStatementHandler.getValue("h")
            metaStatementHandler = SystemMetaObject.forObject(`object`)
        }
        return if (`object` == null) statementHandler else `object` as StatementHandler?
    }

    private fun buildPageSql(sql: String, page: Page): String {
        val pageSql = StringBuilder(200)
        if (DBTYPE_SQLSERVER == dbType) {
            //			String order = "(select 0)";
            //			tmpSql = tmpSql.replace("ORDER BY", orderBy);
            //			String[] str = tmpSql.split(orderBy);
            //			if (str.length == 2) {
            //				order = str[1];
            //			}
            //			pageSql.append("select rn.* from (select top ").append(page.endRow);
            //			pageSql.append(" temp.*,row_number() over(order by").append(order).append(") SerialNumber from (");
            //			pageSql.append(str[0]).append(") temp ) rn where rn.SerialNumber > ").append(page.endRow);
        } else if (DBTYPE_ORACLE == dbType) {
            pageSql.append("select rn.* from ( select temp.*, rownum XH from ( ").append(sql)
                    .append(" ) temp where rownum <= ? ) rn where XH > ?")
            page.pageOne = page.endRow
            page.pageTwo = page.startRow
        } else if (DBTYPE_MYSQL == dbType) {
            pageSql.append(sql).append(" limit ?,?")
            page.pageOne = page.startRow
            page.pageTwo = page.pageRecord
        }
        log.info("page sql: {}", pageSql)
        return pageSql.toString()
    }


    override fun plugin(target: Any): Any {
        return if (target is StatementHandler || target is ResultSetHandler) {
            Plugin.wrap(target, this)
        } else {
            target
        }
    }

    override fun setProperties(properties: Properties) {
        // not implementation
    }

    class Page
    /**
     *
     * @param pageNum
     * @param pageSize
     * @param type
     * 0:SqlServer 1:oracle 2:mysql
     */
    (var currentPage: Int, var pageRecord: Int) {
        var startRow: Int = 0
        var endRow: Int = 0
        //分页的两个参数
        var pageOne: Int = 0
        var pageTwo: Int = 0
        var recordCount: Long = 0
        var pageCount: Int = 0
        //		private int dbType; //
        var result: Any? = null

        init {
            this.startRow = if (currentPage > 0) (currentPage - 1) * pageRecord else 0
            this.endRow = currentPage * pageRecord
            //			this.dbType = type;
        }

        override fun toString(): String {
            return ("Page [currentPage=" + currentPage + ", pageRecord=" + pageRecord + ", startRow=" + startRow
                    + ", endRow=" + endRow + ", recordCount=" + recordCount + ", pageCount=" + pageCount + ", result=" + result + "]")
        }
    }

    companion object {
        private val localPage = ThreadLocal<Page>()
        val DBTYPE_MYSQL = "mysql"
        val DBTYPE_ORACLE = "oracle"
        val DBTYPE_SQLSERVER = "sqlserver"
        var dbType = DBTYPE_ORACLE

        fun startPage(pageNum: Int, pageSize: Int) {
            localPage.set(Page(pageNum, pageSize))
        }

        fun endPage(): Page {
            val page = localPage.get()
            localPage.remove()
            return page
        }
    }
}
