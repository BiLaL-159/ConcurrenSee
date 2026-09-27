import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private final long delayMs;

    public Server(long delayMs) {
        this.delayMs = delayMs;
    }

    public void handleClient(Socket clientSocket) {
        try (clientSocket;
             PrintWriter toClient = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader fromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            fromClient.readLine();
            if (delayMs > 0) {
                Thread.sleep(delayMs); // simulated I/O wait
            }
            toClient.println("Hello World from the server");
        } catch (IOException | InterruptedException ex) {
            System.err.println("Client error: " + ex.getMessage());
        }
    }

    public void run(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port: " + port + " (delay " + delayMs + " ms)");
            while (true) {
                try {
                    // One connection at a time, on the main thread
                    handleClient(serverSocket.accept());
                } catch (IOException ex) {
                    System.err.println("Accept error: " + ex.getMessage());
                }
            }
        }
    }

    // Usage: java Server [port] [delayMs]
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8010;
        long delayMs = args.length > 1 ? Long.parseLong(args[1]) : 0;
        new Server(delayMs).run(port);
    }
}
