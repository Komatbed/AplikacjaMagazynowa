🔥 MASTER PROMPT / INSTRUKCJA DLA AI-DEVELOPERA
(do wklejenia w narzędzie AI tworzące kod)
To jest finalna, kompletna specyfikacja zastępująca moduł „szprosy V2”.

🧩 PROJEKT: SZPROSY V3 (REWRITE)
CEL
Całkowicie zastąpić istniejący moduł „szprosy V2”, zachowując jedynie koncepcję:
wirtualnego skrzydła (canvas)
Nowy moduł musi być:
produkcyjny (realne długości cięcia),
montażowy (informacje dla montera),
niezależny od innych modułów aplikacji.

🏗 ARCHITEKTURA
Moduł:
feature_muntins_v3

Struktura:
ui/
canvas/
domain/
geometry/
calculations/
cut_optimizer/
database/


🗄 BAZA DANYCH (NIEZALEŻNA)
Room DB.
PROFILE
id
name
glass_offset_x
glass_offset_y
GLASS_BEAD
id
name
angle_face (10–40°)
effective_glass_offset
MUNTIN
id
name
width
thickness
PROJECT
frame_width
frame_height
profile_id
bead_id
LAYOUT
json_geometry

📐 MODEL GEOMETRYCZNY (KRYTYCZNE)
System NIE operuje na długościach.
Model:
Node
- x
- y

Segment
- startNode
- endNode
- width
- angleStart
- angleEnd

Intersection
- type

Typy przecięć:
BEAD_EDGE
MUNTIN_EDGE
T_JOINT


📏 ZASADA WYMIAROWANIA
Wszystkie wymiary opisują:
OŚ SZPROSA + KIERUNEK POMIARU


🔩 OBLICZENIA (CORE)
1️⃣ Oblicz światło szyby
glassWidth  = frameWidth - 2*(offset + beadOffset)
glassHeight = frameHeight - 2*(offset + beadOffset)


2️⃣ Generuj layout osi.

3️⃣ UWZGLĘDNIJ GRUBOŚĆ SZPROSA
Przecięcia liczone:
EDGE TO EDGE

nie oś–oś.
Grubość fizyczna wpływa na długość.

4️⃣ Długość produkcyjna
Output:
REAL CUT LENGTH

mierzona po najdłuższej płaszczyźnie szprosa.

5️⃣ Luz technologiczny
1 mm na każdy punkt styku


6️⃣ Finalny wzór
L_final =
edge_distance
+ angle_correction_start
+ angle_correction_end
- clearance_total


✂️ LOGIKA KĄTÓW
Każdy koniec:
cut_angle
source:
 - BEAD_FACE
 - MUNTIN_FACE

zakres 10–40°.
Możliwe różne kąty na obu końcach.

🎨 CANVAS (VIRTUAL WINDOW)
Canvas używany wyłącznie do:
edycji layoutu
wizualizacji
Canvas NIE zawiera logiki geometrycznej.

Funkcje:
zoom / pan
drag osi
snapping
grid
auto-center

Warstwy:
skrzydło
światło szyby
szprosy
punkty przecięć
linie pomiarowe

⭐ TRYB NIETYPOWYCH SZPROSÓW
Ukryty pod ikoną obok ustawień.
Domyślnie niewidoczny.

Presety:
romb
krzyż 45°
pajęczyna (promienie z jednego punktu)
łuk
sunburst
gotyk
trapez

🌙 ŁUKI (WAŻNE)
Łuk NIE jest jednym elementem.
Definicja:
segments[]
- axis_length
- axis_angle

Dodatkowo:
predicted_radius

tylko informacyjnie.

📍 MONTAŻ – INSTRUKCJE
System generuje:
punkt startowy klejenia
kolejność montażu

Kolejność:
najdłuższy szpros
dzielące
krzyżowe
dekoracyjne

Monter widzi:
LISTWA S3
DOCIĄĆ: 786 mm
KĄT L: 22°
KĄT P: 15°

PRZYKLEIĆ:
412 mm od lewej
643 mm od dołu


📋 TABELA CIĘĆ
| id | length | angleL | angleP | qty | description |
Automatyczne grupowanie identycznych.

🪚 OPTYMALIZACJA SZTANG
Input:
długość sztangi
kerf piły
do 10 skrzydeł
Zakładamy identyczny layout.
Output:
Cut Plan
+ opis listewek


⚙️ KOREKTY (MUST HAVE)
globalne:
saw_offset_mm

dla skrzydła:
manual_window_correction

kompensacja zewn./wewn.:
top
bottom
left
right compensation


🧠 WSKAZÓWKI MONTAŻOWE (ADVANCED)
Dla presetów nietypowych generować instrukcję:
ROMB:
znajdź środek skrzydła
przyklej pierwszy element od góry

🚫 ZABRONIONE
liczenie „na długościach”
logika w canvasie
pomijanie grubości szprosa
procentowe pozycjonowanie UI

🎯 FINALNY EFEKT
System ma eliminować:
ręczne dopasowywanie długości,
zgadywanie przez montera,
poprawki na budowie.