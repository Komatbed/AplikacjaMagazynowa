package com.example.warehouse.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.net.Socket

object ZplPrinter {

    // Default template from LABEL_PRINTING.md
    private const val WASTE_LABEL_TEMPLATE = """
^XA
^PW800
^LL400

// Nagłówek: Typ Profila
^FO50,50^A0N,50,50^FD{profileCode}^FS

// Wielka Długość (Najważniejsze dla pracownika)
^FO50,120^A0N,150,150^FD{lengthMm} mm^FS

// Kolory
^FO50,280^A0N,40,40^FD{colorName}^FS

// Kod Kreskowy (Code 128) - zawiera unikalne ID odpadu
^FO450,50^BY3,3,100^BCN,100,Y,N,N
^FD{wasteId}^FS

// Lokalizacja docelowa
^FO450,280^A0N,40,40^FDPaleta: {location}^FS

^XZ
"""

    fun generateZpl(
        profileCode: String,
        lengthMm: Int,
        colorName: String,
        wasteId: String,
        location: String
    ): String {
        return WASTE_LABEL_TEMPLATE
            .replace("{profileCode}", profileCode)
            .replace("{lengthMm}", lengthMm.toString())
            .replace("{colorName}", colorName)
            .replace("{wasteId}", wasteId)
            .replace("{location}", location)
    }

    suspend fun printDirectly(ipAddress: String, port: Int = 9100, zplData: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Socket(ipAddress, port).use { socket ->
                    val out = DataOutputStream(socket.getOutputStream())
                    out.writeBytes(zplData)
                    out.flush()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
