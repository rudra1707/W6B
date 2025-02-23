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
