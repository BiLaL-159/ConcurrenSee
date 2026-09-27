import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final ExecutorService threadPool;
    private final long delayMs;

    public Server(int poolSize, long delayMs) {
        this.threadPool = Executors.newFixedThreadPool(poolSize);
        this.delayMs = delayMs;
    }

    public void handleClient(Socket clientSocket) {
        try (clientSocket;
             PrintWriter toSocket = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader fromSocket = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            fromSocket.readLine();
            if (delayMs > 0) {
                Thread.sleep(delayMs); // simulated I/O wait
            }
            toSocket.println("Hello from server " + clientSocket.getInetAddress());
        } catch (IOException | InterruptedException ex) {
            System.err.println("Client error: " + ex.getMessage());
        }
    }

    // Usage: java Server [port] [delayMs] [poolSize]
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8010;
        long delayMs = args.length > 1 ? Long.parseLong(args[1]) : 0;
        int poolSize = args.length > 2 ? Integer.parseInt(args[2]) : 10;
        Server server = new Server(poolSize, delayMs);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port
                    + " (delay " + delayMs + " ms, pool " + poolSize + ")");

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();

                    // Use the thread pool to handle the client
                    server.threadPool.execute(() -> server.handleClient(clientSocket));
                } catch (IOException ex) {
                    System.err.println("Accept error: " + ex.getMessage());
                }
            }
        } finally {
            // Shutdown the thread pool when the server exits
            server.threadPool.shutdown();
        }
    }
}
