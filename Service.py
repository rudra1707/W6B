import socket
import time
import os
import re

from datetime import datetime

# Log Level Constants
DEBUG = "DEBUG"
INFO = "INFO"
WARN = "WARN"
ERROR = "ERROR"
FATAL = "FATAL"

# Configurable settings
LOG_FILE = "logs.txt"
RATE_LIMIT = 5  # Max messages per second per client
BUFFER_SIZE = 1024
SERVICE_NAME = "LoggingService"


# Track last log timestamps per client
client_log_times = {}

# Ensure log file exists
try:
    if not os.path.exists(LOG_FILE):
        open(LOG_FILE, "w").close()
except IOError as e:
    print(f"Error creating log file: {e}")
    exit(1)


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

# Load configuration
config = load_config("config.txt")
server_ip = config["server_ip"]
server_port = config["server_port"]

buffer_size = 1024
log_file = "logs.txt"
RATE_LIMIT = 5  # Max messages per second per client

# Track last log timestamps per client
client_log_times = {}

# Ensure log file exists
if not os.path.exists(log_file):
    open(log_file, "w").close()

# Create a UDP socket
UDPServerSocket = socket.socket(family=socket.AF_INET, type=socket.SOCK_DGRAM)
UDPServerSocket.bind((server_ip, server_port))

print(f"UDP Logging Service started on {server_ip}:{server_port}")

def rate_limited(client_address):
    """Check if client is sending logs too fast."""
    now = time.time()
    if client_address in client_log_times:
        if now - client_log_times[client_address] < (1 / RATE_LIMIT):
            return True
    client_log_times[client_address] = now
    return False

while True:
    bytes_address_pair = UDPServerSocket.recvfrom(buffer_size)
    message = bytes_address_pair[0].decode("utf-8")
    client_address = bytes_address_pair[1]

    try:
        log_entry = eval(message)  # Converts string dictionary to actual dictionary
        log_entry["timestamp"] = datetime.now().isoformat()
        log_entry["client_ip"] = client_address[0]

        if rate_limited(client_address[0]):
            print(f"Rate limit exceeded for {client_address[0]}. Dropping log.")
            continue

        # Append log entry to file
        with open(log_file, "a") as f:
            f.write(str(log_entry) + "\n")

        print(f"Logged: {log_entry}")

    except Exception as e:
        print(f"Error processing log from {client_address[0]}: {message} ({e})")
