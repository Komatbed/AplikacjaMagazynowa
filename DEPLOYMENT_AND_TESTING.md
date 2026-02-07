# Dokumentacja Wdrożeniowa i Testowa

## 1. Raport Pokrycia Testami (Test Coverage Report)

**Cel:** Osiągnięcie minimum 80% pokrycia kodu testami jednostkowymi dla logiki biznesowej.

### Obecny Stan (Szacunkowy):
- **FileProcessingOptimizer:** ~95% (Krytyczna logika optymalizacji, wszystkie tryby, obsługa błędów parsowania).
- **InventoryViewModel:** ~70% (Logika UI, obsługa stanów, mockowanie repozytorium).
- **InventoryRepository:** ~40% (Wymaga refaktoringu, trudne do testowania bez in-memory DB).
- **UI Components:** ~10% (Testy UI nie są obecnie priorytetem w stosunku do logiki).

### Plan Uzupełnienia:
1. **Repository Layer:** Dodać testy integracyjne z `Room.inMemoryDatabaseBuilder`.
2. **Utils:** Pokryć testami wszystkie klasy pomocnicze (`CsvParser`, `DateUtils`).
3. **UseCases:** Po wydzieleniu logiki do UseCase, pokryć je w 100%.

---

## 2. Analiza Bezpieczeństwa (Security Analysis - OWASP Mobile Top 10)

### M1: Improper Platform Usage
- **Status:** OK. Używamy standardowych komponentów Androida (Intent, ContentProvider).
- **Zalecenie:** Sprawdzić flagi `exported` w `AndroidManifest.xml`.

### M2: Insecure Data Storage
- **Zagrożenie:** Baza danych SQLite (Room) jest niezaszyfrowana.
- **Rozwiązanie:** Wdrożono **SQLCipher** dla Room. Klucze szyfrowania przechowywane w **EncryptedSharedPreferences** (AndroidX Security).

### M3: Insecure Communication
- **Zagrożenie:** Komunikacja z API (przyszła) po HTTP.
- **Rozwiązanie:** Wdrożono `network_security_config.xml`. Wymusza HTTPS dla połączeń zewnętrznych, zezwala na cleartext tylko dla sieci lokalnych (LAN/Dev).

### M5: Insufficient Cryptography
- **Zagrożenie:** Słabe algorytmy haszowania (jeśli używane).
- **Rozwiązanie:** Używać SHA-256 lub wyższych. Nie używać MD5. (SQLCipher używa AES-256).

### M9: Reverse Engineering
- **Zagrożenie:** Kod łatwy do dekompilacji.
- **Rozwiązanie:** Skonfigurowano **ProGuard/R8** w `proguard-rules.pro`. W buildzie release należy ustawić `minifyEnabled true`.

---

## 3. Raport Analizy Statycznej (SonarQube / Lint Simulation)

### Zidentyfikowane Problemy (High Priority):
1. **God Object:** `InventoryRepository` (zbyt duża odpowiedzialność).
2. **Magic Numbers:** Występują w `FileProcessingOptimizer` (np. 100000 offsetu kosztu) - należy przenieść do stałych (`const val`).
3. **Hardcoded Strings:** Część tekstów w UI nie korzysta z `strings.xml`.
4. **Deprecated API:** Użycie `Divider` i starych ikon (Naprawione w ostatniej iteracji).

### Dług Techniczny (Technical Debt):
- **Wysoki:** Warstwa danych (brak separacji interfejsów).
- **Średni:** Obsługa błędów w UI (czasami generyczne "Error").
- **Niski:** Styl kodowania (Kotlin Style Guide jest przestrzegany).

---

## 4. Instrukcja Wdrożenia (Deployment Guide)

### Wymagania Środowiskowe
- **JDK:** 17 lub nowszy (zalecany 21 dla Gradle 9.0+ compatibility).
- **Android Studio:** Koala / Ladybug lub nowsze.
- **Android SDK:** API 35 (compileSdk), API 24 (minSdk).

### Budowanie Aplikacji (Build Process)

1. **Czyszczenie Projektu:**
   ```bash
   ./gradlew clean
   ```

2. **Uruchomienie Testów:**
   ```bash
   ./gradlew testDebugUnitTest
   ```

3. **Budowanie APK (Debug):**
   ```bash
   ./gradlew assembleDebug
   ```
   Wynik: `app/build/outputs/apk/debug/app-debug.apk`

4. **Budowanie APK (Release):**
   ```bash
   ./gradlew assembleRelease
   ```
   *Wymaga skonfigurowanego keystore w `gradle.properties`.*

### Konfiguracja Release (gradle.properties)
```properties
MYAPP_UPLOAD_STORE_FILE=my-upload-key.keystore
MYAPP_UPLOAD_STORE_PASSWORD=***
MYAPP_UPLOAD_KEY_ALIAS=my-key-alias
MYAPP_UPLOAD_KEY_PASSWORD=***
```

### Instalacja na Urządzeniu
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Weryfikacja Po Wdrożeniu (Smoke Test)
1. Uruchom aplikację.
2. Przejdź do Ustawień -> Zarezerwowane Odpady. Dodaj wartość (np. 1500).
3. Przejdź do Optymalizacji. Załaduj plik testowy.
4. Wybierz tryb "Odpady Zdefiniowane".
5. Zweryfikuj, czy wynik zawiera odpady o długości 1500mm (±10mm).
