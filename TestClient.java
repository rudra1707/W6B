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

// Load configuration from a text file
private static void loadConfig(String configFile) {
    File file = new File(configFile);
    if (!file.exists()) {
        System.out.println("Config file not found: " + configFile + ". Using default values.");
        return;
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
                    System.out.println("Invalid port in config file. Using default 5000.");
                    serverPort = 5000;
                }
            }
        }
    } catch (IOException e) {
        System.out.println("Error reading config file: " + e.getMessage());
    }

    if (!isValidIP(serverIP)) {
        System.out.println("Invalid IP in config file. Using default 127.0.0.1.");
        serverIP = "127.0.0.1";
    }
    if (!isValidPort(serverPort)) {
        System.out.println("Invalid port in config file. Using default 5000.");
        serverPort = 5000;
    }
}

    // Send log message
    public static void sendLog(String level, String message) {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress serverAddress = InetAddress.getByName(serverIP);

            // Manually create JSON-like log string
            String logEntry = "{ \"level\": \"" + level + "\", \"message\": \"" + message + "\" }";

            byte[] sendData = logEntry.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
            socket.send(sendPacket);
            socket.close();

            logToFile(logEntry);
            System.out.println("Sent & Logged: " + logEntry);
        } catch (IOException e) {
            System.out.println("Error sending log: " + e.getMessage());
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
