# FILE: Service.py
# PROJECT: UDP Logging System
# PROGRAMMER: VIRAJSINH SOLANKI (8981864) & RUDRA NITESHKUMAR BHATT(8980507)
# FIRST VERSION: 2025-02-25
# DESCRIPTION:
# This script runs a UDP logging server. It receives log messages, 
# checks their format, limits message rate per client, and saves logs to a file.
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
logFile = "logs.txt"
rateLimit = 5  
bufferSize = 1024
serviceName = "LoggingService"


# Track last log timestamps per client
clientLogTimes = {}

# Ensure log file exists
try:
    if not os.path.exists(logFile):
        open(logFile, "w").close()
except IOError as e:
    print(f"Error creating log file: {e}")
    exit(1)


# FUNCTION: ValidIP
# DESCRIPTION:
# Checks if an IP address is in the correct format.
# PARAMETERS:
# ip (str) - The IP address to check.
# RETURNS:
# bool - True if valid, False otherwise.
def ValidIP(ip):
    pattern = r"^\d{1,3}(\.\d{1,3}){3}$"
    return re.match(pattern, ip) is not None


# FUNCTION: ValidPort
# DESCRIPTION:
# Checks if a port number is valid.
# PARAMETERS:
# port (int) - The port number to check.
# RETURNS:
# bool - True if valid, False otherwise.
def ValidPort(port):
    return 1024 <= port <= 65535


# FUNCTION: LoadConfig
# DESCRIPTION:
# Reads server IP and port from a configuration file.
# PARAMETERS:
# ConfigFile (str) - Path to the config file.
# RETURNS:
# dict - Dictionary with 'serverIP' and 'serverPort'.
def LoadConfig(ConfigFile):
    config = {}

    if os.path.exists(ConfigFile):
        try:
            with open(ConfigFile, "r") as f:
                for line in f:
                    line = line.strip()
                    if line.startswith("serverIP="):
                        config["serverIP"] = line.split("=")[1].strip()
                    elif line.startswith("serverPort="):
                        try:
                            config["serverPort"] = int(line.split("=")[1].strip())
                        except ValueError:
                            print("Invalid port format in config file.")
        except IOError as e:
            print(f"Error reading config file: {e}")
    else:
        print("Config file not found. Please provide a valid 'config.txt' file.")
        exit(1)

    if "serverIP" not in config or not ValidIP(config["serverIP"]):
        print("Invalid or missing server IP in config file. Please provide a valid IP.")
        exit(1)

    if "serverPort" not in config or not ValidPort(config["serverPort"]):
        print("Invalid or missing server port in config file. Please provide a valid port number (1024-65535).")
        exit(1)

    return config




# FUNCTION: rateLimited
# DESCRIPTION:
# Limits the number of logs per second for a client.
# PARAMETERS:
# ClientIP (str) - The IP address of the client.
# RETURNS:
# bool - True if the client exceeded the limit, False otherwise.
def rateLimited(ClientIP):
    now = time.time()
    if ClientIP not in clientLogTimes:
        clientLogTimes[ClientIP] = []

    clientLogTimes[ClientIP] = [
        timestamp for timestamp in clientLogTimes[ClientIP]
        if now - timestamp < 1
    ]

    if len(clientLogTimes[ClientIP]) >= rateLimit:
        return True

    clientLogTimes[ClientIP].append(now)
    return False


# FUNCTION: FormatLog
# DESCRIPTION:
# Formats log messages with timestamp and details.
# PARAMETERS:
# level (str) - Log level.
# ClientIP (str) - Client IP address.
# message (str) - Log message.
# requestID (str) - Unique request ID.
# RETURNS:
# str - Formatted log string.
def FormatLog(level, ClientIP, message, requestID):
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    return f"[{timestamp}] [{level}] [{ClientIP}] [{serviceName}] {message} [{requestID}]"

# Load server configuration
config = LoadConfig("config.txt")
serverIP = config["serverIP"]
serverPort = config["serverPort"]

# Start UDP Server
try:
    UDPServerSocket = socket.socket(family=socket.AF_INET, type=socket.SOCK_DGRAM)
    UDPServerSocket.bind((serverIP, serverPort))
    print(f"UDP Logging Service started on {serverIP}:{serverPort}")
except socket.error as e:
    print(f"Failed to create UDP server socket: {e}")
    exit(1)

# Main server loop with proper error handling
while True:
    try:
        bytes_address_pair = UDPServerSocket.recvfrom(bufferSize)
        message = bytes_address_pair[0].decode("utf-8").strip()
        client_address = bytes_address_pair[1][0]

        if not message:
            print(f"Received empty message from {client_address}")
            continue

        log_parts = message.split("|")
        if len(log_parts) != 3:
            print(f"Invalid log format from {client_address}: {message}")
            continue

        level, log_message, requestID = log_parts

        if level not in [DEBUG, INFO, WARN, ERROR, FATAL]:
            print(f"Invalid log level received from {client_address}: {level}")
            continue

        if rateLimited(client_address):
            print(f"Rate limit exceeded for {client_address}. Dropping log.")
            continue

        logEntry = FormatLog(level, client_address, log_message, requestID)

        try:
            with open(logFile, "a") as f:
                f.write(logEntry + "\n")
            print(f"Logged: {logEntry}")
        except IOError as e:
            print(f"Error writing log entry to file: {e}")

    except socket.error as e:
        print(f"Network error: {e}")
    except Exception as e:
        print(f"Unexpected error occurred: {e}")
 