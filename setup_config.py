import os
import sys

def ask_user(question, default_value=None):
    """Asks the user a question and returns the answer or default value."""
    prompt = f"{question}"
    if default_value:
        prompt += f" [{default_value}]"
    prompt += ": "
    
    answer = input(prompt).strip()
    if not answer and default_value:
        return default_value
    return answer

def main():
    print("=== ASYSTENT KONFIGURACJI SYSTEMU MAGAZYNOWEGO (ROZSZERZONY) ===")
    print("Odpowiedz na pytania, aby dostosować system do specyfiki produkcji PVC.")
    print("Naciśnij ENTER, aby zaakceptować wartość domyślną w nawiasie [].\n")

    config = {}

    # 1. Specyfika Magazynu
    print("--- 1. FIZYCZNA STRUKTURA MAGAZYNU ---")
    config['WAREHOUSE_ROWS'] = ask_user("Ile rzędów regałów posiada magazyn?", "25")
    config['SHELF_CAPACITY'] = ask_user("Jaka jest maksymalna liczba palet na jeden rząd?", "3")
    config['LOCATION_FORMAT'] = ask_user("Jaki jest format lokalizacji (np. RR-P)?", "RR-P")

    # 2. Parametry Materiałowe
    print("\n--- 2. PARAMETRY MATERIAŁOWE (PVC) ---")
    config['STD_PROFILE_LENGTH'] = ask_user("Standardowa długość nowej sztangi (mm)", "6500")
    config['USEFUL_WASTE_LIMIT'] = ask_user("Minimalna długość użytecznego odpadu (mm)", "500")
    config['SCRAP_LIMIT'] = ask_user("Próg złomowania - poniżej tego to śmieć (mm)", "200")

    # 3. Infrastruktura IT
    print("\n--- 3. INFRASTRUKTURA IT ---")
    config['SERVER_IP'] = ask_user("Adres IP Twojego komputera (dla API)", "192.168.1.101")
    config['SERVER_PORT'] = ask_user("Port serwera backendu", "8080")
    config['PRINTER_IP'] = ask_user("Adres IP drukarki Zebra", "192.168.1.200")
    config['PRINTER_PORT'] = ask_user("Port drukarki", "9100")

    # 4. Procesy
    print("\n--- 4. PROCESY BIZNESOWE ---")
    auth_req = ask_user("Czy wymagać logowania pracownika przy każdej akcji? (tak/nie)", "nie")
    config['REQUIRE_AUTH'] = "true" if auth_req.lower() in ['tak', 'y', 'yes', 't'] else "false"
    
    auto_sugg = ask_user("Czy system ma automatycznie sugerować lokalizację odpadu? (tak/nie)", "tak")
    config['AUTO_SUGGEST_LOCATION'] = "true" if auto_sugg.lower() in ['tak', 'y', 'yes', 't'] else "false"

    # 5. Optymalizacja Cięcia
    print("\n--- 5. OPTYMALIZACJA CIĘCIA ---")
    config['OPTIMIZATION_ALGO'] = ask_user("Preferowany algorytm (MAX_YIELD = jak najmniej odpadu, PREFER_WASTE = generuj użyteczne odpady)", "MAX_YIELD")
    reserve_waste = ask_user("Czy włączyć rezerwację specjalnych wymiarów odpadów (np. 1200mm)? (tak/nie)", "nie")
    config['RESERVE_WASTE_ENABLED'] = "true" if reserve_waste.lower() in ['tak', 'y', 'yes', 't'] else "false"
    if config['RESERVE_WASTE_ENABLED'] == "true":
        config['RESERVED_WASTE_LENGTHS'] = ask_user("Podaj długości do zachowania po przecinku (np. 1200,850)", "1200")
    else:
        config['RESERVED_WASTE_LENGTHS'] = ""

    # 6. Kalkulator Szprosów (Muntins)
    print("\n--- 6. KALKULATOR SZPROSÓW (WIEDEŃSKIE) ---")
    config['MUNTIN_GAP_MM'] = ask_user("Domyślny luz na łączeniu szpros-szpros (mm)", "1.0")
    config['BEAD_GAP_MM'] = ask_user("Domyślny luz na łączeniu listwa-szpros (mm)", "1.0")
    config['MUNTIN_TAPE_THICKNESS'] = ask_user("Grubość taśmy klejącej pod szprosem (mm)", "0.5")
    
    # Generowanie pliku
    output_filename = "generated_config.env"
    print(f"\nGenerowanie pliku konfiguracyjnego: {output_filename}...")
    
    try:
        with open(output_filename, "w", encoding='utf-8') as f:
            f.write("# Wygenerowano automatycznie przez setup_config.py\n")
            f.write("# Skopiuj te wartości do odpowiednich plików .properties lub zmiennych środowiskowych\n\n")
            
            f.write("# --- MAGAZYN ---\n")
            f.write(f"WAREHOUSE_ROWS={config['WAREHOUSE_ROWS']}\n")
            f.write(f"SHELF_CAPACITY={config['SHELF_CAPACITY']}\n")
            f.write(f"LOCATION_FORMAT={config['LOCATION_FORMAT']}\n\n")
            
            f.write("# --- MATERIAŁY ---\n")
            f.write(f"STD_PROFILE_LENGTH={config['STD_PROFILE_LENGTH']}\n")
            f.write(f"USEFUL_WASTE_LIMIT={config['USEFUL_WASTE_LIMIT']}\n")
            f.write(f"SCRAP_LIMIT={config['SCRAP_LIMIT']}\n\n")
            
            f.write("# --- SIEC ---\n")
            f.write(f"SERVER_URL=http://{config['SERVER_IP']}:{config['SERVER_PORT']}/api/v1/\n")
            f.write(f"PRINTER_IP={config['PRINTER_IP']}\n")
            f.write(f"PRINTER_PORT={config['PRINTER_PORT']}\n\n")
            
            f.write("# --- LOGIKA ---\n")
            f.write(f"REQUIRE_AUTH={config['REQUIRE_AUTH']}\n")
            f.write(f"AUTO_SUGGEST_LOCATION={config['AUTO_SUGGEST_LOCATION']}\n")
            
            f.write("# --- OPTYMALIZACJA ---\n")
            f.write(f"OPTIMIZATION_ALGO={config['OPTIMIZATION_ALGO']}\n")
            f.write(f"RESERVE_WASTE_ENABLED={config['RESERVE_WASTE_ENABLED']}\n")
            f.write(f"RESERVED_WASTE_LENGTHS={config['RESERVED_WASTE_LENGTHS']}\n")
            
            f.write("# --- SZPROSY ---\n")
            f.write(f"MUNTIN_GAP_MM={config['MUNTIN_GAP_MM']}\n")
            f.write(f"BEAD_GAP_MM={config['BEAD_GAP_MM']}\n")
            f.write(f"MUNTIN_TAPE_THICKNESS={config['MUNTIN_TAPE_THICKNESS']}\n")
            
        print("SUKCES! Plik został utworzony.")
        print(f"Możesz teraz użyć tych wartości w backend/src/main/resources/application.properties")
        print(f"lub w ustawieniach aplikacji mobilnej.")
        
        # Wyświetlenie podglądu
        print("\n--- PODGLĄD PLIKU ---")
        with open(output_filename, "r", encoding='utf-8') as f:
            print(f.read())
            
    except Exception as e:
        print(f"BŁĄD podczas zapisywania pliku: {e}")

if __name__ == "__main__":
    main()
