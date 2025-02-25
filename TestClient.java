import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

public class TestClient {
    private static String serverIP;
    private static int serverPort;
    private static final String LOG_FILE = "client_logs.txt";
    private static final int MAX_RETRIES = 3;

    // Log Level Constants (All will be used)
    private static final String DEBUG = "DEBUG";
    private static final String INFO = "INFO";
    private static final String WARN = "WARN";
    private static final String ERROR = "ERROR";
    private static final String FATAL = "FATAL";


// Function to validate IP address format
private static boolean isValidIP(String ip) {
    String pattern = "^\\d{1,3}(\\.\\d{1,3}){3}$";
    return Pattern.matches(pattern, ip);
}

// Function to validate port number
private static boolean isValidPort(int port) {
    return port >= 1024 && port <= 65535;
}

// Load configuration from config.txt (no default values)
private static void loadConfig(String configFile) {
    File file = new File(configFile);
    if (!file.exists()) {
        System.out.println("Error: Config file not found. Please provide a valid 'config.txt'.");
        System.exit(1);
    }

    try (Scanner scanner = new Scanner(file)) {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.startsWith("server_ip=")) {
                serverIP = line.split("=")[1].trim();
            } else if (line.startsWith("server_port=")) {
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

    if (serverIP == null || !isValidIP(serverIP)) {
        System.out.println("Error: Invalid or missing server IP in config file.");
        System.exit(1);
    }
    if (serverPort == 0 || !isValidPort(serverPort)) {
        System.out.println("Error: Invalid or missing server port in config file.");
        System.exit(1);
    }
}

 // Send log message with retry mechanism and try-with-resources
 public static void sendLog(String level, String message, String requestId) {
    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
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
            if (attempt == MAX_RETRIES) {
                System.out.println("Failed to send log after " + MAX_RETRIES + " attempts.");
            }
        }
    }
}


        // Log locally to a file
        private static void logToFile(String logEntry) {
            try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
                writer.write(logEntry + "\n");
            } catch (IOException e) {
                System.out.println("Error writing to client log file: " + e.getMessage());
            }
        }
    
        // Get config file from command-line arguments or use default
    public static void main(String[] args) {

        String configFile;
        if (args.length > 0) {
            configFile = args[0];  // Use provided config file path
        } else {
            configFile = "config.txt";  // Default to "config.txt" in the current directory
        }

        // Load config file
        loadConfig(configFile);

        Scanner scanner = new Scanner(System.in);

        System.out.print("Select test type: (1) Manual (2) Automated: ");
        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        if (choice == 1) {
            System.out.println("Manual test mode...");
            while (true) {
                System.out.print("Enter log level (DEBUG, INFO, WARN, ERROR, FATAL) or 'exit': ");
                String level = scanner.nextLine().toUpperCase();

                if (level.equals("EXIT")) break;

                System.out.print("Enter log message: ");
                String message = scanner.nextLine();

                sendLog(level, message);
            }
        } else {
            System.out.println("Automated tests starting...");
            String[] levels = {"DEBUG", "INFO", "WARN", "ERROR", "FATAL"};
            String[] messages = {"Test1", "Test2", "Test3", "Test4", "Test5"};

            try {
                for (int i = 0; i < levels.length; i++) {
                    sendLog(levels[i], messages[i]);
                    Thread.sleep(200);
                }

                System.out.println("Testing rate limit...");
                for (int i = 0; i < 10; i++) {
                    sendLog("WARN", "Rate limit test");
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                System.out.println("Sleep interrupted: " + e.getMessage());
            }
        }

        scanner.close();
    }
}
