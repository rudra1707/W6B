// FILE: TestClient.java
// PROJECT: UDP Logging System
// PROGRAMMER: VIRAJSINH SOLANKI (8981864) & RUDRA NITESHKUMAR BHATT(8980507)
// FIRST VERSION: 2025-02-25
// DESCRIPTION:
// This client sends log messages to a UDP server. It reads server details from 
// a config file, validates input, supports retrying, and logs locally.

// Reference for UDP Socket Communication: 
// https://docs.oracle.com/javase/tutorial/networking/datagrams/clientServer.html

// Reference for File Handling: 
// https://www.geeksforgeeks.org/filewriter-class-in-java/

// Reference for Reading Files with Scanner: 
// https://www.javatpoint.com/how-to-read-file-line-by-line-in-java

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

public class TestClient {
    private static String serverIP;
    private static int serverPort;
    private static final String logFile = "client_logs.txt";
    private static final int MaxRetries = 3;

    // Log Level Constants
    private static final String DEBUG = "DEBUG";
    private static final String INFO = "INFO";
    private static final String WARN = "WARN";
    private static final String ERROR = "ERROR";
    private static final String FATAL = "FATAL";


// FUNCTION: ValidIP
    // DESCRIPTION:
    // Checks if an IP address is in correct format.
    // PARAMETERS:
    // ip - The IP address to check.
    // RETURNS:
    // boolean - True if valid, False otherwise.
private static boolean ValidIP(String ip) {
    String pattern = "^\\d{1,3}(\\.\\d{1,3}){3}$";
    return Pattern.matches(pattern, ip);
}

// FUNCTION: ValidPort
    // DESCRIPTION:
    // Checks if a port number is valid.
    // PARAMETERS:
    // port - The port number to check.
    // RETURNS:
    // boolean - True if valid, False otherwise.
private static boolean ValidPort(int port) {
    return port >= 1024 && port <= 65535;
}

// FUNCTION: LoadConfig
    // DESCRIPTION:
    // Reads server IP and port from a config file.
    // PARAMETERS:
    // configFile - Path to the config file.
    // RETURNS:
    // None
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

 // FUNCTION: sendLog
    // DESCRIPTION:
    // Sends a log message to the UDP server with retry.
    // PARAMETERS:
    // level - Log level.
    // message - Log message.
    // requestId - Unique request ID.
    // RETURNS:
    // None

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
            break; 
        } catch (IOException e) {
            System.out.println("Attempt " + attempt + " failed: " + e.getMessage());
            if (attempt == MaxRetries) {
                System.out.println("Failed to send log after " + MaxRetries + " attempts.");
            }
        }
    }
}


    
// FUNCTION: logToFile
    // DESCRIPTION:
    // Saves the log entry to a local file.
    // PARAMETERS:
    // logEntry - The log message to save.
    // RETURNS:
    // None
    private static void logToFile(String logEntry) {
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(logEntry + "\n");
        } catch (IOException e) {
            System.out.println("Error writing to client log file: " + e.getMessage());
        }
    }

// FUNCTION: manualTest
    // DESCRIPTION:
    // Allows user to enter log messages manually.
    // PARAMETERS:
    // None
    // RETURNS:
    // None
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

 // FUNCTION: automatedTest
    // DESCRIPTION:
    // Sends multiple logs automatically.
    // PARAMETERS:
    // None
    // RETURNS:
    // None

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
            Thread.currentThread().interrupt(); 
            System.out.println("Sleep interrupted: " + e.getMessage());
        }
    }


     // FUNCTION: main
    // DESCRIPTION:
    // Starts the client and selects test type.
    // PARAMETERS:
    // args String[] - Command line arguments.
    // RETURNS:
    // None
    public static void main(String[] args) {
        LoadConfig("config.txt");

        Scanner scanner = new Scanner(System.in);
        System.out.print("Select test type: (1) Manual (2) Automated: ");
        int choice = scanner.nextInt();
        scanner.nextLine(); 

        if (choice == 1) {
            manualTest();
        } else {
            automatedTest();
        }

        scanner.close();
    }
}

