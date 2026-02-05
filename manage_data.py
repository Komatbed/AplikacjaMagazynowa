import urllib.request
import urllib.parse
import json
import sys

# Default API URL (adjust if needed)
API_URL = "http://localhost:8080/api/v1/config"

def get_input(prompt, default=None, required=True):
    while True:
        p = f"{prompt}"
        if default:
            p += f" [{default}]"
        p += ": "
        val = input(p).strip()
        if not val:
            if default: return default
            if not required: return ""
            print("Wartość wymagana!")
        else:
            return val

def post_data(endpoint, data):
    url = f"{API_URL}/{endpoint}"
    try:
        json_data = json.dumps(data).encode('utf-8')
        req = urllib.request.Request(url, data=json_data, headers={'Content-Type': 'application/json'})
        with urllib.request.urlopen(req) as response:
            if response.status in [200, 201]:
                print(f"✅ Sukces: {endpoint} dodany.")
                return True
            else:
                print(f"❌ Błąd: Server zwrócił status {response.status}")
    except Exception as e:
        print(f"❌ Błąd połączenia: {e}")
        return False

def add_color():
    print("\n--- DODAWANIE KOLORU ---")
    name = get_input("Nazwa koloru (np. Winchester)")
    code = get_input("Kod systemu (np. winchester_1)")
    palette = get_input("Numer z kolornika (np. renolit 49233)")
    veka = get_input("Numer wewnętrzny Veka (np. 123.456)")
    desc = get_input("Opis (opcjonalnie)", required=False)

    data = {
        "code": code,
        "name": name,
        "paletteCode": palette,
        "vekaCode": veka,
        "description": desc
    }
    post_data("colors", data)

def add_profile():
    print("\n--- DODAWANIE PROFILA ---")
    code = get_input("Numer profila (np. 101.200)")
    desc = get_input("Opis (np. Skrzydło okienne)", required=False)
    
    print("--- Wymiary Detalu (do obliczeń) ---")
    height = int(get_input("Wysokość (mm)", "80"))
    width = int(get_input("Szerokość (mm)", "70"))
    bead_h = int(get_input("Wysokość listwy (mm)", "20"))
    bead_a = float(get_input("Kąt listwy (stopnie)", "45.0"))
    std_len = int(get_input("Standardowa długość sztangi (mm)", "6500"))

    data = {
        "code": code,
        "description": desc,
        "heightMm": height,
        "widthMm": width,
        "beadHeightMm": bead_h,
        "beadAngle": bead_a,
        "standardLengthMm": std_len
    }
    post_data("profiles", data)

def main():
    print("=== ASYSTENT DANYCH BAZOWYCH ===")
    print(f"API URL: {API_URL}")
    
    while True:
        print("\n1. Dodaj Kolor")
        print("2. Dodaj Profil")
        print("3. Wyjście")
        
        choice = input("Wybierz opcję: ")
        
        if choice == "1":
            add_color()
        elif choice == "2":
            add_profile()
        elif choice == "3":
            sys.exit(0)

if __name__ == "__main__":
    main()
