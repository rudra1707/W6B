import socket
import time
import os
import re

from datetime import datetime

# Function to validate IP address format
def is_valid_ip(ip):
    pattern = r"^\d{1,3}(\.\d{1,3}){3}$"
    return re.match(pattern, ip) is not None

# Function to validate port number
def is_valid_port(port):
    return 1024 <= port <= 65535

# Load configuration from a text file
def load_config(config_file):
    config = {"server_ip": "0.0.0.0", "server_port": 5000}  # Default values

    if os.path.exists(config_file):
        with open(config_file, "r") as f:
            for line in f:
                line = line.strip()
                if line.startswith("server_ip="):
                    config["server_ip"] = line.split("=")[1].strip()
                elif line.startswith("server_port="):
                    try:
                        config["server_port"] = int(line.split("=")[1].strip())
                    except ValueError:
                        print("Invalid port in config file. Using default 5000.")

    if not is_valid_ip(config["server_ip"]):
        print("Invalid IP in config file. Using default 0.0.0.0.")
        config["server_ip"] = "0.0.0.0"

    if not is_valid_port(config["server_port"]):
        print("Invalid port in config file. Using default 5000.")
        config["server_port"] = 5000

    return config
