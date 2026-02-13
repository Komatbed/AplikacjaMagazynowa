
import sys
import os
import json
from unittest.mock import patch
import builtins

# Add path to config_wizard
sys.path.append(os.path.join(os.getcwd(), 'plikikonfiguracyjne'))

import config_wizard

def test_wizard():
    print("Running Config Wizard Test...")
    
    # Define inputs for the linear flow
    inputs = [
        "n", # Add profiles?
        "n", # Add colors?
        "t", # Warehouse config?
        "10", # Threshold
        "100", # Capacity
        "1000, 2000", # Waste lengths
        "ColorA, ColorB, X/Y", # Custom colors
        "n", # Core map?
    ]
    
    input_iter = iter(inputs)
    
    def mock_input(prompt=""):
        try:
            val = next(input_iter)
            print(f"[MockInput] {prompt} -> {val}")
            return val
        except StopIteration:
            return "" # Default fallback
            
    # Mock input
    with patch('builtins.input', side_effect=mock_input):
        config_wizard.main()
        
    # Verify file
    config_path = os.path.join(os.getcwd(), "backend", "src", "main", "resources", "warehouse_config.json")
    if os.path.exists(config_path):
        with open(config_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
            print("\nGenerated Config:")
            print(json.dumps(data, indent=2, ensure_ascii=False))
            
            if "customMultiCoreColors" in data:
                colors = data["customMultiCoreColors"]
                if "ColorA" in colors and "X/Y" in colors:
                    print("\nSUCCESS: Custom colors found in config!")
                else:
                    print("\nFAILURE: Custom colors missing or incorrect.")
            else:
                print("\nFAILURE: customMultiCoreColors field missing.")
    else:
        print(f"\nFAILURE: Config file not found at {config_path}")

if __name__ == "__main__":
    test_wizard()
