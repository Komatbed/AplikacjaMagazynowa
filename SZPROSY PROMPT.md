# Prompt wdrożeniowy: **Kalkulator Szprosów V2 (szprosy wiedeńskie naklejane)**

Skopiuj cały ten prompt do Trae i wykonaj go **end-to-end**.

---

Jesteś seniorem Kotlin/Android + architektem domenowym. Pracujesz na istniejącym projekcie `AplikacjaMagazynowa`.
W projekcie istnieje już pierwszy kalkulator szprosów (`MuntinCalculator`, `MuntinScreen`, `MuntinViewModel`) i ma pozostać nienaruszony jako wariant V1.

## Cel biznesowy
Zaimplementuj **osobny moduł „Kalkulator Szprosów V2”** dla szprosów wiedeńskich naklejanych, który:
1. Przyjmuje dane skrzydła i profili.
2. Pozwala użytkownikowi wskazać konfigurację szprosów na „wirtualnym skrzydle” (interaktywnie).
3. Uwzględnia geometrię połączeń pod kątem (szpros-listwa, szpros-szpros) + luz technologiczny.
4. Daje gotową listę cięć: **wymiar, kąt lewy, kąt prawy, ilość, identyfikatory**.
5. Ma własną bazę profili tylko dla V2 (profile skrzydła, listew przyszybowych, szprosów).

## Twarde wymagania domenowe
Uwzględnij i zaimplementuj poniższą logikę:
- Baza długości roboczej osi (X/Y) startuje od wymiaru skrzydła i odejmuje:
  - `2 × wysokość profilu skrzydła`
  - `2 × efektywny zabór listwy przyszybowej` liczony z geometrii (kąt listwy ma wpływ).
- Gdy w danej osi jest tylko jeden szpros: policz jego długość z uwzględnieniem cięcia/łączenia z listwą pod kątem.
- Gdy szprosy się krzyżują:
  - nie stosuj zakładki,
  - domyślnie element „ciągły” to **pionowy** (jeden całościowy szpros),
  - użytkownik może to ręcznie zmienić (reguła krzyżowania),
  - elementy dochodzące mają długość pomniejszoną o szerokość elementu ciągłego (z korektą geometrii profilu).
- Na każdym łączeniu szpros-szpros oraz szpros-listwa zastosuj domyślnie **1.0 mm luzu na każdy punkt styku** (konfigurowalny).
- Każde cięcie i długość mają uwzględniać kąty wynikające z geometrii przekroju i kierunku łączenia.
- Na teraz obsłuż **listewki proste (pion/poziom)**; logikę ukośną przygotuj jako oddzielną ścieżkę rozszerzenia w przyszłości (bez wdrażania pełnej geometrii ukośnej teraz).

## Wymagania architektoniczne
1. **Nie psuj V1**: pozostaw obecny kalkulator jako osobny tryb.
2. Dodaj V2 jako nowy moduł/feature:
   - osobny ekran Compose,
   - osobny ViewModel,
   - osobny silnik obliczeń (`MuntinCalculatorV2` lub analogiczny),
   - osobna warstwa danych profili V2.
3. W UI dodaj przełącznik/wybór „Kalkulator V1 / Kalkulator V2”.
4. V2 ma własne modele domenowe, nie mieszaj ich z V1.

## Model danych V2 (minimum)
Zaprojektuj i wdroż:
- `SashProfileV2`:
  - `profileNo`,
  - `widthMm`,
  - `heightMm`,
  - `outerConstructionAngleDeg` (kąt zewnętrzny dla okna jako konstrukcji).
- `BeadProfileV2`:
  - `profileNo`,
  - `widthMm`,
  - `heightMm`,
  - `innerBeadAngleDeg` (kąt wewnętrzny).
- `MuntinProfileV2`:
  - `profileNo`,
  - `widthMm`,
  - `heightMm`,
  - `wallAngleDeg` (kąt ścianki).
- `V2GlobalSettings`:
  - `sawCorrectionMm` (korekta piły),
  - `windowCorrectionMm` (korekta strony zewnętrznej),
  - `assemblyClearanceMm` (luz montażowy, domyślnie 1.0 mm).
- `V2ProfileCatalog` (repozytorium/baza lokalna tylko dla V2, seed + CRUD).
- `CutItemV2`:
  - `sashNo`, `slatNo`, `muntinNo` (**numeracja globalna dla całego skrzydła**),
  - `axis` (X/Y),
  - `lengthMm`,
  - `leftAngleDeg`, `rightAngleDeg`,
  - `qty`,
  - `profileName`,
  - `description` (np. `pionowy`, `lewy1`, `lewy2`, `prawy1`, `prawy2` licząc od góry),
  - `notes`.

## UX V2 (minimum)
Na nowym ekranie V2 dostarcz:
- Dane wejściowe:
  - numer skrzydła,
  - wymiar skrzydła (szerokość/wysokość),
  - wybór profilu skrzydła, listwy, szprosa z bazy V2,
  - luzy (domyślnie 1 mm),
  - ustawienia globalne modułu (`korekta piły`, `korekta okna`, `luz montażowy`),
  - wybór reguły krzyżowania (który element jest ciągły).
- Interaktywne „wirtualne skrzydło”:
  - dodawanie/usuwanie szprosów pionowych i poziomych,
  - numerowanie elementów (listewka/szpros),
  - podgląd punktów przecięć.
- Wynik:
  - tabela listy cięć w kolejności: **pierwszy kąt, wymiar, drugi kąt, opis**,
  - dodatkowo opcjonalna wizualizacja listewki/szprosa, którego dotyczy dany rekord,
  - agregacja po identycznych cięciach (długość+kąty+profil),
  - bez integracji ERP na tym etapie; output to lokalna lista cięć,
  - osobna sekcja „**Montaż / znakowanie**” z informacją:
    - na jakiej wysokości/szerokości skrzydła montować każdy szpros,
    - jaki wymiar od **zewnętrznej krawędzi skrzydła** podać operatorowi do narysowania znacznika,
    - dla podziału symetrycznego znacznik liczony zawsze symetrycznie,
    - **kreska znacznika oznacza środek szprosa (oś szprosa)**.


## Perspektywa montera (obowiązkowa)
Projektując logikę i UI V2, AI ma „wejść w rolę montera szprosów wiedeńskich”:
- wynik ma minimalizować ryzyko pomyłki na hali,
- wszystkie wymiary montażowe i znaczniki podawaj w sposób jednoznaczny, praktyczny i produkcyjny,
- przy każdym rekordzie montażowym jasno podawaj punkt odniesienia: **zewnętrzna krawędź skrzydła**,
- opisy elementów mają odpowiadać realnej kolejności pracy (np. od góry / od lewej) i ułatwiać szybkie odnalezienie miejsca montażu,
- jeśli reguła może być dwuznaczna, preferuj wariant najbezpieczniejszy dla montera i dodaj notatkę ostrzegawczą.

## Matematyka / algorytm
Zaimplementuj czytelny pipeline:
1. `computeUsableOpening()` – światło robocze po odjęciu profilu skrzydła i listwy przyszybowej.
2. `buildTopology()` – graf linii szprosów i przecięć na podstawie siatki/kliknięć.
3. `resolveJoints()` – wyznaczenie elementów ciągłych i dochodzących + redukcje długości.
4. `applyClearances()` – odjęcia luzów technologicznych per końcówka.
5. `solveCutAngles()` – obliczenia kątów lewego/prawego cięcia.
6. `generateCutList()` – lista cięć + agregacja ilości.
7. `applySawAndWindowCorrections()` – na końcu potoku zastosuj `sawCorrectionMm` i `windowCorrectionMm` wg reguł technologicznych.

8. `generateMountingMarks()` – wygeneruj listę wymiarów montażowych/znaczników (offset liczony od **zewnętrznej krawędzi skrzydła**) dla każdego szprosa; dla układów symetrycznych licz po osi szprosa.

Wdrożenia dokonaj tak, aby każda faza była testowalna osobno.

## Testy i walidacja (obowiązkowe)
Dodaj testy jednostkowe dla V2:
- przypadek A: jeden szpros w osi bez krzyżowania,
- przypadek B: krzyż 1×1 z elementem ciągłym + dochodzącym,
  - domyślnie ciągły pionowy,
- przypadek C: wiele podziałów z różnymi profilami,
- przypadek D: wpływ zmiany kąta listwy,
- przypadek E: wpływ luzu 1 mm oraz luzu niestandardowego.

- przypadek F: poprawność wymiarów znaczników montażowych (kreska = środek szprosa) dla podziału symetrycznego.

Dodaj testy UI/ViewModel (minimum smoke):
- zmiana parametrów powoduje rekalkulację,
- lista cięć odświeża się i agreguje poprawnie.

Po implementacji uruchom pełen zestaw testów repo + nowe testy V2 i przedstaw wynik.

## Kryteria akceptacji (Definition of Done)
- V1 działa jak wcześniej.
- V2 działa niezależnie i ma własną bazę profili.
- Użytkownik kończy z jednoznaczną listą cięć: długość/kąty/ilość/identyfikatory.
- Lista cięć jest czytelna produkcyjnie: `kąt1 → wymiar → kąt2 → opis` + globalna numeracja elementów dla całego skrzydła.
- Krzyżowania, kąty i luz 1 mm są poprawnie uwzględnione.
- Kod ma sensowne nazwy, jest podzielony na małe funkcje, bez duplikacji.
- Testy automatyczne przechodzą.



## Zakres na teraz vs. wiele skrzydeł (wyjaśnienie)
- Na teraz implementuj V2 dla **jednego skrzydła na kalkulację**.
- Numeracja `slatNo` i `muntinNo` jest globalna **w obrębie pojedynczego skrzydła**.
- Jeśli w przyszłości pojawi się tryb wielu skrzydeł, agregację wykonuj po polu `sashNo` i dodaj widok zbiorczy jako osobny krok (poza zakresem tego wdrożenia).

## Wymagany format odpowiedzi końcowej od Trae
1. Krótkie podsumowanie zmian per plik.
2. Lista decyzji algorytmicznych.
3. Wyniki testów (komendy + output).
4. Ryzyka i ograniczenia (jeśli jakieś pozostały).
5. Instrukcja dla użytkownika: jak użyć V2 krok po kroku.

---

## Dodatkowe wytyczne implementacyjne do tego repo
- Punkt startowy obecnego V1 znajdziesz m.in. w:
  - `app/src/main/java/com/example/warehouse/util/MuntinCalculator.kt`
  - `app/src/main/java/com/example/warehouse/ui/viewmodel/MuntinViewModel.kt`
  - `app/src/main/java/com/example/warehouse/ui/screens/MuntinScreen.kt`
- Zadbaj o kompatybilność z Compose i aktualną strukturą nawigacji aplikacji.
- Unikaj „big-bang refactor”; wprowadzaj V2 inkrementalnie.


---***********************----------------SZPROSY TRYB SKOŚNY


# Prompt wdrożeniowy: **Kalkulator Szprosów V2 – Tryb Skośny (osobna zakładka)**

Skopiuj cały ten prompt do Trae i wykonaj go **end-to-end**.

---

Jesteś seniorem Kotlin/Android + architektem domenowym. Pracujesz w projekcie `AplikacjaMagazynowa`.
W projekcie istnieje kalkulator szprosów V1 oraz prompt dla V2 (tryb prosty). Teraz wdrażasz **osobny tryb skośny** jako **nową zakładkę** wewnątrz narzędzia V2.

## Cel
Dodać w V2 nową zakładkę: **„Tryb skośny”**, która obsłuży:
1. Szprosy pod zadanymi kątami.
2. Krzyże + ukosy (układ mieszany).
3. Wzór **„pajęczyna”** (radialny).
4. Wzór **łukowy** (szprosy po łuku oraz dojścia).

## Twarde wymagania funkcjonalne
- Tryb skośny jest **osobnym promptem i osobną zakładką UI** (nie mieszać z zakładką prostą).
- Dostępne predefiniowane kąty: **15°, 22.5°, 30°, 45°, 60°, 75°, 90°**.
- Dopuść też „kąt własny” (manual), ale waliduj zakres 0–90°.
- Zachowaj dotychczasowe zasady technologiczne z V2:
  - luz na punkt styku,
  - korekta piły,
  - korekta okna,
  - opis cięć produkcyjnych.
- Punkt odniesienia dla montażu/znaczników: **zewnętrzna krawędź skrzydła**.

## Zakres geometrii trybu skośnego
### A) Pojedyncze i wielokrotne ukosy
- Użytkownik dodaje linie pod wybranym kątem.
- System oblicza przecięcia z granicą pola roboczego.
- Każda linia ma:
  - długość rzeczywistą,
  - kąty cięcia L/P,
  - pozycję znacznika montażowego (oś szprosa).

### B) Krzyż + ukosy
- Układ łączony: pion/poziom + linie skośne.
- Reguła domyślna ciągłości:
  - pionowy element bazowy pozostaje domyślnie ciągły,
  - elementy dochodzące skracane o szerokość elementu ciągłego + luz per styk.
- Użytkownik może wymusić inną regułę ciągłości.

### C) Pajęczyna
- Definicja wejścia:
  - środek układu,
  - liczba ramion,
  - opcjonalne pierścienie (okręgi/segmenty łukowe),
  - kąt startowy.
- Wynik:
  - lista promieniowych elementów,
  - lista elementów obwodowych (jeśli aktywne pierścienie),
  - pełna lista cięć i znaczników.

### D) Łuk
- Definicja wejścia:
  - promień lub cięciwa + strzałka łuku,
  - zakres kąta łuku,
  - liczba podziałów łuku,
  - wariant: same segmenty łukowe / łuk + dojścia proste.
- Wynik:
  - długości cięciw i/lub segmentów,
  - kąty cięcia wynikowe,
  - oznaczenia montażowe od krawędzi zewnętrznej skrzydła.

## Modele danych (skośny)
Dodaj osobne modele, np.:
- `AngularModeConfigV2`:
  - `allowedAnglesDeg: List<Double>` domyślnie `[15, 22.5, 30, 45, 60, 75, 90]`,
  - `customAngleEnabled: Boolean`.
- `DiagonalLineV2`:
  - `lineId`, `angleDeg`, `offsetRefMm`, `isContinuous`, `profileNo`.
- `SpiderPatternV2`:
  - `centerX`, `centerY`, `armCount`, `startAngleDeg`, `ringCount`, `ringSpacingMm`.
- `ArchPatternV2`:
  - `radiusMm` lub (`chordMm`, `sagittaMm`),
  - `arcStartDeg`, `arcEndDeg`, `divisionCount`, `withStraightJoins`.
- `MountMarkV2`:
  - `itemId`, `referenceEdge`, `offsetMm`, `axisDescription`.

## UX (zakładka „Tryb skośny”)
1. Przełącznik zakładek: `Prosty` | `Skośny`.
2. Panel ustawień kąta:
   - szybkie przyciski: 15 / 22.5 / 30 / 45 / 60 / 75 / 90,
   - opcja „kąt własny”.
3. Canvas interaktywny:
   - dodawanie linii skośnych,
   - snap do środka i punktów przecięć,
   - podgląd osi szprosa i numeracji globalnej.
4. Presety układów:
   - `Krzyż + ukosy`, `Pajęczyna`, `Łuk`.
5. Wynik produkcyjny:
   - tabela: `kąt1 → wymiar → kąt2 → opis`,
   - sekcja `Montaż / znakowanie` od zewnętrznej krawędzi skrzydła,
   - opcjonalna mini-wizualizacja elementu przy rekordzie.

## Algorytm (pipeline)
1. `computeUsableOpening()` – jak w V2.
2. `buildAngularTopology()` – topologia linii skośnych, punktów przecięć i kontaktu z obwiednią.
3. `resolveCrossingPriority()` – ciągłość elementów (domyślnie pionowy bazowy, chyba że użytkownik zmieni).
4. `trimByIntersectionsAndProfileWidth()` – skracanie elementów dochodzących o geometrię styku.
5. `applyClearancesAndCorrections()` – luz per styk + korekty piły/okna.
6. `solveMitersAndEndAngles()` – kąty końcowe dla cięć skośnych.
7. `generateCutList()` – lista cięć i agregacja.
8. `generateMountingMarks()` – znaczniki osi szprosa od zewnętrznej krawędzi skrzydła.

## Testy obowiązkowe
Dodaj testy jednostkowe dla trybu skośnego:
- T1: pojedynczy ukos 45°.
- T2: 2 ukosy przecinające się + reguła ciągłości.
- T3: krzyż + ukosy (układ mieszany).
- T4: zestaw kątów predefiniowanych (15/22.5/30/45/60/75/90).
- T5: pajęczyna (min. 8 ramion, 1 pierścień).
- T6: łuk (cięciwa+strzałka, kilka podziałów).
- T7: poprawność znaczników montażowych liczonych od zewnętrznej krawędzi skrzydła.

Dodaj testy UI/ViewModel (smoke):
- zmiana zakładki na „Skośny” przełącza logikę,
- wybór kąta aktualizuje wizualizację i listę cięć,
- preset „Pajęczyna” i „Łuk” generuje dane bez błędów.

## Definition of Done
- Zakładka „Skośny” działa niezależnie od trybu prostego.
- Obsługuje wszystkie kąty predefiniowane + kąt własny.
- Obsługuje: krzyż+ukosy, pajęczynę i łuk.
- Wynik zawiera listę cięć oraz znaczniki montażowe od zewnętrznej krawędzi skrzydła.
- Testy przechodzą.

## Wymagany format odpowiedzi końcowej od Trae
1. Zmiany per plik.
2. Decyzje algorytmiczne (skośny/pajęczyna/łuk).
3. Wyniki testów (komendy + output).
4. Ryzyka i ograniczenia.
5. Instrukcja dla montera: krok po kroku jak wyznaczyć i nanieść znaczniki.
