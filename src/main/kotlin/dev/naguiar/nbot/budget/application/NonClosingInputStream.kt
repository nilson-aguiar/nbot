package dev.naguiar.nbot.budget.application

import java.io.InputStream

internal class NonClosingInputStream(
    private val delegate: InputStream,
) : InputStream() {
    override fun read(): Int = delegate.read()

    override fun read(b: ByteArray): Int = delegate.read(b)

    override fun read(
        b: ByteArray,
        off: Int,
        len: Int,
    ): Int = delegate.read(b, off, len)

    override fun skip(n: Long): Long = delegate.skip(n)

    override fun available(): Int = delegate.available()

    override fun mark(readlimit: Int) = delegate.mark(readlimit)

    override fun reset() = delegate.reset()

    override fun markSupported(): Boolean = delegate.markSupported()

    override fun close() {
        // Intentionally empty — prevents XML parser from closing ZipInputStream
    }
}
