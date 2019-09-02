import io.vertx.core.Future
import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.client.WebClientSession
import org.jsoup.Jsoup

const val username = "username"
const val password = "password"

fun main() {
    val vertx = Vertx.vertx()
    val option = WebClientOptions()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.132 Safari/537.36")
    val client = WebClient.create(vertx, option)

    val session = WebClientSession.create(client)

    val request1 = Future.future<HttpResponse<Buffer>> {
        session.getAbs("https://jwxt.sustech.edu.cn/jsxsd/framework/xsMain.jsp")
            .send(it)
    }.compose {
        println("\u001B[32m===============================\u001B[0m")
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

        println("\u001B[32m===============================\u001B[0m")
        println(location)

        Future.future<HttpResponse<Buffer>> { promise ->
            session.getAbs(location)
                .send(promise)
        }

    }.compose {

        println("\u001B[32m===============================\u001B[0m")
        println("CAS专跳到jwxt")

        Future.future<HttpResponse<Buffer>> { promise ->
            session.getAbs("https://jwxt.sustech.edu.cn/jsxsd/xsxk/xsxk_index?jx0502zbid=3E90176099AB4B3DA057E81E0FBD5181")
                .send(promise)
        }

    }.compose {

        println("\u001B[32m===============================\u001B[0m")
        println("选课主页")

        Future.future<HttpResponse<Buffer>> { promise ->
            session.getAbs("https://jwxt.sustech.edu.cn/jsxsd/xsxkkc/bxqjhxkOper?jx0404id=201920201001468&xkzy=&trjf=")
                .send(promise)
        }

    }.setHandler {

        println("\u001B[32m===============================\u001B[0m")

        vertx.close()
        if (it.failed()) {
            println("fail")
            it.cause().printStackTrace()
        } else {
            println(it.result().bodyAsString())

        }
    }

}
