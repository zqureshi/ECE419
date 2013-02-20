package mazewar.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class MazeServer {

    private static class ServerHandler implements Runnable {
        private Socket socket;

        private ServerHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;

        try {
            int serverPort = 8000;
            serverSocket = new ServerSocket(serverPort);
            System.out.println("Server listening on port: " + serverPort);
        } catch (IOException e) {
            System.err.println("Could not bind to port!");
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Server shutting down.");
            }
        }));

        while(serverSocket.isBound()) {
            new Thread(new ServerHandler(serverSocket.accept())).start();
        }

        serverSocket.close();
    }
}
