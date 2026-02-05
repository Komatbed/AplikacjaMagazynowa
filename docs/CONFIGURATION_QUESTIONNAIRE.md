# Ankieta Konfiguracyjna Systemu

Poniższe pytania mają na celu dopasowanie systemu do specyfiki Twojego magazynu. Odpowiedzi posłużą do skonfigurowania plików `.env` oraz tabel w bazie danych.

## 1. Specyfika Fizyczna Magazynu
1.  **Ile rzędów regałów posiada magazyn?**
    *   *Przykład: 10*
2.  **Jaka jest maksymalna pojemność (liczba palet) na jeden rząd?**
    *   *Przykład: 20*
3.  **Jak oznaczane są lokalizacje?**
    *   *Czy format "Rząd-Miejsce" (np. 01-A) jest wystarczający, czy stosujecie inne kody?*

## 2. Parametry Materiałowe (Profile PVC)
1.  **Jaka jest standardowa długość nowej sztangi profilu?**
    *   *Domyślnie: 6500 mm. Czy występują inne standardy (np. 6000 mm)?*
2.  **Jaka jest minimalna długość odpadu, który warto zachować ("Użyteczny Odpad")?**
    *   *Domyślnie: 500 mm. Czy kawałki krótsze są zawsze wyrzucane?*
3.  **Jaki jest próg "Złomu" (Scrap)?**
    *   *Domyślnie: 200 mm. Kawałki poniżej tej wartości są traktowane jako śmieci i nie wracają do systemu.*

## 3. Infrastruktura IT
1.  **Jaki jest adres IP serwera, na którym stanie Docker?**
    *   *Wymagane do konfiguracji aplikacji mobilnych.*
2.  **Czy drukarki etykiet (Zebra) mają stałe adresy IP?**
    *   *Jeśli tak, proszę je wylistować. Jeśli nie, czy obsługują DHCP?*
3.  **Czy sieć WiFi w hali jest stabilna w każdym miejscu?**
    *   *Jeśli nie, priorytetem będzie tryb Offline (już zaimplementowany).*

## 4. Procesy Biznesowe
1.  **Czy wymagana jest autoryzacja każdego pobrania (logowanie pracownika)?**
    *   *Obecnie system jest otwarty. Czy dodać PIN/Kartę RFID?*
2.  **Czy system ma automatycznie sugerować, który odpad wziąć?**
    *   *Czy magazynier ma swobodę wyboru, czy system ma narzucać konkretną lokalizację (Algorytm Best Fit)?*
