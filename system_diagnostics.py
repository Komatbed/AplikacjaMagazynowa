import socket
import sys
import time
import json
import urllib.request
import urllib.error
from urllib.parse import urlencode

# Configuration
POSTGRES_HOST = "localhost"
POSTGRES_PORT = 5433 # External mapping
BACKEND_URL = "http://localhost:8080"
AI_SERVICE_URL = "http://localhost:8000"

def print_header(title):
    print("\n" + "="*50)
    print(f" {title}")
    print("="*50)

def check_port(host, port, name):
    print(f"[*] Checking {name} on {host}:{port}...", end=" ")
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(2)
    result = sock.connect_ex((host, port))
    sock.close()
    if result == 0:
        print("OK")
        return True
    else:
        print("FAILED (Connection Refused/Timeout)")
        return False

def http_get(url, name):
    print(f"[*] Testing API {name} ({url})...", end=" ")
    try:
        with urllib.request.urlopen(url, timeout=5) as response:
            status = response.getcode()
            if 200 <= status < 300:
                print(f"OK (Status: {status})")
                return True
            else:
                print(f"FAILED (Status: {status})")
                return False
    except urllib.error.URLError as e:
        print(f"FAILED ({e})")
        return False
    except Exception as e:
        print(f"ERROR ({e})")
        return False

def http_post(url, data, name):
    print(f"[*] Testing POST {name} ({url})...")
    print(f"    Payload: {json.dumps(data)}")
    try:
        req = urllib.request.Request(url)
        req.add_header('Content-Type', 'application/json; charset=utf-8')
        json_data = json.dumps(data).encode('utf-8')
        req.add_header('Content-Length', len(json_data))
        
        with urllib.request.urlopen(req, json_data, timeout=5) as response:
            status = response.getcode()
            resp_body = response.read().decode('utf-8')
            print(f"    Result: OK (Status: {status})")
            print(f"    Response: {resp_body[:100]}...") # Truncate
            return True
    except urllib.error.HTTPError as e:
        print(f"    Result: FAILED (Status: {e.code})")
        print(f"    Reason: {e.read().decode('utf-8')}")
        return False
    except Exception as e:
        print(f"    Result: ERROR ({e})")
        return False

def run_diagnostics():
    print_header("SYSTEM DIAGNOSTICS & SIMULATION")
    
    # 1. Infrastructure Checks
    print_header("1. Infrastructure Port Checks")
    pg_ok = check_port(POSTGRES_HOST, POSTGRES_PORT, "PostgreSQL")
    backend_ok = check_port("localhost", 8080, "Backend (Spring Boot)")
    ai_ok = check_port("localhost", 8000, "AI Service")

    if not (pg_ok and backend_ok and ai_ok):
        print("\n[CRITICAL] Some services are not running. Attempting simulation might fail.")
        # We continue anyway to see what works
    
    # 2. Service Health Checks
    print_header("2. Service Health Checks")
    if backend_ok:
        # Backend doesn't have a root endpoint, usually /api/v1/inventory/items is a good check
        # But we can try a simple GET to see if it responds (even 404 is a response from server)
        # Let's try to list items
        http_get(f"{BACKEND_URL}/api/v1/inventory/items", "Get Inventory Items")
    
    if ai_ok:
        http_get(f"{AI_SERVICE_URL}/", "AI Service Root")

    # 3. Functional Simulation (End-to-End)
    print_header("3. End-to-End Simulation")
    
    if backend_ok:
        # Step A: Register Waste (Simulating Camera/Manual Input)
        waste_req = {
            "locationLabel": "01A",
            "profileCode": "TEST-PROFILE-X",
            "lengthMm": 1500,
            "quantity": 5,
            "color": "White" # Backend doesn't strictly validate this yet in DTO but handles it
        }
        http_post(f"{BACKEND_URL}/api/v1/inventory/waste", waste_req, "Register Waste")
        
        # Step B: Take Item (Simulating Manual Take)
        take_req = {
            "locationLabel": "01A",
            "profileCode": "TEST-PROFILE-X",
            "lengthMm": 1500,
            "quantity": 1,
            "reason": "SIMULATION_TEST",
            "force": False
        }
        http_post(f"{BACKEND_URL}/api/v1/inventory/take", take_req, "Take Item")

    if ai_ok:
        # Step C: AI Recommendation
        ai_req = {
            "profile_code": "TEST-PROFILE-X",
            "color": "White",
            "required_length_mm": 1200,
            "available_waste": [
                {"id": "test-1", "location": "01A", "length_mm": 1500, "profile_code": "TEST-PROFILE-X", "color": "White"}
            ]
        }
        http_post(f"{AI_SERVICE_URL}/recommend/waste", ai_req, "AI Recommendation")

    print_header("DIAGNOSTICS COMPLETE")

if __name__ == "__main__":
    run_diagnostics()
