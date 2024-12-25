package Chatapplication;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class ChatClient {
    private static JTextPane chatPane;
    private static JTextField inputField;
    private static PrintWriter out;
    private static BufferedReader in;
    private static String username;
    private static String serverIP;

    private static final Color[] USER_COLORS = {
        Color.CYAN, Color.MAGENTA, Color.ORANGE, Color.PINK, 
        new Color(0x00FF7F), new Color(0xFFD700), new Color(0xFF4500), new Color(0x7B68EE)
    }; // Predefined glowing colors suitable for black background
    private static final Map<String, Color> userColorMap = new HashMap<>();
    private static int colorIndex = 0;

    public static void main(String[] args) {
        // Ask for the server IP and username before connecting
        serverIP = JOptionPane.showInputDialog(null, "Enter the Server IP:", "localhost"); // Default to localhost
        if (serverIP == null || serverIP.isEmpty()) {
            serverIP = "localhost";  // Fallback to localhost if input is empty or canceled
        }

        username = JOptionPane.showInputDialog(null, "Enter your username:", "User" + (int) (Math.random() * 1000)); // Random default username if none entered
        if (username == null || username.isEmpty()) {
            username = "User" + (int) (Math.random() * 1000); // Generate random username if input is empty or canceled
        }

        // Set up the UI
        setupUI();

        try {
            // Establish socket connection with the server
            Socket socket = new Socket(serverIP, 9865);  // Port 9865
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Start a new thread to listen for incoming messages from the server
            Thread listener = new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        appendMessage(message);
                    }
                } catch (IOException e) {
                    appendMessage("[LEFT] Server disconnected.");
                    System.exit(0);
                }
            });
            listener.start();

            // Send the username to the server once the connection is established
            out.println(username);  // Send the username to the server

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to send a message to the server
    private static void sendMessage(String message) {
        out.println(message);  // Send the message to the server
    }

    // Method to append a message to the chat pane (GUI)
    private static void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatPane.getStyledDocument();
            Style style = chatPane.addStyle("Style", null);

            if (message.startsWith("[JOIN]")) {
                // Join message in green
                StyleConstants.setForeground(style, Color.GREEN);
                try {
                    doc.insertString(doc.getLength(), message + "\n", style);
                    chatPane.setCaretPosition(chatPane.getDocument().getLength());
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            } else if (message.startsWith("[LEFT]")) {
                // Leave message in red
                StyleConstants.setForeground(style, Color.RED);
                try {
                    doc.insertString(doc.getLength(), message + "\n", style);
                    chatPane.setCaretPosition(chatPane.getDocument().getLength());
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            } else {
                // General messages with username-specific color
                String[] parts = message.split(": ", 2); // Split the message into username and message
                if (parts.length == 2) {
                    String usernamePart = parts[0];
                    String messagePart = parts[1];

                    // Get or assign a unique color for the user
                    Color userColor = userColorMap.computeIfAbsent(usernamePart, k -> {
                        Color color = USER_COLORS[colorIndex % USER_COLORS.length];
                        colorIndex++;
                        return color;
                    });

                    // Set color for username
                    StyleConstants.setForeground(style, userColor);
                    try {
                        doc.insertString(doc.getLength(), usernamePart + ": ", style);
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }

                    // Set color for the message text (same as username color)
                    try {
                        doc.insertString(doc.getLength(), messagePart + "\n", style);
                        chatPane.setCaretPosition(chatPane.getDocument().getLength());
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Default to white for unexpected message format
                    StyleConstants.setForeground(style, Color.WHITE);
                    try {
                        doc.insertString(doc.getLength(), message + "\n", style);
                        chatPane.setCaretPosition(chatPane.getDocument().getLength());
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    // Method to set up the GUI
    private static void setupUI() {
        JFrame frame = new JFrame("Chat Client");
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(Color.BLACK); // Black background for the chat log
        chatPane.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(chatPane);

        inputField = new JTextField(40);
        inputField.setBackground(new Color(50, 50, 50)); // Dark background for input box
        inputField.setForeground(Color.WHITE); // White text for input box
        inputField.setCaretColor(Color.WHITE); // White cursor
        inputField.setFont(new Font("Arial", Font.PLAIN, 14));
        inputField.setText("Write your message here...");
        inputField.setForeground(Color.GRAY); // Placeholder text color
        inputField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (inputField.getText().equals("Write your message here...")) {
                    inputField.setText("");
                    inputField.setForeground(Color.WHITE);
                }
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                if (inputField.getText().equals("")) {
                    inputField.setText("Write your message here...");
                    inputField.setForeground(Color.GRAY);
                }
            }
        });

        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String message = inputField.getText();
                    if (!message.isEmpty() && !message.equals("Write your message here...")) {
                        sendMessage(message);
                        inputField.setText("");
                    }
                }
            }
        });

        JButton sendButton = new JButton("Send");
        sendButton.setForeground(Color.WHITE);
        sendButton.setBackground(new Color(40, 40, 40)); // Darker gray background
        sendButton.addActionListener(e -> {
            String message = inputField.getText();
            if (!message.isEmpty() && !message.equals("Write your message here...")) {
                sendMessage(message);
                inputField.setText("");
            }
        });

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);

        frame.add(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setVisible(true);
    }
}
