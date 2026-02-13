import json
import os
import sys

def clear_screen():
    os.system('cls' if os.name == 'nt' else 'clear')

def get_input(prompt, default=None, required=True, val_type=str):
    while True:
        p = f"{prompt}"
        if default is not None:
            p += f" [{default}]"
        p += ": "
        
        val = input(p).strip()
        
        if not val and default is not None:
            return default
            
        if not val and required:
            print("Wartość jest wymagana!")
            continue
            
        try:
            return val_type(val)
        except ValueError:
            print(f"Niepoprawny format. Oczekiwano {val_type.__name__}.")

def create_profile():
    print("\n--- Dodawanie Profilu ---")
    return {
        "code": get_input("Kod Profilu (np. 103341)"),
        "description": get_input("Opis", default=""),
        "system": get_input("System (np. Veka Softline 82)", default="Veka"),
        "manufacturer": get_input("Producent", default="Veka"),
        "standardLengthMm": get_input("Standardowa Długość (mm)", default=6500, val_type=int),
        "heightMm": get_input("Wysokość (mm)", default=0, val_type=int, required=False),
        "widthMm": get_input("Szerokość (mm)", default=0, val_type=int, required=False),
        "beadHeightMm": get_input("Wysokość Listwy (mm)", default=0, val_type=int, required=False),
        "beadAngle": get_input("Kąt Listwy", default=0.0, val_type=float, required=False)
    }

def create_color():
    print("\n--- Dodawanie Koloru ---")
    return {
        "code": get_input("Kod Koloru (np. RAL9016, 123456)"),
        "name": get_input("Nazwa (np. Biały, Złoty Dąb)"),
        "description": get_input("Opis", default=""),
        "paletteCode": get_input("Kod z Palety", default=""),
        "vekaCode": get_input("Kod Veka", default=""),
        "type": get_input("Typ (smooth/wood/mat)", default="smooth"),
        "foilManufacturer": get_input("Producent Okleiny", default="")
    }

def create_warehouse_config():
    print("\n--- Konfiguracja Magazynu ---")
    return {
        "lowStockThreshold": get_input("Próg ostrzegania o niskim stanie (szt.)", default=5, val_type=int),
        "defaultPalletCapacity": get_input("Domyślna pojemność palety (szt.)", default=50, val_type=int),
        "reserveWasteLengths": [
            int(x.strip()) for x in get_input("Zastrzeżone długości odpadów (po przecinku, np. 1000, 1500)", default="").split(',') if x.strip()
        ],
        "customMultiCoreColors": [
            x.strip() for x in get_input("Kolory specjalne (wybór rdzenia/RAL9001) (po przecinku, np. Złoty Dąb, Orzech)", default="").split(',') if x.strip()
        ]
    }

def create_core_color_map():
    print("\n--- Mapowanie Kolorów Rdzenia ---")
    print("Format: nazwa_zewnetrzna: kolor_rdzenia")
    print("Przykład: winchester: caramel")
    
    mapping = {}
    while True:
        entry = get_input("Podaj mapowanie (lub Enter aby zakończyć)", required=False)
        if not entry:
            break
            
        if ':' in entry:
            key, val = entry.split(':', 1)
            mapping[key.strip()] = val.strip()
        else:
            print("Błędny format! Użyj dwukropka.")
            
    return mapping

def main():
    # Paths
    base_dir = os.path.dirname(os.path.abspath(__file__))
    initial_data_dir = os.path.join(base_dir, "..", "backend", "src", "main", "resources", "initial_data")
    resources_dir = os.path.join(base_dir, "..", "backend", "src", "main", "resources")
    
    for d in [initial_data_dir, resources_dir]:
        if not os.path.exists(d):
            try:
                os.makedirs(d)
            except OSError:
                print(f"Nie można utworzyć folderu: {d}")

    profiles = []
    colors = []
    warehouse_config = None
    core_color_map = None

    print("Witaj w kreatorze konfiguracji!")
    
    # Profiles Loop
    if get_input("\nCzy chcesz dodać profile? (t/n)", default="t").lower() == 't':
        while True:
            profiles.append(create_profile())
            if get_input("Dodać kolejny profil? (t/n)", default="t").lower() != 't':
                break
    
    # Colors Loop
    if get_input("\nCzy chcesz dodać kolory? (t/n)", default="t").lower() == 't':
        while True:
            colors.append(create_color())
            if get_input("Dodać kolejny kolor? (t/n)", default="t").lower() != 't':
                break

    # Warehouse Config
    if get_input("\nCzy skonfigurować parametry magazynu (progi, pojemności)? (t/n)", default="t").lower() == 't':
        warehouse_config = create_warehouse_config()

    # Core Color Map
    if get_input("\nCzy skonfigurować mapowanie kolorów rdzenia? (t/n)", default="t").lower() == 't':
        core_color_map = create_core_color_map()

    # Save Initial Data (Profiles/Colors)
    if profiles:
        path = os.path.join(initial_data_dir, "profiles.json")
        with open(path, "w", encoding="utf-8") as f:
            json.dump(profiles, f, indent=2, ensure_ascii=False)
        print(f"\nZapisano {len(profiles)} profili do {path}")

    if colors:
        path = os.path.join(initial_data_dir, "colors.json")
        with open(path, "w", encoding="utf-8") as f:
            json.dump(colors, f, indent=2, ensure_ascii=False)
        print(f"\nZapisano {len(colors)} kolorów do {path}")

    # Save Warehouse Config
    if warehouse_config:
        path = os.path.join(resources_dir, "warehouse_config.json")
        with open(path, "w", encoding="utf-8") as f:
            json.dump(warehouse_config, f, indent=2, ensure_ascii=False)
        print(f"\nZapisano konfigurację magazynu do {path}")

    # Save Core Color Map
    if core_color_map:
        path = os.path.join(resources_dir, "core_color_map.json")
        with open(path, "w", encoding="utf-8") as f:
            json.dump(core_color_map, f, indent=2, ensure_ascii=False)
        print(f"\nZapisano mapowanie rdzeni do {path}")

    print("\nGotowe! Uruchom aplikację backendową, aby załadować dane.")

if __name__ == "__main__":
    main()