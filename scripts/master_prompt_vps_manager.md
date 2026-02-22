MASTER PROMPT DLA APLIKACJI NA VPS (SYSTEMD + NGINX + VPS_MANAGER)

Ten prompt wklejaj do AI (np. przy generowaniu nowej aplikacji), podmieniając nazwy ról/portów/technologii według potrzeb.

---
(wszystkie wartości są przykładowe i należy je zastąpić własnymi)
1. KONTEKST SYSTEMU

- Aplikacja działa na VPS z Ubuntu, bez Dockera.
- Uruchamianie odbywa się przez:
  - systemd (usługa .service),
  - reverse proxy Nginx,
  - moduł vps_manager, który:
    - czyta pliki YAML z konfiguracją roli,
    - generuje jednostkę systemd,
    - pozwala zarządzać aplikacjami przez bota Telegram (status, logi, restart, healthcheck).

Załóż następujące przykładowe wartości (możesz je zmieniać przy kolejnych użyciach promptu):
- nazwa roli: Aplikacja_tradingowa_vps_companion
- alias roli: trading
- port HTTP aplikacji (APP_PORT): 8101
- użytkownik systemowy roli: app_trading
- katalog roboczy aplikacji na VPS: /srv/app_trading

Technologia aplikacji: PYTHON lub JAVA (wybierz jedną i trzymaj się jej konsekwentnie).

---

2. WYMAGANIA DLA APLIKACJI

2.1. Port i nasłuch
- Aplikacja musi nasłuchiwać na porcie HTTP podanym w zmiennej środowiskowej APP_PORT.
- Jeśli APP_PORT nie jest ustawiony, przyjmij domyślnie 8000 (ale w praktyce będzie ustawiony).
- Nasłuch ma być na 127.0.0.1:APP_PORT (tylko interfejs lokalny).

2.2. Endpoint healthcheck
- Aplikacja musi udostępniać endpoint:
  - GET /health
  - Zwracający JSON z kluczem "status": "ok" oraz kod HTTP 200 przy poprawnym działaniu.
- Przy błędzie zwraca odpowiedni kod HTTP (np. 500) i prostą wiadomość błędu.

2.3. Logowanie
- Wszystkie logi aplikacji są wypisywane na stdout/stderr, jedna linia na zdarzenie.
- Format logów ma być czytelny (czas, poziom, komunikat), tak aby można je wygodnie czytać przez journalctl.

2.4. Konfiguracja przez zmienne środowiskowe
- Nie używaj na sztywno zakodowanych haseł, adresów baz danych ani portów.
- Użyj co najmniej następujących zmiennych środowiskowych:
  - APP_ENV – środowisko (np. dev, prod),
  - APP_PORT – port HTTP, na którym aplikacja nasłuchuje,
  - APP_DB_HOST, APP_DB_PORT, APP_DB_NAME, APP_DB_USER, APP_DB_PASSWORD – parametry połączenia z bazą danych,
  - APP_SECRET_KEY – sekret do sesji/podpisów/szyfrowania,
  - opcjonalnie APP_LOG_LEVEL i inne, jeśli są potrzebne.

2.5. Punkt startowy (entrypoint)

PYTHON:
- Projekt ma moduł app.main, który startuje serwer HTTP na APP_PORT.
- Aplikacja ma dać się uruchomić komendą:
  APP_PORT=8101 APP_ENV=prod ... python -m app.main

JAVA:
- Zbuduj jeden fat-jar (wszystkie zależności w środku).
- Główna klasa startuje serwer HTTP na APP_PORT.
- Aplikacja ma dać się uruchomić komendą:
  APP_PORT=8101 APP_ENV=prod ... java -jar app.jar

2.6. Baza danych
- Aplikacja łączy się z bazą danych wyłącznie na podstawie zmiennych środowiskowych.
- Zakładaj, że baza i użytkownik istnieją; nie musisz ich tworzyć.
- Migracje możesz:
  - wykonać automatycznie przy starcie,
  - albo osobną komendą (np. python -m app.migrate, jar z profilem migrate).

---

3. PLIKI I ŚCIEŻKI WYMAGANE PRZEZ VPS_MANAGER DLA JEDNEJ ROLI

vps_manager oczekuje kilku plików konfiguracyjnych, które muszą być spójne z kodem aplikacji. Poniższe ścieżki dotyczą przykładowej roli Aplikacja_tradingowa_vps_companion.

3.1. Plik roli do listowania i bota (YAML)

Ścieżka na VPS:
  /etc/vps_manager/roles/app_trading.yml

Zastosowanie:
- używany przez bota Telegram do komend:
  /roles, /status, /logs, /restart, /health
- definiuje nazwę roli, alias, nazwę jednostki systemd i użytkownika.

Wygeneruj taki YAML (przykładowe wartości):

name: Aplikacja_tradingowa_vps_companion
alias: trading
systemd_unit: app_trading.service
system_user: app_trading
health_url: "http://127.0.0.1:8101/health"
description: Backend tradingowy

Wymagane pola:
- name – pełna nazwa roli (czytelna dla człowieka),
- alias – krótka nazwa używana w Telegramie (np. trading, android),
- systemd_unit – nazwa jednostki systemd (np. app_trading.service),
- system_user – użytkownik systemowy (np. app_trading),
- health_url – opcjonalny URL endpointu /health,
- description – opis roli.

3.2. Plik definicji roli do autokonfiguracji (YAML)

Ścieżka na VPS:
  /etc/vps_manager/role_definitions/app_trading_full.yml

Zastosowanie:
- używany przez skrypt vps_manager.role_applier,
- automatycznie:
  - tworzy użytkownika systemowego,
  - tworzy katalog roboczy,
  - instaluje pakiety apt,
  - generuje i włącza jednostkę systemd.

Wygeneruj następujący YAML (skrypt role_applier.py potrafi automatycznie uzupełnić
brakujące working_dir, env_file i port, ale w promptcie pokaż je jawnie, żeby było
jasne, gdzie mają trafić pliki):

name: Aplikacja_tradingowa_vps_companion
system_user: app_trading
service_name: app_trading
working_dir: /srv/app_trading
exec_start: "/srv/app_trading/venv/bin/python -m app.main"
env_file: "/etc/app_trading.env"
apt_packages:
  - python3
  - python3-venv
  - python3-pip
description: Backend tradingowy

Wymagane pola:
- name – nazwa roli,
- system_user – użytkownik systemowy,
- service_name – nazwa usługi systemd (plik będzie app_trading.service),
- working_dir – katalog roboczy aplikacji (cd przed startem, domyślnie /srv/<system_user>),
- exec_start – pełna komenda startowa (ściśle zgodna z kodem aplikacji),
- env_file – ścieżka do pliku ze zmiennymi środowiskowymi roli (domyślnie /etc/<system_user>.env),
- apt_packages – lista pakietów apt, które trzeba doinstalować,
- description – opis jednostki systemd.

3.3. Plik środowiskowy roli (env_file)

Ścieżka na VPS:
  /etc/app_trading.env

Zastosowanie:
- wczytywany przez systemd jako EnvironmentFile,
- przechowuje wszystkie zmienne wymagane przez aplikację.

Wygeneruj przykładową zawartość:

APP_ENV="prod"
APP_PORT="8101"
APP_DB_HOST="127.0.0.1"
APP_DB_PORT="5432"
APP_DB_NAME="trading_db"
APP_DB_USER="trading_user"
APP_DB_PASSWORD="SUPER_TAJNE_HASLO"
APP_SECRET_KEY="LOSOWY_SEKRET"

3.4. Struktura katalogów i pliki aplikacji

PYTHON – pokaż:
- katalog roboczy: /srv/app_trading
- strukturę:
  /srv/app_trading/app/__init__.py
  /srv/app_trading/app/main.py
  (plus inne pliki według potrzeb)
- zawartość app/main.py uruchamiającą serwer HTTP na APP_PORT i endpoint /health.
- komendę exec_start zgodną z tą strukturą:
  /srv/app_trading/venv/bin/python -m app.main

JAVA – pokaż:
- katalog roboczy: /srv/app_trading
- nazwę zbudowanego jar:
  /srv/app_trading/app.jar
- główną klasę startującą serwer HTTP na APP_PORT i /health.
- komendę exec_start:
  java -jar /srv/app_trading/app.jar

---

4. INSTRUKCJA INTEGRACJI Z VPS_MANAGER (DLA ODPOWIEDZI MODELU)

W odpowiedzi wygeneruj krótką instrukcję krok po kroku, którą użytkownik wykona na VPS:

4.1. Kopiowanie plików na VPS
- Skopiować kod aplikacji do working_dir (np. /srv/app_trading).
- Skopiować plik roli do:
  /etc/vps_manager/roles/app_trading.yml
- Skopiować plik definicji roli do:
  /etc/vps_manager/role_definitions/app_trading_full.yml
- Utworzyć plik środowiskowy:
  /etc/app_trading.env
  z wartościami zmiennych środowiskowych (co najmniej APP_PORT, APP_ENV i dane bazy).

4.2. Uruchomienie role_applier
- Na VPS (jako root lub z sudo) uruchomić:

cd /srv/vps_manager
source .venv/bin/activate
python -m vps_manager.role_applier /etc/vps_manager/role_definitions/app_trading_full.yml

- Następnie sprawdzić status usługi:

systemctl status app_trading.service

4.3. Sprawdzenie działania przez bota Telegram
- Założyć, że bot vps_manager_bot już działa.
- W Telegramie użyć komend:
  /roles
  /status trading
  /logs trading
  /restart trading
  /health trading

Model, odpowiadając na ten prompt, powinien:
- wygenerować pełny kod aplikacji (pliki źródłowe + struktura),
- wygenerować przykładowe treści wszystkich plików konfiguracyjnych opisanych powyżej,
- pokazać dokładne komendy do uruchomienia i integracji z vps_manager.
