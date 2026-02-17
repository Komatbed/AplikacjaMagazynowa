# Jak policzyć wymiary dla profilu, listwy i szprosów (przykład)

Ten plik pokazuje, jak na podstawie zmierzonego gotowego skrzydła policzyć wartości, które trzeba wpisać w aplikacji w oknach:
- „Dodaj Profil (V3)”
- „Dodaj Listwę (V3)”
- „Dodaj Szpros (V3)”

oraz ogólną metodę, jak to powtarzać dla innych systemów.

---

## 1. Dane z rzeczywistego skrzydła (przykład)

- Wymiar zewnętrzny skrzydła:  
  - szerokość `W_f = 900 mm`  
  - wysokość `H_f = 1405 mm`
- Wymiar pakietu szybowego (IGU):  
  - szerokość `W_g = 772 mm`  
  - wysokość `H_g = 1277 mm`
- Luz do szklenia: `C ≈ 6 mm` na stronę (na klocki szklarskie).
- Szpros pionowy: długość cięcia ok. `1240 mm` (informacja pomocnicza).
- Szprosy poziome: `356,5 mm` (informacja pomocnicza).
- Listwa przyszybowa: szerokość `25 mm`, wysokość `27,5 mm`, kąt twarzy ok. `18°`.
- Szpros dekoracyjny: szerokość `25 mm`, wysokość `12 mm`, kąt cięcia szprosów ok. `18°` (uśredniony).

---

## 2. Od skrzydła do wymiaru wrębu pod szkło

Aplikacja liczy najpierw **światło wrębu pod szkło** (miejsce, gdzie wchodzi pakiet), a dopiero potem można z tego wyprowadzić wymiar pakietu (odejmując luz).

1. Najpierw do wymiaru pakietu dodajemy luz po 6 mm na stronę:

   - Szerokość wrębu pod szkło:
     - `W_r = W_g + 2 * C = 772 + 2 * 6 = 784 mm`
   - Wysokość wrębu:
     - `H_r = H_g + 2 * C = 1277 + 2 * 6 = 1289 mm`

2. Ile „zabiera” profil skrzydła + listwa przyszybowa z każdej strony?

   - Po szerokości:  
     - całkowity ubytek = `W_f – W_r = 900 – 784 = 116 mm`  
     - na jedną stronę: `D_r = 116 / 2 = 58 mm`
   - Po wysokości:  
     - `H_f – H_r = 1405 – 1289 = 116 mm`  
     - na jedną stronę również `58 mm`.

Oznacza to, że:

> **W tym konkretnym oknie ~58 mm na każdą stronę to łączne „odsunięcie” od krawędzi skrzydła do płaszczyzny szkła/listwy, czyli wkład profilu skrzydła + listwy przyszybowej. To właśnie na tej powierzchni są później klejone szprosy.**

---

## 3. Interpretacja 58 mm – profil a listwa

Rozpiszmy ogólną zależność:

```text
odsunięcie od krawędzi skrzydła = odsunięcie profilu + odsunięcie listwy + luz
```

W podanym przez Ciebie oknie:

- **mierzone 58 mm na stronę to już wymiar „do płaszczyzny listwy”, czyli do miejsca, w którym faktycznie spoczywa szkło i gdzie klejone są szprosy.**  
  To oznacza, że:
  - `Profile.glassOffsetX/Y + Bead.effectiveGlassOffset ≈ 58 mm`,
  - luz do szklenia ≈ `6 mm` na stronę (wynika z różnicy między wrębem a pakietem),
  - same **wymiary listwy** (wysokość i kąt) są potrzebne, żeby poprawnie policzyć **rzeczywistą długość szprosów**, bo szpros klei się właśnie do listwy, a nie do „gołego” profilu.

Dalej w konfiguracji możesz rozdzielić te 58 mm na „odsunięcie profilu” i „efektywne odsunięcie listwy” – ważne, żeby ich suma zgadzała się z rzeczywistym pomiarem do powierzchni klejenia.

---

## 4. Szacowanie odsunięcia listwy przyszybowej (opcjonalnie)

Listwa z przykładu ma:

- wysokość `h_b = 27,5 mm`,
- kąt twarzy `α ≈ 18°`.

Jeśli trafisz na system, w którym listwa **realnie zawęża wrąb** (tj. bez listwy szkło miałoby większy wymiar), możesz oszacować jej wkład:

```text
Efektywny offset listwy ≈ h_b * cos(α)
```

Przykładowo:

- `cos(18°) ≈ 0,95`
- `B_eff ≈ 27,5 mm * 0,95 ≈ 26,1 mm`

W praktyce przy szprosach naklejanych na listwę **to właśnie efektywny offset listwy ma duże znaczenie**, bo od niego zależy, jak bardzo listwa „wchodzi” w naroża i o ile trzeba skrócić szpros względem czystego światła szkła.

> **Docelowo chodzi o to, aby suma `Profile.glassOffset + Bead.effectiveGlassOffset` odpowiadała dokładnemu wymiarowi od krawędzi skrzydła do powierzchni listwy, na której klejony jest szpros.**

Dzięki temu aplikacja może policzyć zarówno poprawny wymiar wrębu/pakietu, jak i **dokładną długość szprosów z uwzględnieniem listwy i kąta.**

---

## 5. Proponowane wartości do wpisania w aplikacji

Na podstawie powyższych obliczeń możesz wprowadzić jeden „zestaw” danych V3 dla tego okna:

### Profil (V3) – przykład do aplikacji

- Nazwa: np. `Profil PVC 82`
- Odsunięcie szkła X od ramy (mm): **32**
- Odsunięcie szkła Y od ramy (mm): **32**
- Kąt zewnętrzny naroża (°): **90**

Wzór używany w aplikacji (uproszczony):

```text
GlassWidth_app  = FrameWidth  – 2 * (Profile.glassOffsetX + Bead.effectiveGlassOffset)
GlassHeight_app = FrameHeight – 2 * (Profile.glassOffsetY + Bead.effectiveGlassOffset)
```

Dla Twojego systemu ważne jest, aby:

- `Profile.glassOffsetX/Y ≈ 32 mm`
- `Bead.effectiveGlassOffset ≈ 26 mm`
- `Profile.glassOffsetX/Y + Bead.effectiveGlassOffset ≈ 58 mm`  
  (czyli razem dają to, co zmierzyłeś od krawędzi skrzydła do powierzchni listwy/szkła),

czyli:

- `GlassWidth_app = 900 – 2 * 58 = 784 mm`
- `GlassHeight_app = 1405 – 2 * 58 = 1289 mm`

To są **wymiary wrębu pod szkło**.  
Jeśli chcesz wymiar pakietu, odejmujesz po 6 mm na stronę:

- `Wymiar pakietu = 784 – 2 * 6 = 772 mm`  
- `Wymiar pakietu = 1289 – 2 * 6 = 1277 mm`

czyli dokładnie to, co zmierzyłeś.

### Listwa przyszybowa (V3) – przykład do aplikacji

- Nazwa: np. `Listwa 25/27,5`
- Kąt twarzy listwy (°): **18**
- Efektywne odsunięcie szkła (mm): **26**  
  (zaokrąglone `B_eff ≈ 27,5 mm * cos(18°)`, czyli tyle mm listwa „zabiera” z wrębu po jednej stronie; wtedy `Profile.glassOffset ≈ 58 mm – 26 mm ≈ 32 mm`)

### Szpros (V3)

- Nazwa: np. `Szpros 25/12`
- Szerokość (mm): **25** (szerokość widoczna na szybie)
- Grubość szprosa (mm): **12** (wymiar „w głąb”)
- Kąt ścianki szprosa (°): **18**  
  (uśredniony kąt z Twojego przykładu – używany do korekt długości przy cięciu)

---

## 6. Ogólna recepta dla innych systemów

Dla innego profilu/listwy możesz powtórzyć ten sam schemat:

1. **Zmierzone dane**
   - zewnętrzny wymiar skrzydła: `W_f`, `H_f`,
   - wymiar pakietu szybowego: `W_g`, `H_g`,
   - założony luz na stronę: `C` (np. 5–6 mm),
   - geometria listwy: wysokość `h_b`, kąt twarzy `α`.

2. **Policz wymiary wrębu pod szkło**:

   ```text
   W_r = W_g + 2 * C
   H_r = H_g + 2 * C
   ```

3. **Policz całkowity ubytek na stronę** od skrzydła do wrębu:

   ```text
   D_r_width  = (W_f – W_r) / 2
   D_r_height = (H_f – H_r) / 2
   ```

   Jeśli profil jest symetryczny, te wartości powinny być bardzo zbliżone – przyjmij średnią:

   ```text
   D_r ≈ (D_r_width + D_r_height) / 2
   ```

4. **Zdecyduj, czy listwa zawęża wrąb**

- Jeśli listwa **nie zmienia wymiaru wrębu** (typowa listwa, która siedzi „w środku” profilu), możesz przyjąć:
  - `Profile.glassOffset ≈ D_r`
  - `Bead.effectiveGlassOffset ≈ 0`
- Jeśli listwa **realnie zmniejsza wymiar wrębu** (np. gruba listwa, inna geometria), możesz oszacować:

  ```text
  Bead.effectiveGlassOffset ≈ h_b * cos(α)
  Profile.glassOffset ≈ D_r – Bead.effectiveGlassOffset
  ```

5. **Wpisz do aplikacji**:

   - Profil:
     - `glassOffsetX ≈ Profile.glassOffset`
     - `glassOffsetY ≈ Profile.glassOffset` (jeśli symetryczny)
     - `outerAngleDeg = 90` dla prostokątnych skrzydeł.
   - Listwa:
     - `angleFace = α`
     - `effectiveGlassOffset ≈ h_b * cos(α)`
   - Szpros:
     - szerokość = szerokość widoczna profilu,
     - grubość = wymiar w głąb,
     - `wallAngleDeg` = realny kąt ścianki szprosa (np. 18°).

W ten sposób aplikacja będzie liczyć:

- oś szprosa w świetle szkła,
- a następnie oś od **zewnętrznego wymiaru skrzydła**,
- z uwzględnieniem rzeczywistej geometrii profilu i listwy oraz luzów do szklenia.
