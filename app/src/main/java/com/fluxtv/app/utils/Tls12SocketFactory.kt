package com.fluxtv.app.utils

import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Habilita TLSv1.1/TLSv1.2 en sockets SSL para compatibilidad con
 * Android TV viejos (API < 20) donde estos protocolos estan
 * deshabilitados por defecto.
 */
class Tls12SocketFactory(private val delegate: SSLSocketFactory) : SSLSocketFactory() {
    private val enabledProtocols = arrayOf("TLSv1.1", "TLSv1.2")

    override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites
    override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites

    private fun patch(s: Socket): Socket {
        if (s is SSLSocket) {
            try {
                val supported = s.supportedProtocols.toSet()
                s.enabledProtocols = enabledProtocols.filter { it in supported }.toTypedArray()
                    .ifEmpty { s.enabledProtocols }
            } catch (_: Exception) {}
        }
        return s
    }

    @Throws(IOException::class)
    override fun createSocket(s: Socket?, host: String?, port: Int, autoClose: Boolean): Socket =
        patch(delegate.createSocket(s, host, port, autoClose))

    @Throws(IOException::class)
    override fun createSocket(host: String?, port: Int): Socket =
        patch(delegate.createSocket(host, port))

    @Throws(IOException::class)
    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket =
        patch(delegate.createSocket(host, port, localHost, localPort))

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress?, port: Int): Socket =
        patch(delegate.createSocket(host, port))

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket =
        patch(delegate.createSocket(address, port, localAddress, localPort))
}
