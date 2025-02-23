import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

public class TestClient {
    private static String serverIP;
    private static int serverPort;
    private static final String LOG_FILE = "client_logs.txt";

// Function to validate IP address format
private static boolean isValidIP(String ip) {
    String pattern = "^\\d{1,3}(\\.\\d{1,3}){3}$";
    return Pattern.matches(pattern, ip);
}

// Function to validate port number
private static boolean isValidPort(int port) {
    return port >= 1024 && port <= 65535;
}
