---
title: "Instrukcja konfiguracji palet – pallet_config.json"
---

# 1. Cel pliku `pallet_config.json`

Plik `pallet_config.json` służy do **pełnej konfiguracji wszystkich palet magazynowych**:
- opisuje, jakie palety istnieją (etykiety takie jak `01A`, `07C`, `25B`),
- definiuje **pojemność**, próg przepełnienia, sposób animacji wypełnienia w mapie magazynu,
- pozwala opisać paletę (teksty widoczne w UI),
- pozwala zdefiniować **grupy profili i kolorów**, które „powinny” znajdować się na danej palecie.

Dzięki temu:
- UI (Android / Web) może wyświetlać **realny poziom wypełnienia**,
- backend może porównywać stan rzeczywisty z konfiguracją,
- można uzyskać efekt wizualny w stylu „tetrisa” – każdy profil jako osobny kolorowy kwadracik.

Plik znajduje się w repozytorium tutaj:
- [backend/src/main/resources/pallet_config.json](file:///f:/VsCodeWorkspace/ApliakcjaAndroidowa/backend/src/main/resources/pallet_config.json)

Na serwerze VPS plik trafia do katalogu:
- `/home/deployer/warehouse/backend/src/main/resources/pallet_config.json`  
po uruchomieniu:

```powershell
.\scripts\deploy_to_vps.ps1
```

---

# 2. Struktura ogólna

Plik ma postać jednego obiektu JSON z tablicą `pallets`:

```json
{
  "pallets": [
    {
      "label": "01A",
      "displayName": "01A",
      "description": "Stojak A, rząd 01 - pełne sztangi",
      "capacity": 70,
      "overflowThresholdPercent": 90,
      "fillAnimation": "vertical",
      "details": {
        "zone": "A",
        "row": 1,
        "type": "FULL_BARS"
      },
      "groups": [
        {
          "name": "Przykład: profil 103.341 białe",
          "profileCodes": ["103341", "103.341"],
          "colorCodes": ["RAL9016", "915205"]
        },
        {
          "name": "Przykład: profil 103.341 dowolny kolor",
          "profileCodes": ["103341", "103.341"],
          "colorCodes": []
        }
      ]
    }
  ]
}
```

Każdy element tablicy `pallets` odpowiada **jednej palecie** w magazynie.

**Bardzo ważne:**  
Pole `label` musi być **identyczne** z wartością `Location.label` w bazie danych (np. `01A`, `07C`). Jeśli w `pallet_config.json` wpiszesz `A-01-01`, a w bazie masz `01A`, konfiguracja dla tej palety **nie zadziała**.

---

# 3. Pola palety – opis szczegółowy

Każda paleta w tablicy `pallets` ma następujące pola:

## 3.1 `label` (wymagane)

- Typ: `String`
- Przykład: `"01A"`, `"07C"`, `"25B"`
- Musi zgadzać się z:
  - `Location.label` w backendzie,
  - tym, co widzisz w **mapie magazynu** (Android/Web).

Jeśli zmienisz etykietę palety w bazie (np. migracja z `01A` na `A-01-01`), musisz:
- zmienić również `label` w `pallet_config.json`,
- zmigrować dane w tabeli `locations` (żeby nazwy dalej były spójne),
- upewnić się, że Android/web używa nowej etykiety.

## 3.2 `displayName`

- Typ: `String`
- Przykład: `"01A"`, `"01A - Okna 82 mm"`
- Tekst wyświetlany w UI:
  - może być identyczny jak `label`,
  - może zawierać bardziej opisową nazwę.

Rekomendacja:
- **Nie zmieniaj** `label`, jeśli chcesz kosmetyczną zmianę nazwy – zmień `displayName`.

## 3.3 `description`

- Typ: `String`
- Przykład: `"Stojak A, rząd 01 - pełne sztangi"`, `"Paleta odpadów – profil 103.341"`
- Służy do wyświetlania w widokach szczegółów / tooltipach.

Możesz tu opisać:
- typ towaru (np. „okna 82 mm”),
- specjalne przeznaczenie (np. „tylko profile szare”),
- informacje dla operatora („nie odkładać odpadów powyżej 3 m”).

## 3.4 `capacity`

- Typ: `Number` (int)
- Przykład: `70`
- Interpretacja: **ile sztuk** (profili) powinna przyjąć paleta przy „100%” wypełnienia.

Zastosowanie:
- backend może użyć tej wartości do policzenia:
  - procentowego wypełnienia (`itemCount / capacity`),
  - ostrzeżeń o przepełnieniu.

Jeżeli nie ustawisz `capacity` w `pallet_config.json`, backend może użyć:
- domyślnej wartości z `warehouse_config.json` (`defaultPalletCapacity`),
- lub wartości z `Location.capacity` w bazie.

Rekomendacja:
- dla precyzyjnej kontroli **zawsze ustawiaj** `capacity` w `pallet_config.json`.

## 3.5 `overflowThresholdPercent`

- Typ: `Number` (int)
- Przykład: `90`
- Próg (w %) wypełnienia, przy którym paleta traktowana jest jako „prawie pełna / przepełniona”.

Przykład:
- `capacity = 70`,
- `overflowThresholdPercent = 90`,
- pojemność „bezpieczna” = `70 * 0.9 = 63 sztuk`.

Zastosowanie w UI:
- jeśli `itemCount >= 63`, można:
  - pokolorować paletę na żółto/czerwono,
  - wyświetlić alert („zostało mniej niż 10% miejsca”).

## 3.6 `fillAnimation`

- Typ: `String`
- Przykładowe wartości:
  - `"vertical"` – pasek/kolumna rosnąca w pionie,
  - w przyszłości: `"grid"`, `"pulse"`, itp. – do sterowania stylem wizualizacji.

To pole jest czysto „wizualne” – steruje tym, jak paleta jest narysowana w mapie magazynu.

## 3.7 `details`

- Typ: obiekt JSON (słownik metadanych).
- Przykład:

  ```json
  "details": {
    "zone": "A",
    "row": 1,
    "type": "FULL_BARS"
  }
  ```

Domyślny schemat:
- `zone`: `"A"`, `"B"` lub `"C"` – litera stojaka,
- `row`: numer rzędu (1–25),
- `type`: tekst opisowy:
  - `"FULL_BARS"` – pełne sztangi,
  - `"WASTE"` – odpady,
  - można dodać inne, np. `"MIXED"`, `"RESERVED"`.

Nie ma sztywnego schematu – możesz dodać więcej pól (np. `category`, `workCenter`, `safetyNote`).  
Ważne jest, by klienci (Android/web) **wiedzieli, czego się spodziewać** i uwzględnili to w kodzie.

## 3.8 `groups`

- Typ: tablica obiektów.
- Służy do definiowania **logicznych grup profili/kolorów** przewidzianych dla tej palety.

Przykład:

```json
"groups": [
  {
    "name": "Okna 103.341 białe",
    "profileCodes": ["103341", "103.341"],
    "colorCodes": ["RAL9016", "915205"]
  },
  {
    "name": "Okna 103.341 dowolny kolor",
    "profileCodes": ["103341", "103.341"],
    "colorCodes": []
  }
]
```

Każda grupa ma pola:

### `name`

- Typ: `String`
- Wyłącznie opis dla człowieka – pojawia się w UI (np. w szczegółach palety).

### `profileCodes`

- Typ: `String[]` (lista kodów profilu)
- Przykład: `["103341", "103.341"]`
- Powinny odpowiadać polu `code` z:
  - `initial_data/profiles.json`,
  - tabeli `ProfileDefinition` w bazie.

Możesz użyć:
- jednego kodu – grupa dotyczy jednego profilu,
- kilku kodów – ta sama grupa opisuje kilka technicznie równoważnych profili (np. różne nazewnictwo).

### `colorCodes`

- Typ: `String[]` (lista kodów koloru)
- Przykład: `["RAL9016", "915205"]`
- Powinny odpowiadać polu `code` z:
  - `initial_data/colors.json`,
  - tabeli `ColorDefinition` w bazie.

**Szczególny przypadek:**  
`"colorCodes": []` – **dowolny kolor**.  
Ta grupa mówi: „profil z `profileCodes`, ale kolor nie ma znaczenia”.

---

# 4. Typowe kombinacje i przykłady

## 4.1 Profil + konkretne kolory (ściśle określona paleta)

Przykład: paleta `01A` ma trzymać tylko profil `103.341` w kolorze białym lub kremowym.

```json
"groups": [
  {
    "name": "Profil 103.341 białe/kremowe",
    "profileCodes": ["103341"],
    "colorCodes": ["RAL9016", "915205"]
  }
]
```

Zastosowanie:
- łatwo wykryć, że na palecie pojawił się „nieprawidłowy” kolor (np. bazalt).

## 4.2 Profil + dowolny kolor

```json
"groups": [
  {
    "name": "Profil 103.341 dowolny kolor",
    "profileCodes": ["103341"],
    "colorCodes": []
  }
]
```

Interpretacja:
- liczy się tylko kod profilu,
- kolor nie jest ograniczeniem.

Możliwe użycie:
- paleta dedykowana jednemu profilowi, ale nie chcesz mnożyć palet per kolor.

## 4.3 Wiele profili, ten sam zestaw kolorów

Przykład: na palecie trzymasz profile 103.341 i 101.290, ale tylko w „szarościach”:

```json
"groups": [
  {
    "name": "Profile okienne szare",
    "profileCodes": ["103341", "101290"],
    "colorCodes": ["701605", "703805", "701205"]
  }
]
```

## 4.4 Paleta „mieszana” – kilka różnych grup

```json
"groups": [
  {
    "name": "Okna 82 – białe",
    "profileCodes": ["103341", "103342"],
    "colorCodes": ["RAL9016"]
  },
  {
    "name": "Okna 82 – drewnopodobne",
    "profileCodes": ["103341", "103342"],
    "colorCodes": ["2052089", "2178001", "3069041"]
  },
  {
    "name": "Okna 70 – dowolny kolor",
    "profileCodes": ["101290"],
    "colorCodes": []
  }
]
```

Interpretacja:
- część palety zarezerwowana dla 82-ki białej,
- część dla 82-ki drewnopodobnej,
- reszta dla 70-ki w dowolnym kolorze.

## 4.5 Paleta odpadów

Dla palet typu `WASTE` (np. `01C`, `02C`), często nie ma sensu definiować grup profili/kolorów:

```json
{
  "label": "01C",
  "displayName": "01C",
  "description": "Stojak C, rząd 01 - odpady",
  "capacity": 200,
  "overflowThresholdPercent": 80,
  "fillAnimation": "vertical",
  "details": {
    "zone": "C",
    "row": 1,
    "type": "WASTE"
  },
  "groups": []
}
```

Wtedy logika może traktować tę paletę jako „cokolwiek, byle odpad”, ale nadal masz:
- pojemność,
- próg przepełnienia,
- opis i metadane do wyświetlania w UI.

---

# 5. Typowe problemy i pułapki

## 5.1 `label` nie zgadza się z bazą

Objawy:
- paleta w mapie magazynu nie ma opisów z `pallet_config`,
- pojemność pozostaje domyślna,
- grupy profili wydają się ignorowane.

Najczęstsza przyczyna:
- w bazie jest `01A`, a w `pallet_config.json` wpisałeś `A-01-01`,
- lub odwrotnie.

Rozwiązanie:
- sprawdź tabelę `locations` (kolumna `label`) albo widok „mapa magazynu”,
- upewnij się, że `label` w `pallet_config.json` jest **identyczny**.

## 5.2 Pomyłka w kodzie profilu / koloru

Objawy:
- tetris/mapa pokazuje „nieznane” lub pusty profil/kolor,
- backend nie rozpoznaje dopasowania do grupy.

Przyczyny:
- literówka w `profileCodes` (np. `"103341 "` ze spacją),
- literówka w `colorCodes` (np. `7016O5` zamiast `701605`),
- użycie nazwy zamiast kodu (np. `"Złoty Dąb"` zamiast `"2178001"`).

Rozwiązanie:
- opieraj się na plikach źródłowych:
  - [backend/src/main/resources/initial_data/profiles.json](file:///f:/VsCodeWorkspace/ApliakcjaAndroidowa/backend/src/main/resources/initial_data/profiles.json)
  - [backend/src/main/resources/initial_data/colors.json](file:///f:/VsCodeWorkspace/ApliakcjaAndroidowa/backend/src/main/resources/initial_data/colors.json)
- kopiuj kody (bez spacji, dokładnie).

## 5.3 Sprzeczne grupy dla tej samej palety

Przykład problematyczny:

```json
"groups": [
  {
    "name": "Profil 103.341 białe",
    "profileCodes": ["103341"],
    "colorCodes": ["RAL9016"]
  },
  {
    "name": "Profil 103.341 dowolny kolor",
    "profileCodes": ["103341"],
    "colorCodes": []
  }
]
```

Interpretacja:
- druga grupa „zjada” pierwszą (bo mówi „dowolny kolor” dla tego samego profilu).

To **nie jest błąd techniczny**, ale logiczny:
- backend/UX może mieć trudność, jak policzyć „zgodność z planem”.

Rekomendacja:
- jeśli potrzebujesz rozróżnienia, grupy powinny być rozłączne:
  - np. w pierwszej grupie tylko konkretny zbiór kolorów,
  - w drugiej grupa „reszta kolorów” (jeśli taką logikę wprowadzisz).

## 5.4 Zbyt duży `capacity` vs. rzeczywistość

Jeśli wpiszesz:
- `capacity = 500`, a fizycznie paleta mieści ~70 sztuk,
- UI może pokazywać niskie wypełnienie („14%”) mimo, że paleta jest pełna,
- alerty o przepełnieniu się nie pojawią.

Rekomendacja:
- ustawiaj `capacity` realistycznie,
- jeśli rodzaj profilu ma większą objętość (np. pakiety), możesz dać mniejszą pojemność.

## 5.5 Zmiany w pliku bez redeployu backendu

Objawy:
- edytujesz `pallet_config.json` w repo,
- na VPS dalej widzisz starą konfigurację.

Przyczyna:
- backend na VPS nie został przebudowany/uruchomiony z nowym plikiem.

Rozwiązanie:

Z Windows/VS Code:

```powershell
.\scripts\deploy_to_vps.ps1
```

Skrypt:
- pakuje aktualny katalog `backend`,
- wysyła na VPS,
- uruchamia tam `docker-compose` z `docker-compose.prod.yml` (buduje nowe obrazy),
- restartuje kontener backendu.

Po tym `pallet_config.json` na serwerze powinien odpowiadać wersji z repozytorium.

---

# 6. Dobre praktyki edycji

- **Zawsze** pilnuj poprawnego JSON:
  - brak przecinków na końcu ostatniego elementu,
  - cudzysłowy `"` wokół nazw pól i stringów,
  - wartości liczbowe bez cudzysłowów (`70`, nie `"70"`).
- Zmiany rób stopniowo:
  - najpierw jedna paleta (np. `01A`),
  - deploy na VPS,
  - test w mapie magazynu,
  - dopiero potem kopiuj wzór na kolejne rzędy.
- Utrzymuj spójność nazewnictwa:
  - `zone` = litera stojaka (`A`/`B`/`C`),
  - `row` = numer z etykiety (`01` → `1`, `10` → `10`),
  - `type` = jedna z kilku wartości (`FULL_BARS`, `WASTE`, ewentualnie nowe).
- Dokumentuj decyzje:
  - jeśli któraś paleta ma nietypowe przeznaczenie, dopisz to w `description`,
  - łatwiej będzie potem zrozumieć, czemu jest skonfigurowana inaczej.

---

# 7. Szybki checklist przed deployem

1. Czy `label` każdej palety zgadza się z tym, co widzisz w mapie (`01A`, `07B`, `25C`)?  
2. Czy `capacity` jest realistyczne i spójne z `warehouse_config.json`?  
3. Czy `overflowThresholdPercent` ma sens (np. 80–95, a nie 0 albo 150)?  
4. Czy `profileCodes` i `colorCodes` są zgodne z `profiles.json` / `colors.json`?  
5. Czy JSON jest poprawny (można użyć dowolnego validatora online)?  
6. Czy po zmianach wykonałeś:

   ```powershell
   .\scripts\deploy_to_vps.ps1
   ```

7. Czy po deployu sprawdziłeś mapę magazynu (Android/Web), czy:
   - opisy i pojemności są zgodne,
   - alerty pojawiają się, gdy paleta jest blisko pełna?

Jeśli wszystkie odpowiedzi są „tak”, konfiguracja `pallet_config.json` powinna działać stabilnie i być gotowa pod dalszą integrację z wizualizacją „tetrisa”. 

