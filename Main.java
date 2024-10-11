import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

enum LogLevel {
    INFO,
    WARNING,
    ERROR
}

class Logger {
    private static Logger instance;
    private static final ReentrantLock lock = new ReentrantLock();
    private LogLevel logLevel;
    private String logFilePath;

    private Logger() {
        logLevel = LogLevel.INFO;
        logFilePath = "log.txt";
    }

    public static Logger getInstance() {
        if (instance == null) {
            lock.lock();
            try {
                if (instance == null) {
                    instance = new Logger();
                }
            } finally {
                lock.unlock();
            }
        }
        return instance;
    }

    public void setLogLevel(LogLevel level) {
        this.logLevel = level;
    }

    public void log(String message, LogLevel level) {
        if (level.ordinal() >= logLevel.ordinal()) {
            try {
                saveToFile(formatMessage(message, level));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String formatMessage(String message, LogLevel level) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        return "[" + timestamp + "] [" + level + "] " + message;
    }

    private void saveToFile(String message) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
            writer.write(message);
            writer.newLine();
        }
    }

    public void loadConfig(String configFilePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(configFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    if ("logLevel".equals(parts[0])) {
                        this.logLevel = LogLevel.valueOf(parts[1].toUpperCase());
                    } else if ("logFilePath".equals(parts[0])) {
                        this.logFilePath = parts[1];
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class LogReader {
    public static void readLogs(String logFilePath, LogLevel filterLevel) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("[" + filterLevel + "]")) {
                    System.out.println(line);
                }
            }
        }
    }
}

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        Logger logger = Logger.getInstance();
        logger.loadConfig("loggerConfig.txt");

        Thread t1 = new Thread(() -> {
            Logger log = Logger.getInstance();
            log.log("Message from Thread 1", LogLevel.INFO);
        });

        Thread t2 = new Thread(() -> {
            Logger log = Logger.getInstance();
            log.log("Warning from Thread 2", LogLevel.WARNING);
        });

        Thread t3 = new Thread(() -> {
            Logger log = Logger.getInstance();
            log.log("Error from Thread 3", LogLevel.ERROR);
        });

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        LogReader.readLogs("log.txt", LogLevel.ERROR);
    }
}
