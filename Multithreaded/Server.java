import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public class Server {
    private final long delayMs;

    public Server(long delayMs) {
        this.delayMs = delayMs;
    }

    public Consumer<Socket> getConsumer() {
        return (clientSocket) -> {
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
        };
    }

    // Usage: java Server [port] [delayMs]
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8010;
        long delayMs = args.length > 1 ? Long.parseLong(args[1]) : 0;
        Server server = new Server(delayMs);
        Consumer<Socket> handler = server.getConsumer();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port + " (delay " + delayMs + " ms)");
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();

                    // Create and start a new thread for each client
                    Thread thread = new Thread(() -> handler.accept(clientSocket));
                    thread.start();
                } catch (IOException ex) {
                    System.err.println("Accept error: " + ex.getMessage());
                }
            }
        }
    }
}
