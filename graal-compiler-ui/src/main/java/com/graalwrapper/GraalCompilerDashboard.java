package com.graalwrapper;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.File;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GraalCompilerDashboard extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(GraalCompilerDashboard.class.getName());
    private static final String BROWSE_LABEL = "Browse...";

    private JTextField targetJarField;
    private JTextField graalHomeField;
    private JTextField vcvarsField;
    private JTextField additionalArgsField;
    private JComboBox<String> ramComboBox;
    private JTextArea consoleArea;
    private final JButton buildButton;

    public GraalCompilerDashboard() {
        super("GraalVM Native Image Compiler Dashboard");
        
        // Setup FlatLaf Dark Theme
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException ex) {
            LOGGER.log(Level.SEVERE, "Failed to initialize FlatLaf", ex);
        }

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Create main container with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top section: Inputs
        mainPanel.add(createInputPanel(), BorderLayout.NORTH);

        // Center section: Console output
        mainPanel.add(createConsolePanel(), BorderLayout.CENTER);

        // Bottom section: Build button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buildButton = new JButton("Build Native Image");
        buildButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        buildButton.setPreferredSize(new Dimension(200, 40));
        buildButton.addActionListener(e -> startBuildProcess());
        buttonPanel.add(buildButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Target JAR
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        panel.add(new JLabel("Target JAR File:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        targetJarField = new JTextField();
        panel.add(targetJarField, gbc);
        gbc.gridx = 2; gbc.weightx = 0.0;
        JButton browseJarBtn = new JButton(BROWSE_LABEL);
        browseJarBtn.addActionListener(e -> browseFile(targetJarField, false));
        panel.add(browseJarBtn, gbc);

        row++;

        // GraalVM Home
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        panel.add(new JLabel("GraalVM Home:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        // Default to relative path
        graalHomeField = new JTextField(new File("graalvm-ce-java11-22.3.3").getAbsolutePath());
        panel.add(graalHomeField, gbc);
        gbc.gridx = 2; gbc.weightx = 0.0;
        JButton browseGraalBtn = new JButton(BROWSE_LABEL);
        browseGraalBtn.addActionListener(e -> browseFile(graalHomeField, true));
        panel.add(browseGraalBtn, gbc);

        row++;

        // vcvars64.bat Path
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        panel.add(new JLabel("vcvars64.bat Path:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        vcvarsField = new JTextField("C:\\Program Files\\Microsoft Visual Studio\\2022\\BuildTools\\VC\\Auxiliary\\Build\\vcvars64.bat");
        panel.add(vcvarsField, gbc);
        gbc.gridx = 2; gbc.weightx = 0.0;
        JButton browseVsBtn = new JButton(BROWSE_LABEL);
        browseVsBtn.addActionListener(e -> browseFile(vcvarsField, false));
        panel.add(browseVsBtn, gbc);

        row++;

        // Additional Arguments
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        panel.add(new JLabel("Additional Arguments:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        additionalArgsField = new JTextField("--no-fallback");
        panel.add(additionalArgsField, gbc);
        gbc.gridwidth = 1;

        row++;

        // RAM Allocation
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        panel.add(new JLabel("Max Build RAM:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        String[] ramOptions = {"-J-Xmx4G", "-J-Xmx6G", "-J-Xmx8G", "-J-Xmx12G", "-J-Xmx16G"};
        ramComboBox = new JComboBox<>(ramOptions);
        ramComboBox.setSelectedIndex(2); // Default to 8G
        panel.add(ramComboBox, gbc);

        return panel;
    }

    private JPanel createConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Build Console"));

        consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        consoleArea.setBackground(new Color(30, 30, 30));
        consoleArea.setForeground(new Color(200, 200, 200));

        // Auto-scroll
        DefaultCaret caret = (DefaultCaret) consoleArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane scrollPane = new JScrollPane(consoleArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void browseFile(JTextField targetField, boolean directoriesOnly) {
        JFileChooser chooser = new JFileChooser();
        if (directoriesOnly) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        
        File currentFile = new File(targetField.getText());
        if (currentFile.exists()) {
            chooser.setSelectedFile(currentFile);
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            targetField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void startBuildProcess() {
        // Validate inputs
        String targetJar = targetJarField.getText().trim();
        if (targetJar.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a Target JAR File.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String graalHome = graalHomeField.getText().trim();
        String vcvarsPath = vcvarsField.getText().trim();
        
        // Combine options
        String ramOption = (String) ramComboBox.getSelectedItem();
        String extraArgs = additionalArgsField.getText().trim();
        String combinedOptions = ramOption + " " + extraArgs;

        // UI state update
        buildButton.setEnabled(false);
        buildButton.setText("Building...");
        consoleArea.setText("");

        File workingDir = new File(targetJar).getParentFile();

        NativeImageExecutor executor = new NativeImageExecutor();
        
        // We use the executor which already spawns a background thread.
        // SwingUtilities.invokeLater is used to safely update the UI from the background thread.
        executor.execute(vcvarsPath, graalHome, combinedOptions, targetJar, workingDir, new NativeImageExecutor.LogListener() {
            @Override
            public void onLogMessage(String message) {
                SwingUtilities.invokeLater(() -> consoleArea.append(message + "\n"));
            }

            @Override
            public void onProcessComplete(int exitCode) {
                SwingUtilities.invokeLater(() -> {
                    consoleArea.append("\nProcess finished with exit code: " + exitCode + "\n");
                    resetBuildButton();
                });
            }

            @Override
            public void onProcessFailed(Exception e) {
                SwingUtilities.invokeLater(() -> {
                    consoleArea.append("\nProcess failed to execute: " + e.getMessage() + "\n");
                    resetBuildButton();
                });
            }
        });
    }

    private void resetBuildButton() {
        buildButton.setEnabled(true);
        buildButton.setText("Build Native Image");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GraalCompilerDashboard().setVisible(true));
    }
}
