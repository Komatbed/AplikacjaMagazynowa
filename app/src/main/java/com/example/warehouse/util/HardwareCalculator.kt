package com.example.warehouse.util

enum class FittingSystem(val displayName: String) {
    ACTIVPILOT_CONCEPT("Winkhaus activPilot Concept"),
    ACTIVPILOT_SELECT("Winkhaus activPilot Select (Ukryte)")
}

data class FittingComponent(
    val name: String,
    val articleNumber: String?, // Opcjonalnie
    val quantity: Int = 1,
    val note: String? = null
)

object HardwareCalculator {

    /**
     * Calculates the required hardware components for Winkhaus activPilot.
     * Dimensions are in millimeters (Rebate Dimensions - Wymiar we wrębie).
     * @param profileCode Optional profile code to determine specific frame keeps.
     */
    fun calculate(system: FittingSystem, ffb: Int, ffh: Int, profileCode: String? = null): List<FittingComponent> {
        val components = mutableListOf<FittingComponent>()

        // 1. Zasuwnica (Drive Gear) - Zależna od FFH
        val driveGear = getDriveGear(system, ffh)
        if (driveGear != null) components.add(driveGear)
        else return listOf(FittingComponent("BŁĄD: FFH poza zakresem (${ffh}mm)", null, 0))

        // 2. Narożniki (Corner Drives)
        // Standardowo: 1 dolny (chyba że Select), 1 górny (z rozwórką)
        if (system == FittingSystem.ACTIVPILOT_CONCEPT) {
            components.add(FittingComponent("Narożnik E1 (Dół)", "4928574"))
        } else {
            components.add(FittingComponent("Narożnik Dolny Select (V.AK.95)", "5001234"))
        }

        // 3. Rozwórka / Szyna (Shear) - Zależna od FFB
        val shear = getShear(system, ffb)
        if (shear != null) components.add(shear)
        else return listOf(FittingComponent("BŁĄD: FFB poza zakresem (${ffb}mm)", null, 0))

        // 4. Zawiasy (Hinges)
        if (system == FittingSystem.ACTIVPILOT_CONCEPT) {
            components.add(FittingComponent("Zawias Dolny Ramy + Skrzydła", "Standard"))
            components.add(FittingComponent("Zawias Górny Ramy + Skrzydła", "Standard"))
        } else {
            components.add(FittingComponent("Zawias Dolny Select", "Komplet"))
            components.add(FittingComponent("Zawias Górny Select", "Komplet"))
        }

        // 5. Zaczepy (Keeps) - Ilość zależna od obwodu (uproszczona logika)
        // Zaczepy są ściśle zależne od profilu
        val securityKeeps = calculateSecurityKeeps(ffb, ffh)
        val keepName = if (profileCode != null) "Zaczep Ramowy (Profil $profileCode)" else "Zaczep Ramowy (Uniwersalny)"
        val keepArticle = if (profileCode != null) "SBS.$profileCode" else "SBS.UNI"
        
        components.add(FittingComponent(keepName, keepArticle, securityKeeps, "Rozmieszczenie wg schematu"))

        // 6. Blokada Błędnego Położenia Klamki (DFE)
        if (ffh > 800) {
            components.add(FittingComponent("Blokada DFE / TFE", "Standard"))
        }

        return components
    }

    private fun getDriveGear(system: FittingSystem, ffh: Int): FittingComponent? {
        // Uproszczone zakresy GR (Gear Size) dla activPilot
        // Nazewnictwo: GAM (Zasuwnica Stała) lub GAK (Zmienna) - przyjmujemy GAK/GAM standard
        return when (ffh) {
            in 260..460 -> FittingComponent("Zasuwnica G-260", "GR.0")
            in 461..600 -> FittingComponent("Zasuwnica G-460", "GR.1")
            in 601..800 -> FittingComponent("Zasuwnica G-600", "GR.1")
            in 801..1050 -> FittingComponent("Zasuwnica G-800", "GR.2")
            in 1051..1300 -> FittingComponent("Zasuwnica G-1000", "GR.3")
            in 1301..1600 -> FittingComponent("Zasuwnica G-1400", "GR.4", note = "Klamka 700mm") // Typowa balkonowa niska
            in 1601..1850 -> FittingComponent("Zasuwnica G-1600", "GR.5", note = "Klamka 1050mm")
            in 1851..2300 -> FittingComponent("Zasuwnica G-2300", "GR.6/7", note = "Klamka 1050mm, 2 rygla")
            else -> null
        }
    }

    private fun getShear(system: FittingSystem, ffb: Int): FittingComponent? {
        // Uproszczone zakresy rozwórek (Shears)
        return when (ffb) {
            in 260..400 -> FittingComponent("Narożnik z rozwórką krótką", "E1.SK")
            in 401..600 -> FittingComponent("Szyna Rozwórki 250", "SH.1")
            in 601..800 -> FittingComponent("Szyna Rozwórki 500", "SH.2")
            in 801..1050 -> FittingComponent("Szyna Rozwórki 750", "SH.3")
            in 1051..1400 -> FittingComponent("Szyna Rozwórki 1000", "SH.4")
            else -> null
        }
    }

    private fun calculateSecurityKeeps(ffb: Int, ffh: Int): Int {
        // Bardzo uproszczona reguła: 1 zaczep na każde rozpoczęte 600mm obwodu
        val perimeter = (2 * ffb) + (2 * ffh)
        return (perimeter / 700).coerceAtLeast(2)
    }
}
