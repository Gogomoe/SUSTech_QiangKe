package moe.gogo.qiangke

import io.vertx.core.MultiMap
import io.vertx.ext.web.client.WebClientSession
import io.vertx.kotlin.ext.web.client.sendAwait
import io.vertx.kotlin.ext.web.client.sendFormAwait
import org.jsoup.Jsoup


class Login(private val session: WebClientSession) {

    private var state: LoginState = LoginState.UNKNOWN
    private var execution: String? = null

    enum class LoginState {
        LOGIN, NOT_LOGIN, UNKNOWN
    }

    companion object {
        private const val MAIN_PAGE = "https://jwxt.sustech.edu.cn/jsxsd/framework/main.jsp"
        private const val CAS_TO_JWXT =
            "https://cas.sustech.edu.cn/cas/login?service=https%3A%2F%2Fjwxt.sustech.edu.cn%2Fjsxsd%2Fframework%2Fmain.jsp"
    }

    suspend fun isLogin(): Boolean {
        return when (state) {
            LoginState.LOGIN -> true
            LoginState.NOT_LOGIN -> false
            LoginState.UNKNOWN -> checkLogin()
        }
    }

    private suspend fun checkLogin(url: String = MAIN_PAGE): Boolean {
        val response = session.getAbs(url).sendAwait()
        val page = Jsoup.parse(response.bodyAsString())
        return when {
            page.title().contains("CAS") -> {
                println("登录状态：未登录")
                execution = page
                    .select("#fm1").first()
                    .select("[name=execution]").first().`val`()
                state = LoginState.NOT_LOGIN
                false
            }
            page.title().contains("学生个人中心") -> {
                println("登录状态：已登录")
                state = LoginState.LOGIN
                true
            }
            else -> {
                println("登录状态：未知")
                state = LoginState.UNKNOWN
                false
            }
        }
    }


    suspend fun login(username: String, password: String) {
        println("尝试登录")
        if (execution == null) {
            val response = session.getAbs(MAIN_PAGE).sendAwait()
            val page = Jsoup.parse(response.bodyAsString())
            execution = page
                .select("#fm1").first()
                .select("[name=execution]").first().`val`()
        }

        val eventId = "submit"
        val submit = "登录"

        val para = MultiMap.caseInsensitiveMultiMap().addAll(
            mutableMapOf(
                "username" to username,
                "password" to password,
                "execution" to execution,
                "_eventId" to eventId,
                "submit" to submit
            )
        )

        val response = session.postAbs(CAS_TO_JWXT).sendFormAwait(para)
        val location = response.getHeader("Location")
        println("Location: $location")

        checkLogin(location)
    }

}