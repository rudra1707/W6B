import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

public class TestClient {
    private static String serverIP;
    private static int serverPort;
    private static final String logFile = "client_logs.txt";
    private static final int MaxRetries = 3;

    // Log Level Constants (All will be used)
    private static final String DEBUG = "DEBUG";
    private static final String INFO = "INFO";
    private static final String WARN = "WARN";
    private static final String ERROR = "ERROR";
    private static final String FATAL = "FATAL";


// Function to validate IP address format
private static boolean ValidIP(String ip) {
    String pattern = "^\\d{1,3}(\\.\\d{1,3}){3}$";
    return Pattern.matches(pattern, ip);
}

// Function to validate port number
private static boolean ValidPort(int port) {
    return port >= 1024 && port <= 65535;
}

// Load configuration from config.txt (no default values)
private static void LoadConfig(String configFile) {
    File file = new File(configFile);
    if (!file.exists()) {
        System.out.println("Error: Config file not found. Please provide a valid 'config.txt'.");
        System.exit(1);
    }

    try (Scanner scanner = new Scanner(file)) {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.startsWith("serverIP=")) {
                serverIP = line.split("=")[1].trim();
            } else if (line.startsWith("serverPort=")) {
                try {
                    serverPort = Integer.parseInt(line.split("=")[1].trim());
                } catch (NumberFormatException e) {
                    System.out.println("Error: Invalid port format in config file.");
                    System.exit(1);
                }
            }
        }
    } catch (IOException e) {
        System.out.println("Error reading config file: " + e.getMessage());
        System.exit(1);
    }

    if (serverIP == null || !ValidIP(serverIP)) {
        System.out.println("Error: Invalid or missing server IP in config file.");
        System.exit(1);
    }
    if (serverPort == 0 || !ValidPort(serverPort)) {
        System.out.println("Error: Invalid or missing server port in config file.");
        System.exit(1);
    }
}

 // Send log message with retry mechanism and try-with-resources
 public static void sendLog(String level, String message, String requestId) {
    for (int attempt = 1; attempt <= MaxRetries; attempt++) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName(serverIP);

            String logEntry = level + "|" + message + "|" + requestId;
            byte[] sendData = logEntry.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);

            socket.send(sendPacket);
            logToFile(logEntry);
            System.out.println("Successfully sent log: " + logEntry);
            break; // Exit loop if successful
        } catch (IOException e) {
            System.out.println("Attempt " + attempt + " failed: " + e.getMessage());
            if (attempt == MaxRetries) {
                System.out.println("Failed to send log after " + MaxRetries + " attempts.");
            }
        }
    }
}


       // Log locally to a file
    private static void logToFile(String logEntry) {
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(logEntry + "\n");
        } catch (IOException e) {
            System.out.println("Error writing to client log file: " + e.getMessage());
        }
    }

    // Manual test feature to use all log levels
    private static void manualTest() {
        Scanner scanner = new Scanner(System.in);
        String[] levels = {DEBUG, INFO, WARN, ERROR, FATAL};

        System.out.println("Manual Logging Test Started:");
        while (true) {
            System.out.print("Enter log level (DEBUG, INFO, WARN, ERROR, FATAL) or 'exit': ");
            String level = scanner.nextLine().trim().toUpperCase();

            if (level.equals("EXIT")) break;
            if (!Arrays.asList(levels).contains(level)) {
                System.out.println("Invalid log level. Try again.");
                continue;
            }

            System.out.print("Enter log message: ");
            String message = scanner.nextLine();
            sendLog(level, message, UUID.randomUUID().toString().substring(0, 8)); // Random request ID
        }
        scanner.close();
    }

    // Automated test feature
    private static void automatedTest() {
        System.out.println("Automated tests starting...");
        String[] levels = {DEBUG, INFO, WARN, ERROR, FATAL};
        String[] messages = {"Test1", "Test2", "Test3", "Test4", "Test5"};

        try {
            for (int i = 0; i < levels.length; i++) {
                sendLog(levels[i], messages[i], UUID.randomUUID().toString().substring(0, 8));
                Thread.sleep(200);
            }

            System.out.println("Testing rate limit...");
            for (int i = 0; i < 10; i++) {
                sendLog(WARN, "Rate limit test", UUID.randomUUID().toString().substring(0, 8));
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            System.out.println("Sleep interrupted: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        LoadConfig("config.txt");

        Scanner scanner = new Scanner(System.in);
        System.out.print("Select test type: (1) Manual (2) Automated: ");
        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        if (choice == 1) {
            manualTest();
        } else {
            automatedTest();
        }

        scanner.close();
    }
}

