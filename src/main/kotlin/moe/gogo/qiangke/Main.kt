package moe.gogo.qiangke

import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.client.WebClientSession
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.ext.web.client.sendAwait
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jsoup.Jsoup

const val username = "username"
const val password = "password"
val XUAN_KE_ID: String? = null
val LIST: List<Ke> = listOf(
    Ke("201920202000212", XuanKe.Type.BI_XIU, name = "离散")
)

fun main() {
    val vertx = Vertx.vertx()

    GlobalScope.launch(vertx.dispatcher()) {
        val session = createSession(vertx)

        val login = Login(session)
        while (!login.isLogin()) {
            login.login(username, password)
        }

        var xuanKeID: String? = XUAN_KE_ID
        var count = 0
        while (xuanKeID == null) {
            println("发现选课ID: 第 ${count++} 次")
            xuanKeID = findXuanKeID(session)
            delay(500)
        }

        enterXuanKePage(xuanKeID, session)

        val xuanKe = XuanKe(session, LIST)

        count = 0
        while (!xuanKe.allSuccess()) {
            println("选课: 第 ${count++} 次")
            xuanKe.selectAll()
            delay(500)
        }

        vertx.close()
    }
}

const val XUAN_KE_LIST_PAGE = "https://jwxt.sustech.edu.cn/jsxsd/xsxk/xklc_list"
const val XUAN_KE_VIEW = "https://jwxt.sustech.edu.cn/jsxsd/xsxk/xklc_view?{XUAN_KE_ID}"
const val XUAN_KE_PAGE = "https://jwxt.sustech.edu.cn/jsxsd/xsxk/xsxk_index?{XUAN_KE_ID}"

private fun createSession(vertx: Vertx): WebClientSession {
    val option = WebClientOptions()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.132 Safari/537.36")
    val client = WebClient.create(vertx, option)
    return WebClientSession.create(client)
}

suspend fun findXuanKeID(session: WebClientSession): String? {
    val response = session.getAbs(XUAN_KE_LIST_PAGE).sendAwait()
    val page = Jsoup.parse(response.bodyAsString())
    val links = page.select("#tbKxkc").select("""a[href^="/jsxsd/xsxk/xklc_view"]""")
    val ids = links.map {
        val attr = it.attr("href")
        attr.substring(attr.indexOf('?') + 1)
    }
    println("发现选课ID: $ids")
    return if (ids.size == 1) {
        println("选择选课ID: ${ids.first()}")
        ids.first()
    } else {
        println("选课ID: 多个ID无法断定")
        null
    }
}

suspend fun enterXuanKePage(xuanKeID: String, session: WebClientSession) {
    session.getAbs(XUAN_KE_VIEW.replace("{XUAN_KE_ID}", xuanKeID)).sendAwait()
    session.getAbs(XUAN_KE_PAGE.replace("{XUAN_KE_ID}", xuanKeID)).sendAwait()
}