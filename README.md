# System Magazynowy PVC (Warehouse Management System)

Kompletne rozwiązanie do zarządzania magazynem profili, integrujące aplikację mobilną, backend, bazę danych oraz moduł sztucznej inteligencji.

## Struktura Projektu

*   `/app` - Aplikacja mobilna Android (Kotlin, Jetpack Compose).
*   `/backend` - Serwer API (Spring Boot, Kotlin).
*   `/ai-service` - Moduł inteligencji i optymalizacji (Python, FastAPI).
*   `/docs` - Dokumentacja techniczna i instrukcje.

## Dokumentacja

Szczegółowe informacje znajdują się w katalogu `docs/`:

1.  [Instrukcja Instalacji (INSTALL.md)](docs/INSTALL.md) - Jak uruchomić system krok po kroku.
2.  [Status Techniczny (TECHNICAL_STATUS.md)](docs/TECHNICAL_STATUS.md) - Opis funkcji i architektury.
3.  [Ankieta Konfiguracyjna (CONFIGURATION_QUESTIONNAIRE.md)](docs/CONFIGURATION_QUESTIONNAIRE.md) - Pytania pomocne we wdrożeniu.

## Szybki Start

```bash
docker-compose up --build
```
Dostępne usługi:
*   Backend: http://localhost:8080
*   AI Service: http://localhost:8000
