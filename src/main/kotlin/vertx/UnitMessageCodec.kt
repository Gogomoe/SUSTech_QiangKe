package vertx

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec

class UnitMessageCodec : MessageCodec<Unit, Unit> {

    override fun encodeToWire(buffer: Buffer, s: Unit) {}

    override fun decodeFromWire(pos: Int, buffer: Buffer): Unit = Unit

    override fun transform(s: Unit): Unit = s

    override fun name(): String = "Unit"

    override fun systemCodecID(): Byte = -1
}
