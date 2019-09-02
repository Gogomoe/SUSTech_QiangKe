package vertx

import io.vertx.core.AbstractVerticle
import io.vertx.core.CompositeFuture
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.client.WebClientSession
import io.vertx.ext.web.client.impl.CookieStoreImpl
import io.vertx.ext.web.client.spi.CookieStore

class MainVerticle : AbstractVerticle() {

    override fun start() {
        val option = DeploymentOptions().setConfig(
            JsonObject()
                .put("username", "username")
                .put("password", "password")
                .put("xkid", "3E90176099AB4B3DA057E81E0FBD5181")
        )

        vertx.eventBus().registerDefaultCodec(CookieStoreImpl::class.java, CookieStoreMessageCodec())
        vertx.eventBus().registerDefaultCodec(Unit::class.java, UnitMessageCodec())

        val deploy1 = Future.future<String> { vertx.deployVerticle(LoginVerticle::class.java, option, it) }
        val deploy2 = Future.future<String> { vertx.deployVerticle(GrabberVerticle::class.java, option, it) }

        CompositeFuture.join(deploy1, deploy2).setHandler {
            it.cause()?.printStackTrace()
            vertx.eventBus().request<CookieStore>("check cookie", Unit) {
                vertx.setPeriodic(1000) { id ->
                    vertx.eventBus().send("grab", Unit)
                }
            }
        }
    }

}