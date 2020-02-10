package moe.gogo.qiangke

import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClientSession
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.ext.web.client.sendAwait
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

const val BASE_URL = "https://jwxt.sustech.edu.cn"

data class Ke(val id: String, val type: XuanKe.Type, val name: String? = null)

class XuanKe(val session: WebClientSession, kes: List<Ke>) {

    private val states = kes.map { State(it) }

    private val context = Vertx.currentContext().dispatcher()
    private val scope = CoroutineScope(context)

    data class State(val ke: Ke, var success: Boolean = false, var message: String = "")

    enum class Type(val perfix: String) {
        BI_XIU("/jsxsd/xsxkkc/bxxkOper"),
        XUAN_XIU("/jsxsd/xsxkkc/xxxkOper"),
        BEN_XUE_QI_JI_HUA("/jsxsd/xsxkkc/bxqjhxkOper"),
        KUA_NIAN_JI("/jsxsd/xsxkkc/knjxkOper"),
        KUA_ZHUAN_YE("/jsxsd/xsxkkc/fawxkOper"),
        GONG_XUAN("/jsxsd/xsxkkc/ggxxkxkOper")
    }

    suspend fun selectAll() {
        scope.launch {
            states.filter { !it.success }.forEach {
                launch {
                    select(it)
                }
            }
        }.join()
    }

    suspend fun select(state: State): Boolean {
        val ke = state.ke
        val response = session.getAbs("$BASE_URL${ke.type.perfix}?jx0404id=${ke.id}&xkzy=&trjf=").sendAwait()
        val result = response.bodyAsJsonObject()
        state.success = result.getBoolean("success")
        state.message = result.getString("message")
        val name = ke.name ?: ke.id
        println("$name ${state.success} ${state.message}")
        return state.success
    }

    fun allSuccess() = states.all { it.success }

}