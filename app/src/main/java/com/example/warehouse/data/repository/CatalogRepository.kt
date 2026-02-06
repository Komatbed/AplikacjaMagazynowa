package com.example.warehouse.data.repository

import com.example.warehouse.data.model.CatalogCategory
import com.example.warehouse.data.model.CatalogProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CatalogRepository {

    private val categories = listOf(
        CatalogCategory("VEKA", "VEKA", "Systemy profili okiennych PCV klasy A"),
        CatalogCategory("SALAMANDER", "SALAMANDER", "Ekskluzywne profile okienne i drzwiowe"),
        CatalogCategory("ALURON", "ALURON", "Systemy aluminiowe dla budownictwa"),
        CatalogCategory("WINKHAUS", "WINKHAUS", "Innowacyjne systemy okuć okiennych i drzwiowych")
    )

    private val products = listOf(
        // VEKA Products
        CatalogProduct(
            id = "v1",
            categoryId = "VEKA",
            name = "VEKA Softline 82",
            shortDescription = "Energooszczędny system o głębokości zabudowy 82 mm.",
            fullDescription = "System VEKA Softline 82 to nowa generacja profili o wielokomorowej geometrii. Zapewnia doskonałą izolacyjność termiczną dzięki głębokości zabudowy 82 mm i systemowi trzech uszczelek.",
            usage = "Budownictwo energooszczędne i pasywne. Okna, drzwi balkonowe, drzwi wejściowe.",
            technicalDetails = "Uf = 1.0 W/m2K, 7 komór w ramie, 6 komór w skrzydle."
        ),
        CatalogProduct(
            id = "v2",
            categoryId = "VEKA",
            name = "VEKA Perfectline",
            shortDescription = "Klasyczny system 5-komorowy o szerokości 70 mm.",
            fullDescription = "Stabilny i sprawdzony system 5-komorowy. Ścianki zewnętrzne o grubości 3 mm (klasa A) zapewniają wysoką wytrzymałość i izolację akustyczną.",
            usage = "Standardowe budownictwo mieszkaniowe, renowacje, budynki użyteczności publicznej.",
            technicalDetails = "Uf = 1.3 W/m2K, Klasa A, 5 komór."
        ),

        // SALAMANDER Products
        CatalogProduct(
            id = "s1",
            categoryId = "SALAMANDER",
            name = "bluEvolution 82",
            shortDescription = "Optymalna efektywność energetyczna.",
            fullDescription = "System 6-komorowy o głębokości zabudowy 82 mm. Łączy w sobie wyśmienitą oszczędność energii oraz innowacyjną technologię uszczelniania.",
            usage = "Okna rozwierno-uchylne, drzwi wejściowe, drzwi przesuwne.",
            technicalDetails = "Uf do 1.0 W/m2K, uszczelnienie środkowe."
        ),
        CatalogProduct(
            id = "s2",
            categoryId = "SALAMANDER",
            name = "Streamline 76",
            shortDescription = "Wysoki komfort i bezpieczeństwo.",
            fullDescription = "System 5-komorowy o głębokości 76 mm. Atrakcyjny design skrzydła (proste lub zaokrąglone) i wysokie parametry techniczne.",
            usage = "Nowoczesne budownictwo, renowacje.",
            technicalDetails = "Uf = 1.3 W/m2K, śnieżnobiała biel."
        ),

        // ALURON Products
        CatalogProduct(
            id = "a1",
            categoryId = "ALURON",
            name = "AS 75",
            shortDescription = "System trójkomorowy o wysokiej izolacyjności.",
            fullDescription = "System przeznaczony do konstruowania izolowanych termicznie drzwi i okien. Zastosowano w nim nowatorskie przekładki termiczne ANTI-BI-METAL.",
            usage = "Witryny, okna, drzwi, fasady w budynkach biurowych i użyteczności publicznej.",
            technicalDetails = "Głębokość ościeżnicy 75 mm, Uf od 0,8 W/m2K."
        ),
        CatalogProduct(
            id = "a2",
            categoryId = "ALURON",
            name = "AS 178HS",
            shortDescription = "Drzwi podnoszono-przesuwne (HST).",
            fullDescription = "System pozwala na tworzenie wielkogabarytowych przeszkleń z ruchomymi skrzydłami. Niski próg zapewnia komfort użytkowania (bez barier).",
            usage = "Wyjścia na taras, ogrody zimowe, zabudowa balkonów.",
            technicalDetails = "Ciężar skrzydła do 400 kg, szklenie do 59 mm."
        ),

        // WINKHAUS Products
        CatalogProduct(
            id = "w1",
            categoryId = "WINKHAUS",
            name = "activPilot Concept",
            shortDescription = "Modułowy system okuć rozwierno-uchylnych.",
            fullDescription = "activPilot to system, który łączy funkcjonalność, estetykę i bezpieczeństwo. Oparty na trzpieniach grzybkowych (ośmiokątnych), które współpracują ze standardowymi lub antywłamaniowymi zaczepami.",
            usage = "Okna i drzwi balkonowe z PCV, drewna i aluminium. Umożliwia łatwą zmianę standardu bezpieczeństwa.",
            technicalDetails = "Regulacja docisku na grzybkach, mikrowentylacja w standardzie, blokada obrotu klamki."
        ),
        CatalogProduct(
            id = "w2",
            categoryId = "WINKHAUS",
            name = "activPilot Select",
            shortDescription = "Okucie z ukrytymi zawiasami.",
            fullDescription = "Całkowicie ukryte zawiasy w luzie wrębowym. Zapewnia nienaganny wygląd okna (widoczna tylko klamka) oraz lepszą izolacyjność termiczną (brak mostków termicznych na zawiasach).",
            usage = "Nowoczesne wnętrza, okna o dużej wadze (do 150 kg), renowacja zabytków.",
            technicalDetails = "Nośność do 150 kg, kąt otwarcia do 95 stopni, łatwy montaż."
        ),
        CatalogProduct(
            id = "w3",
            categoryId = "WINKHAUS",
            name = "activPilot Comfort PADK",
            shortDescription = "Okucie z funkcją równoległego odstawienia.",
            fullDescription = "Oprócz otwierania i uchylania, umożliwia odsunięcie skrzydła od ramy o 6mm na całym obwodzie. Zapewnia to bezpieczne i energooszczędne wietrzenie.",
            usage = "Wietrzenie podczas nieobecności (klasa RC2 nawet w pozycji wietrzenia), sypialnie, szkoły.",
            technicalDetails = "Szczelina 6mm, odporność na włamanie w pozycji wietrzenia."
        )
    )

    fun getCategories(): Flow<List<CatalogCategory>> = flow {
        emit(categories)
    }

    fun getProductsByCategory(categoryId: String): Flow<List<CatalogProduct>> = flow {
        emit(products.filter { it.categoryId == categoryId })
    }

    fun getProductById(productId: String): Flow<CatalogProduct?> = flow {
        emit(products.find { it.id == productId })
    }
}
