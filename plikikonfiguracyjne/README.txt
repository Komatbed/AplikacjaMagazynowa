FOLDER KONFIGURACYJNY
=====================

Ten folder zawiera narzędzia do wstępnej konfiguracji aplikacji przed jej uruchomieniem.

ZAWARTOŚĆ:
1. config_wizard.py - Skrypt w języku Python, który pozwala interaktywnie zdefiniować:
   - Profile okienne (kod, system, wymiary)
   - Kolory (kody RAL/Veka, nazwy, typy)
   - Parametry magazynu (próg niskiego stanu, domyślna pojemność palety, zastrzeżone długości odpadów)
   - Mapowanie kolorów rdzenia (np. winchester -> caramel)

   Uruchomienie:
   W terminalu wpisz: python config_wizard.py

2. Wygenerowane pliki:
   - backend/src/main/resources/initial_data/profiles.json (Profile)
   - backend/src/main/resources/initial_data/colors.json (Kolory)
   - backend/src/main/resources/warehouse_config.json (Konfiguracja magazynu)
   - backend/src/main/resources/core_color_map.json (Mapowanie rdzeni)

DZIAŁANIE:
Podczas pierwszego uruchomienia aplikacji backendowej (Spring Boot), system sprawdzi obecność tych plików.
Jeśli tabela w bazie danych jest pusta, dane z plików JSON zostaną automatycznie załadowane.

UWAGA:
Jeśli baza danych już zawiera dane, pliki te zostaną zignorowane, aby nie nadpisać istniejącej konfiguracji.
Aby wymusić ponowne załadowanie, należy wyczyścić odpowiednie tabele w bazie danych.