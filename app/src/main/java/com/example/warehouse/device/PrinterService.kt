package com.example.warehouse.device

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket

class PrinterService {

    suspend fun testConnection(ip: String, port: Int): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, port), 2000) // 2s timeout
                socket.close()
                Result.success("Połączenie udane")
            } catch (e: Exception) {
                Result.failure(Exception("Błąd połączenia: ${e.message}"))
            }
        }
    }

    suspend fun printTestLabel(ip: String, port: Int): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, port), 2000)
                
                val zpl = """
                    ^XA
                    ^FO50,50^ADN,36,20^FDTest Drukarki Zebra^FS
                    ^FO50,100^ADN,18,10^FDIP: $ip Port: $port^FS
                    ^FO50,150^BY3
                    ^BCN,100,Y,N,N
                    ^FDTEST-1234^FS
                    ^XZ
                """.trimIndent()

                val writer = OutputStreamWriter(socket.getOutputStream())
                writer.write(zpl)
                writer.flush()
                writer.close()
                socket.close()
                
                Result.success("Wysłano etykietę testową")
            } catch (e: Exception) {
                Result.failure(Exception("Błąd druku: ${e.message}"))
            }
        }
    }
}
