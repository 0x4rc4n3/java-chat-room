package Chatapplication;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import javax.swing.text.*;

public class ChatServer {
    private static final int PORT = 9865; 
    private static Set<PrintWriter> clientWriters = new HashSet<>();
    private static JTextPane chatPane;
    private static JFrame frame;
    private static String serverName;

    public static void main(String[] args) {
        try {
            // Ask for server IP and name
            String serverIP = JOptionPane.showInputDialog("Enter server IP (default: localhost):");
            if (serverIP == null || serverIP.isEmpty()) serverIP = "localhost";
            
            serverName = JOptionPane.showInputDialog("Enter server name:");
            if (serverName == null || serverName.isEmpty()) serverName = "Chat Server";
            
            System.out.println("Chat server starting on " + serverIP + ":" + PORT);
            setupServerGUI();
            
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                appendLog("[*] Server started on " + serverIP + ":" + PORT);
                
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    appendLog("[*] New client connected: " + clientSocket.getInetAddress());
                    new ClientHandler(clientSocket).start();
                }
            }
        } catch (IOException e) {
            appendLog("[*] Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void setupServerGUI() {
        frame = new JFrame(serverName + " - Logs");
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(Color.BLACK);
        chatPane.setForeground(Color.WHITE);
        chatPane.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(chatPane);

        frame.add(scrollPane);
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                username = in.readLine();
                broadcastMessage("[*] " + username + " has joined the chat.");
                appendLog("[*] " + username + " has joined the chat.");

                String message;
                while ((message = in.readLine()) != null) {
                    broadcastMessage(username + ": " + message);
                    appendLog(username + ": " + message);
                }
            } catch (IOException e) {
                appendLog("[*] Connection with client lost: " + socket.getInetAddress());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (clientWriters) {
                    clientWriters.remove(out);
                }
                broadcastMessage("[*] " + username + " left the chat.");
                appendLog("[*] " + username + " left the chat.");
            }
        }

        private void broadcastMessage(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(message);
                }
            }
        }
    }

    private static void appendLog(String log) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatPane.getStyledDocument();
            Style style = chatPane.addStyle("Style", null);

            // Color message based on content
            if (log.contains("[*] has joined the chat.") || log.contains("[*] left the chat")) {
                StyleConstants.setForeground(style, log.contains("joined") ? Color.GREEN : Color.RED);
            } else {
                StyleConstants.setForeground(style, Color.WHITE);
            }

            try {
                doc.insertString(doc.getLength(), log + "\n", style);
                chatPane.setCaretPosition(chatPane.getDocument().getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }
}
