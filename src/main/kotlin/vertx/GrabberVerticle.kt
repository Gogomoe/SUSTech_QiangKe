package vertx

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.client.WebClientSession
import io.vertx.ext.web.client.spi.CookieStore

class GrabberVerticle : AbstractVerticle() {

    private val option = WebClientOptions()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.132 Safari/537.36")
    private lateinit var client: WebClient

    private lateinit var cookieStore: CookieStore
    private lateinit var session: WebClientSession

    override fun start() {
        client = WebClient.create(vertx, option)
        cookieStore = CookieStore.build()
        session = WebClientSession.create(client, cookieStore)

        vertx.eventBus().consumer<CookieStore>("update cookie") { message ->
            this.cookieStore = message.body()
            this.session = WebClientSession.create(client, cookieStore)
        }
        vertx.eventBus().consumer<Unit>("grab") { message ->
            Future.future<HttpResponse<Buffer>> { promise ->
                session.getAbs("https://jwxt.sustech.edu.cn/jsxsd/xsxkkc/bxqjhxkOper?jx0404id=201920201001468&xkzy=&trjf=")
                    .send(promise)
            }.setHandler {
                if (it.succeeded()) {
                    val body = it.result().bodyAsString()
                    println(body)
                    if (!body.startsWith("{")) {
                        vertx.eventBus().request<CookieStore>("check cookie", Unit) { reply ->
                            //reply.cause()?.printStackTrace()
                            this.cookieStore = reply.result().body()
                            this.session = WebClientSession.create(client, cookieStore)
                        }
                    }
                } else {
                    it.cause().printStackTrace()
                }
            }
        }
    }
}