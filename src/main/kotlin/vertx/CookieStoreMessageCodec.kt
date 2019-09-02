package vertx

import com.google.gson.Gson
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.eventbus.impl.codecs.StringMessageCodec
import io.vertx.ext.web.client.impl.CookieStoreImpl

class CookieStoreMessageCodec : MessageCodec<CookieStoreImpl, CookieStoreImpl> {

    private val stringCodec = StringMessageCodec()
    private val gson = Gson()

    override fun decodeFromWire(pos: Int, buffer: Buffer?): CookieStoreImpl? {
        val str = stringCodec.decodeFromWire(pos, buffer)
        return if (str == "null") {
            null
        } else {
            gson.fromJson(str, CookieStoreImpl::class.java)
        }
    }

    override fun encodeToWire(buffer: Buffer?, s: CookieStoreImpl?) {
        val str = if (s == null) {
            "null"
        } else {
            gson.toJson(s)
        }
        stringCodec.encodeToWire(buffer, str)
    }

    override fun systemCodecID(): Byte = -1

    override fun transform(s: CookieStoreImpl?): CookieStoreImpl? = s

    override fun name(): String = "CookieStoreImplCodec"

}
