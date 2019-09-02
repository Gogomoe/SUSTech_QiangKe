package vertx

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.MultiMap
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.client.WebClientSession
import io.vertx.ext.web.client.spi.CookieStore
import org.jsoup.Jsoup

class LoginVerticle : AbstractVerticle() {

    private lateinit var username: String
    private lateinit var password: String
    private lateinit var xkid: String

    private val option = WebClientOptions()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.132 Safari/537.36")
    private lateinit var client: WebClient

    private lateinit var cookieStore: CookieStore
    private lateinit var session: WebClientSession

    override fun start() {
        username = context.config().getString("username")
        password = context.config().getString("password")
        xkid = context.config().getString("xkid")

        client = WebClient.create(vertx, option)
        cookieStore = CookieStore.build()
        session = WebClientSession.create(client, cookieStore)

        vertx.eventBus().consumer<Unit>("check cookie") { message ->
            testLogin().setHandler { testResult ->
                println("test:$testResult")
                if (testResult.result()) {
                    message.reply(cookieStore)
                    return@setHandler
                }
                loginAndPublish()
                message.reply(cookieStore)
            }
        }
    }

    private fun loginAndPublish() {
        login().setHandler {
            if (it.succeeded()) {
                publishCookie(it.result())
            }
        }
    }

    private fun publishCookie(cookieStore: CookieStore) {
        this.cookieStore = cookieStore
        this.session = WebClientSession.create(client, cookieStore)
        vertx.eventBus().publish("update cookie", cookieStore)
    }

    private fun testLogin(): Future<Boolean> {
        return Future.future<HttpResponse<Buffer>> {
            session.getAbs("https://jwxt.sustech.edu.cn/jsxsd/framework/xsMain.jsp")
                .send(it)
        }.map {
            !it.bodyAsString().contains("Central Authentication Service")
        }
    }

    private fun login(): Future<CookieStore> {

        return Future.future<HttpResponse<Buffer>> {
            session.getAbs("https://jwxt.sustech.edu.cn/jsxsd/framework/xsMain.jsp")
                .send(it)
        }.compose {

            println("拿到登陆页面")

            val page = Jsoup.parse(it.bodyAsString())
            val form = page.select("#fm1").first()
            val execution = form.select("[name=execution]").first().`val`()
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

            Future.future<HttpResponse<Buffer>> { promise ->
                session.postAbs("https://cas.sustech.edu.cn/cas/login?service=https%3A%2F%2Fjwxt.sustech.edu.cn%2Fjsxsd%2Fframework%2FxsMain.jsp")
                    .sendForm(para, promise)
            }

        }.compose {

            val location = it.getHeader("Location")
            println("CAS登陆，location: $location")

            Future.future<HttpResponse<Buffer>> { promise ->
                session.getAbs(location)
                    .send(promise)
            }

        }.compose {
            println("CAS专跳到jwxt")

            Future.future<HttpResponse<Buffer>> { promise ->
                session.getAbs("https://jwxt.sustech.edu.cn/jsxsd/xsxk/xsxk_index?jx0502zbid=$xkid")
                    .send(promise)
            }

        }.map {
            println("选课主页")
            session.cookieStore()
        }

    }
}