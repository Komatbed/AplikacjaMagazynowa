package com.example.warehouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.warehouse.ui.theme.SafetyOrange

data class Lesson(
    val id: String,
    val title: String,
    val description: String,
    val content: String,
    val icon: ImageVector
)

val lessons = listOf(
    Lesson(
        id = "1",
        title = "Profile Okienne - Podstawy",
        description = "Budowa profilu, komory, uszczelki.",
        content = """
            1. Budowa Profilu:
            Współczesne profile PVC są wielokomorowe. Liczba komór wpływa na izolacyjność termiczną (więcej = cieplej).
            
            2. Wzmocnienia:
            Wewnątrz głównej komory zawsze musi znajdować się stalowe wzmocnienie. Decyduje ono o sztywności okna.
            
            3. Uszczelki:
            Wyróżniamy uszczelki przylgowe (zewnętrzna/wewnętrzna) oraz opcjonalnie uszczelkę środkową (MD), która poprawia szczelność i akustykę.
        """.trimIndent(),
        icon = Icons.Default.Info
    ),
    Lesson(
        id = "2",
        title = "Okucia activPilot - Elementy",
        description = "Zasuwnice, narożniki, zawiasy.",
        content = """
            1. Zasuwnica (GAK/GAM):
            Główny element ryglujący. Dobierana do wysokości skrzydła we wrębie (FFH).
            
            2. Narożnik (E1/E2):
            Przenosi napęd z zasuwnicy na górną część okna (do rozwórki).
            
            3. Rozwórka (SH):
            Odpowiada za funkcję uchylania. Dobierana do szerokości skrzydła (FFB).
            
            4. Zaczepy (SBS):
            Przykręcane do ramy. Ich rozmieszczenie decyduje o klasie odporności na włamanie (RC1/RC2).
        """.trimIndent(),
        icon = Icons.Default.Build
    ),
    Lesson(
        id = "3",
        title = "Szklenie i Pakiety Szybowe",
        description = "Ramki dystansowe, argon, powłoki.",
        content = """
            1. Budowa Pakietu:
            Dwie lub trzy tafle szkła oddzielone ramką dystansową. Przestrzeń wypełniona gazem szlachetnym (Argon/Krypton).
            
            2. Współczynnik Ug:
            Określa przenikanie ciepła przez szybę.
            - Pakiet 1-komorowy (2 szyby): Ug ~ 1.1
            - Pakiet 2-komorowy (3 szyby): Ug ~ 0.5 (Standard energooszczędny)
            
            3. Ciepła Ramka:
            Ramka z tworzywa sztucznego zamiast aluminium. Eliminuje mostek termiczny na krawędzi szyby.
        """.trimIndent(),
        icon = Icons.Default.PlayArrow
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    onBackClick: () -> Unit
) {
    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }

    if (selectedLesson != null) {
        LessonDetailScreen(
            lesson = selectedLesson!!,
            onBackClick = { selectedLesson = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("WindowsMaking 101", color = SafetyOrange) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = SafetyOrange
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Baza Wiedzy",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(lessons) { lesson ->
                    LessonCard(lesson = lesson, onClick = { selectedLesson = lesson })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonCard(lesson: Lesson, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = lesson.icon,
                contentDescription = null,
                tint = SafetyOrange,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = lesson.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailScreen(lesson: Lesson, onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lesson.title, color = SafetyOrange) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = SafetyOrange
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = lesson.content,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        }
    }
}
