package com.graalwrapper;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.formdev.flatlaf.FlatDarkLaf;

public class GraalCompilerDashboard extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(GraalCompilerDashboard.class.getName());
    private static final String BROWSE_LABEL = "Browse...";

    private JTextField targetJarField;
    private JTextField mainClassField;
    private JTextField graalHomeField;
    private JTextField vcvarsField;
    private JTextField additionalArgsField;
    private JTextField agentArgsField;
    private JTextField externalResourcesField;
    private JTextField includeResourcesField;
    private JTextField buildTimeInitField;
    private JTextField runTimeInitField;
    private JComboBox<String> targetOsComboBox;
    private JComboBox<String> ramComboBox;
    private JCheckBox standalonePackagerCb;
    private JCheckBox enableHttpsCb;
    private JCheckBox staticBuildCb;
    private JCheckBox verboseCb;
    private JCheckBox exitHandlersCb;
    private JCheckBox diagnosticsCb;
    private JCheckBox mergeAgentConfigsCb;
    
    private JTextPane consoleArea;
    private JProgressBar buildProgressBar;
    private JButton buildButton;
    private JButton runAgentButton;
    
    private CardLayout cardLayout;
    private JPanel mainCardPanel;

    private File currentProjectFile = new File("last-build.graalproj");

    public GraalCompilerDashboard() {
        super("GraalVM Native Image Compiler Dashboard");
        
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException ex) {
            LOGGER.log(Level.SEVERE, "Failed to initialize FlatLaf", ex);
        }

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1100, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        setupMenuBar();

        // --- Sidebar (Navigation) ---
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(43, 45, 48));
        sidebar.setPreferredSize(new Dimension(200, getHeight()));
        sidebar.setBorder(new EmptyBorder(20, 10, 20, 10));

        JLabel logoLabel = new JLabel("GraalVM Tools");
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(logoLabel);
        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

        cardLayout = new CardLayout();
        mainCardPanel = new JPanel(cardLayout);

        sidebar.add(createNavButton("Compiler", "compilerCard"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(createNavButton("Build History", "historyCard"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(createNavButton("Settings", "settingsCard"));

        add(sidebar, BorderLayout.WEST);

        // --- Main Content Cards ---
        mainCardPanel.add(createCompilerPanel(), "compilerCard");
        mainCardPanel.add(createPlaceholderPanel("Build History - Work in Progress"), "historyCard");
        mainCardPanel.add(createPlaceholderPanel("Settings - Work in Progress"), "settingsCard");

        add(mainCardPanel, BorderLayout.CENTER);
    }

    private JButton createNavButton(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(180, 40));
        btn.setFocusPainted(false);
        btn.addActionListener(e -> cardLayout.show(mainCardPanel, cardName));
        return btn;
    }

    private JPanel createPlaceholderPanel(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        JLabel l = new JLabel(title);
        l.setFont(new Font("Segoe UI", Font.BOLD, 24));
        l.setForeground(Color.GRAY);
        p.add(l);
        return p;
    }

    private JPanel createCompilerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(400); // Top part gets 400px
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(null);

        // Top: Configuration Panel
        JPanel configPanel = new JPanel(new BorderLayout());
        configPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Split Config into two sub-panels: File Pickers (North) and Arguments (Center)
        JPanel pathsPanel = createPathsPanel();
        JPanel argsPanel = createMultiColumnArgsPanel();
        
        configPanel.add(pathsPanel, BorderLayout.NORTH);
        configPanel.add(argsPanel, BorderLayout.CENTER);
        
        JScrollPane configScroll = new JScrollPane(configPanel);
        configScroll.setBorder(null);
        splitPane.setTopComponent(configScroll);

        // Bottom: Execution & Terminal
        splitPane.setBottomComponent(createExecutionPanel());

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPathsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Environment Paths"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Font monoFont = new Font("Consolas", Font.PLAIN, 13);

        int row = 0;
        // Target JAR
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        panel.add(new JLabel("Target JAR:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        targetJarField = new JTextField(); targetJarField.setFont(monoFont);
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
        graalHomeField = new JTextField(new File("graalvm-ce-java11-22.3.3").getAbsolutePath()); graalHomeField.setFont(monoFont);
        panel.add(graalHomeField, gbc);
        gbc.gridx = 2; gbc.weightx = 0.0;
        JButton browseGraalBtn = new JButton(BROWSE_LABEL);
        browseGraalBtn.addActionListener(e -> browseFile(graalHomeField, true));
        panel.add(browseGraalBtn, gbc);
        row++;

        // Vcvars
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        panel.add(new JLabel("vcvars64.bat:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        vcvarsField = new JTextField("C:\\Program Files\\Microsoft Visual Studio\\2022\\BuildTools\\VC\\Auxiliary\\Build\\vcvars64.bat"); vcvarsField.setFont(monoFont);
        panel.add(vcvarsField, gbc);
        gbc.gridx = 2; gbc.weightx = 0.0;
        JButton browseVsBtn = new JButton(BROWSE_LABEL);
        browseVsBtn.addActionListener(e -> browseFile(vcvarsField, false));
        panel.add(browseVsBtn, gbc);
        
        return panel;
    }

    private JPanel createMultiColumnArgsPanel() {
        JPanel outerPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        outerPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        // LEFT COLUMN: Standard Arguments
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Compiler Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        leftPanel.add(new JLabel("Main Class:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        mainClassField = new JTextField();
        leftPanel.add(mainClassField, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        leftPanel.add(new JLabel("Additional Args:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JPanel argsWrapper = new JPanel(new BorderLayout(5, 0));
        additionalArgsField = new JTextField("--no-fallback");
        argsWrapper.add(additionalArgsField, BorderLayout.CENTER);
        JButton reachabilityBtn = new JButton("Reachability Metadata");
        reachabilityBtn.addActionListener(e -> openReachabilityDialog());
        argsWrapper.add(reachabilityBtn, BorderLayout.EAST);
        leftPanel.add(argsWrapper, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        leftPanel.add(new JLabel("Ext Resources:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JPanel extWrapper = new JPanel(new BorderLayout(5, 0));
        externalResourcesField = new JTextField();
        extWrapper.add(externalResourcesField, BorderLayout.CENTER);
        JButton browseExtBtn = new JButton(BROWSE_LABEL);
        browseExtBtn.addActionListener(e -> browseMultipleDirectories(externalResourcesField));
        extWrapper.add(browseExtBtn, BorderLayout.EAST);
        leftPanel.add(extWrapper, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        leftPanel.add(new JLabel("Include Res Regex:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        includeResourcesField = new JTextField();
        leftPanel.add(includeResourcesField, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        leftPanel.add(new JLabel("Agent JVM Args:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        agentArgsField = new JTextField();
        leftPanel.add(agentArgsField, gbc);
        row++;

        // Quick Flags
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JPanel flagsPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        standalonePackagerCb = new JCheckBox("Package as Standalone", true);
        enableHttpsCb = new JCheckBox("Enable HTTPS");
        staticBuildCb = new JCheckBox("Static Build");
        verboseCb = new JCheckBox("Verbose");
        exitHandlersCb = new JCheckBox("Exit Handlers");
        diagnosticsCb = new JCheckBox("Diagnostics");
        mergeAgentConfigsCb = new JCheckBox("Merge Agent Configs", true);
        
        flagsPanel.add(standalonePackagerCb);
        flagsPanel.add(mergeAgentConfigsCb);
        flagsPanel.add(enableHttpsCb);
        flagsPanel.add(staticBuildCb);
        flagsPanel.add(verboseCb);
        flagsPanel.add(exitHandlersCb);
        flagsPanel.add(diagnosticsCb);
        leftPanel.add(flagsPanel, gbc);

        // RIGHT COLUMN: Advanced / Class Init
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Performance & Class Init"));
        GridBagConstraints gbcR = new GridBagConstraints();
        gbcR.insets = new Insets(5, 5, 5, 5);
        gbcR.fill = GridBagConstraints.HORIZONTAL;
        gbcR.anchor = GridBagConstraints.WEST;

        int rowR = 0;
        gbcR.gridx = 0; gbcR.gridy = rowR; gbcR.weightx = 0.0;
        rightPanel.add(new JLabel("Target OS:"), gbcR);
        gbcR.gridx = 1; gbcR.weightx = 1.0;
        targetOsComboBox = new JComboBox<>(new String[]{"Windows (.exe)", "Linux (ELF via Docker)"});
        rightPanel.add(targetOsComboBox, gbcR);
        rowR++;

        gbcR.gridx = 0; gbcR.gridy = rowR; gbcR.weightx = 0.0;
        rightPanel.add(new JLabel("Max Build RAM:"), gbcR);
        gbcR.gridx = 1; gbcR.weightx = 1.0;
        ramComboBox = new JComboBox<>(new String[]{"-J-Xmx4G", "-J-Xmx6G", "-J-Xmx8G", "-J-Xmx12G", "-J-Xmx16G"});
        ramComboBox.setSelectedIndex(2);
        rightPanel.add(ramComboBox, gbcR);
        rowR++;

        gbcR.gridx = 0; gbcR.gridy = rowR; gbcR.gridwidth = 2;
        rightPanel.add(new JSeparator(), gbcR);
        rowR++;

        gbcR.gridx = 0; gbcR.gridy = rowR; gbcR.gridwidth = 2;
        rightPanel.add(new JLabel("Build-Time Init Packages:"), gbcR);
        rowR++;
        gbcR.gridx = 0; gbcR.gridy = rowR; gbcR.gridwidth = 2;
        buildTimeInitField = new JTextField();
        rightPanel.add(buildTimeInitField, gbcR);
        rowR++;

        gbcR.gridx = 0; gbcR.gridy = rowR; gbcR.gridwidth = 2;
        rightPanel.add(new JLabel("Run-Time Init Packages:"), gbcR);
        rowR++;
        gbcR.gridx = 0; gbcR.gridy = rowR; gbcR.gridwidth = 2;
        runTimeInitField = new JTextField();
        rightPanel.add(runTimeInitField, gbcR);
        rowR++;

        gbcR.gridx = 0; gbcR.gridy = rowR; gbcR.gridwidth = 2; gbcR.fill = GridBagConstraints.NONE; gbcR.anchor = GridBagConstraints.EAST;
        JButton selectFromJarBtn = new JButton("Select Packages from JAR...");
        selectFromJarBtn.addActionListener(e -> openClassInitDialog());
        rightPanel.add(selectFromJarBtn, gbcR);

        // Fill remaining space
        gbcR.gridy++; gbcR.weighty = 1.0;
        rightPanel.add(new JLabel(""), gbcR);

        outerPanel.add(leftPanel);
        outerPanel.add(rightPanel);

        return outerPanel;
    }

    private JPanel createExecutionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Execution & Terminal"));

        // Controls Top
        JPanel controlsPanel = new JPanel(new BorderLayout(15, 0));
        controlsPanel.setBorder(new EmptyBorder(5, 5, 10, 5));

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        runAgentButton = new JButton("Run Tracing Agent");
        runAgentButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        runAgentButton.addActionListener(e -> startTracingAgent());
        buttonsPanel.add(runAgentButton);

        buildButton = new JButton("Build Native Executable");
        buildButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        buildButton.setBackground(new Color(62, 138, 204));
        buildButton.setForeground(Color.WHITE);
        buildButton.addActionListener(e -> startBuildProcess());
        buttonsPanel.add(buildButton);

        controlsPanel.add(buttonsPanel, BorderLayout.WEST);

        buildProgressBar = new JProgressBar();
        buildProgressBar.setStringPainted(true);
        buildProgressBar.setString("Ready");
        controlsPanel.add(buildProgressBar, BorderLayout.CENTER);

        panel.add(controlsPanel, BorderLayout.NORTH);

        // Terminal
        consoleArea = new JTextPane();
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        consoleArea.setBackground(new Color(30, 30, 30));
        consoleArea.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(consoleArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void appendColoredText(String message) {
        if (consoleArea == null) return;
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = consoleArea.getStyledDocument();
            SimpleAttributeSet style = new SimpleAttributeSet();
            
            StyleConstants.setForeground(style, new Color(200, 200, 200)); // Default light gray
            
            String lowerMsg = message.toLowerCase();
            if (lowerMsg.contains("error") || lowerMsg.contains("failed") || lowerMsg.contains("exception")) {
                StyleConstants.setForeground(style, new Color(255, 85, 85)); // Red
            } else if (lowerMsg.contains("warning")) {
                StyleConstants.setForeground(style, new Color(255, 184, 108)); // Yellow
            } else if (lowerMsg.contains("success") || lowerMsg.contains("complete") || lowerMsg.contains("finished")) {
                StyleConstants.setForeground(style, new Color(80, 250, 123)); // Green
            }
            
            try {
                doc.insertString(doc.getLength(), message + "\n", style);
                consoleArea.setCaretPosition(doc.getLength());
            } catch (Exception e) {}
        });
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        
        JMenuItem loadItem = new JMenuItem("Load Project...");
        loadItem.addActionListener(e -> loadProjectDialog());
        
        JMenuItem saveItem = new JMenuItem("Save Project");
        saveItem.addActionListener(e -> saveProject(currentProjectFile));
        
        JMenuItem saveAsItem = new JMenuItem("Save Project As...");
        saveAsItem.addActionListener(e -> saveProjectAsDialog());
        
        fileMenu.add(loadItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        
        SwingUtilities.invokeLater(() -> {
            if (currentProjectFile.exists()) {
                loadProject(currentProjectFile);
            }
        });
    }

    // --- Core Logic Methods (Save/Load/Execute) ---

    private void saveProject(File file) {
        Properties props = new Properties();
        props.setProperty("targetJar", targetJarField.getText());
        props.setProperty("mainClass", mainClassField.getText());
        props.setProperty("graalHome", graalHomeField.getText());
        props.setProperty("vcvars", vcvarsField.getText());
        props.setProperty("additionalArgs", additionalArgsField.getText());
        props.setProperty("agentArgs", agentArgsField.getText());
        props.setProperty("externalResources", externalResourcesField.getText());
        props.setProperty("includeResources", includeResourcesField.getText());
        props.setProperty("buildTimeInit", buildTimeInitField.getText());
        props.setProperty("runTimeInit", runTimeInitField.getText());
        props.setProperty("ramOption", (String) ramComboBox.getSelectedItem());
        props.setProperty("standalone", String.valueOf(standalonePackagerCb.isSelected()));
        props.setProperty("enableHttps", String.valueOf(enableHttpsCb.isSelected()));
        props.setProperty("staticBuild", String.valueOf(staticBuildCb.isSelected()));
        props.setProperty("verbose", String.valueOf(verboseCb.isSelected()));
        props.setProperty("exitHandlers", String.valueOf(exitHandlersCb.isSelected()));
        props.setProperty("diagnostics", String.valueOf(diagnosticsCb.isSelected()));
        props.setProperty("mergeConfigs", String.valueOf(mergeAgentConfigsCb.isSelected()));
        props.setProperty("targetOs", (String) targetOsComboBox.getSelectedItem());

        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, "GraalVM Native Image Compiler Project Settings");
            appendColoredText("Project saved to: " + file.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to save project: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveProjectAsDialog() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Graal Project (*.graalproj)", "graalproj"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().endsWith(".graalproj")) {
                file = new File(file.getAbsolutePath() + ".graalproj");
            }
            currentProjectFile = file;
            saveProject(file);
        }
    }

    private void loadProjectDialog() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Graal Project (*.graalproj)", "graalproj"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            currentProjectFile = file;
            loadProject(file);
        }
    }

    private void loadProject(File file) {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
            targetJarField.setText(props.getProperty("targetJar", ""));
            mainClassField.setText(props.getProperty("mainClass", ""));
            graalHomeField.setText(props.getProperty("graalHome", ""));
            vcvarsField.setText(props.getProperty("vcvars", ""));
            additionalArgsField.setText(props.getProperty("additionalArgs", ""));
            agentArgsField.setText(props.getProperty("agentArgs", ""));
            externalResourcesField.setText(props.getProperty("externalResources", ""));
            includeResourcesField.setText(props.getProperty("includeResources", ""));
            buildTimeInitField.setText(props.getProperty("buildTimeInit", ""));
            runTimeInitField.setText(props.getProperty("runTimeInit", ""));
            ramComboBox.setSelectedItem(props.getProperty("ramOption", "-J-Xmx8G"));
            
            standalonePackagerCb.setSelected(Boolean.parseBoolean(props.getProperty("standalone", "true")));
            enableHttpsCb.setSelected(Boolean.parseBoolean(props.getProperty("enableHttps", "false")));
            staticBuildCb.setSelected(Boolean.parseBoolean(props.getProperty("staticBuild", "false")));
            verboseCb.setSelected(Boolean.parseBoolean(props.getProperty("verbose", "false")));
            exitHandlersCb.setSelected(Boolean.parseBoolean(props.getProperty("exitHandlers", "false")));
            diagnosticsCb.setSelected(Boolean.parseBoolean(props.getProperty("diagnostics", "false")));
            mergeAgentConfigsCb.setSelected(Boolean.parseBoolean(props.getProperty("mergeConfigs", "true")));
            targetOsComboBox.setSelectedItem(props.getProperty("targetOs", "Windows (.exe)"));
            
            appendColoredText("Project loaded from: " + file.getAbsolutePath());
        } catch (Exception ex) {
            appendColoredText("Failed to load project: " + ex.getMessage());
        }
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

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            targetField.setText(selected.getAbsolutePath());
            if (targetField == targetJarField && selected.getName().toLowerCase().endsWith(".jar")) {
                autoDetectMainClass(selected);
            }
        }
    }

    private void autoDetectMainClass(File jarFile) {
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
            java.util.jar.Manifest manifest = jar.getManifest();
            if (manifest != null) {
                String mainClass = manifest.getMainAttributes().getValue(java.util.jar.Attributes.Name.MAIN_CLASS);
                if (mainClass != null) {
                    mainClassField.setText(mainClass);
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Could not read Main-Class from JAR", ex);
        }
    }

    private void browseMultipleDirectories(JTextField targetField) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            StringBuilder sb = new StringBuilder(targetField.getText().trim());
            for (File f : files) {
                if (sb.length() > 0 && !sb.toString().endsWith(";")) sb.append(";");
                sb.append(f.getAbsolutePath());
            }
            targetField.setText(sb.toString());
        }
    }

    private void openReachabilityDialog() {
        String targetJarPath = targetJarField.getText().trim();
        File workingDir = new File(".");
        File targetJar = null;
        if (!targetJarPath.isEmpty()) {
            targetJar = new File(targetJarPath);
            if (targetJar.exists() && targetJar.getParentFile() != null) {
                workingDir = targetJar.getParentFile();
            }
        }
        
        ReachabilityDialog dialog = new ReachabilityDialog(this, workingDir, targetJar);
        dialog.setVisible(true);
        
        String result = dialog.getResultArgs();
        if (result != null && !result.isEmpty()) {
            String current = additionalArgsField.getText().trim();
            if (!current.contains(result)) {
                additionalArgsField.setText(current.isEmpty() ? result : current + " " + result);
            }
        }
    }

    private void openClassInitDialog() {
        String targetJarPath = targetJarField.getText().trim();
        if (targetJarPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a Target JAR File first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        File targetJar = new File(targetJarPath);
        if (!targetJar.exists()) {
            JOptionPane.showMessageDialog(this, "Target JAR file does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ClassInitDialog dialog = new ClassInitDialog(this, targetJar, buildTimeInitField.getText(), runTimeInitField.getText());
        dialog.setVisible(true);

        if (dialog.isApplied()) {
            buildTimeInitField.setText(dialog.getResultBuildTime());
            runTimeInitField.setText(dialog.getResultRunTime());
        }
    }

    private void startBuildProcess() {
        String targetJar = targetJarField.getText().trim();
        String mainClass = mainClassField.getText().trim();
        if (targetJar.isEmpty() || mainClass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a Target JAR File and ensure Main Class is specified.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        saveProject(currentProjectFile);

        String graalHome = graalHomeField.getText().trim();
        String vcvarsPath = vcvarsField.getText().trim();
        
        StringBuilder optionsBuilder = new StringBuilder();
        optionsBuilder.append(ramComboBox.getSelectedItem()).append(" ").append(additionalArgsField.getText().trim());
        
        String externalRes = externalResourcesField.getText().trim();
        StringBuilder classPathBuilder = new StringBuilder(targetJar);
        if (!externalRes.isEmpty()) {
            for (String folder : externalRes.split(";")) {
                if (!folder.trim().isEmpty()) classPathBuilder.append(";").append(folder.trim());
            }
        }
        String classPath = classPathBuilder.toString();
        
        String includeRes = includeResourcesField.getText().trim();
        if (!includeRes.isEmpty()) {
            optionsBuilder.append(" -H:IncludeResources=\"").append(includeRes).append("\"");
        } else if (!externalRes.isEmpty()) {
            optionsBuilder.append(" -H:IncludeResources=\".*\"");
        }

        String buildTimeStr = buildTimeInitField.getText().trim().replaceAll("\\s+", "");
        if (!buildTimeStr.isEmpty()) optionsBuilder.append(" --initialize-at-build-time=").append(buildTimeStr);

        String runTimeStr = runTimeInitField.getText().trim().replaceAll("\\s+", "");
        if (!runTimeStr.isEmpty()) optionsBuilder.append(" --initialize-at-run-time=").append(runTimeStr);

        if (enableHttpsCb.isSelected()) optionsBuilder.append(" --enable-https --enable-http");
        if (staticBuildCb.isSelected()) optionsBuilder.append(" --static");
        if (verboseCb.isSelected()) optionsBuilder.append(" --verbose");
        if (exitHandlersCb.isSelected()) optionsBuilder.append(" --install-exit-handlers");
        if (diagnosticsCb.isSelected()) optionsBuilder.append(" --diagnostics-mode");
        
        String combinedOptions = optionsBuilder.toString().trim();

        buildButton.setEnabled(false);
        runAgentButton.setEnabled(false);
        consoleArea.setText("");
        buildProgressBar.setIndeterminate(true);
        buildProgressBar.setString("Building Native Executable...");

        File workingDir = new File(targetJar).getParentFile();
        boolean packageStandalone = standalonePackagerCb.isSelected();
        String targetOs = (String) targetOsComboBox.getSelectedItem();
        
        NativeImageExecutor executor = new NativeImageExecutor();
        executor.execute(vcvarsPath, graalHome, combinedOptions, classPath, mainClass, workingDir, packageStandalone, targetOs, new NativeImageExecutor.LogListener() {
            @Override
            public void onLogMessage(String message) {
                appendColoredText(message);
            }

            @Override
            public void onProcessComplete(int exitCode) {
                SwingUtilities.invokeLater(() -> {
                    appendColoredText("\nProcess finished with exit code: " + exitCode);
                    resetButtons();
                });
            }

            @Override
            public void onProcessFailed(Exception e) {
                SwingUtilities.invokeLater(() -> {
                    appendColoredText("\nProcess failed to execute: " + e.getMessage());
                    resetButtons();
                });
            }
        });
    }

    private void startTracingAgent() {
        String targetJar = targetJarField.getText().trim();
        String mainClass = mainClassField.getText().trim();
        if (targetJar.isEmpty() || mainClass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a Target JAR File and ensure Main Class is specified.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        saveProject(currentProjectFile);

        String graalHome = graalHomeField.getText().trim();
        File workingDir = new File(targetJar).getParentFile();
        File configDir = new File(workingDir, "native-image-configs");
        if (!configDir.exists()) configDir.mkdirs();

        buildButton.setEnabled(false);
        runAgentButton.setEnabled(false);
        consoleArea.setText("");
        buildProgressBar.setIndeterminate(true);
        buildProgressBar.setString("Running Tracing Agent...");

        String classPath = targetJar;
        String externalRes = externalResourcesField.getText().trim();
        if (!externalRes.isEmpty()) classPath += ";" + externalRes;

        NativeImageExecutor executor = new NativeImageExecutor();
        executor.executeAgent(graalHome, agentArgsField.getText().trim(), classPath, mainClass, workingDir, mergeAgentConfigsCb.isSelected(), new NativeImageExecutor.LogListener() {
            @Override
            public void onLogMessage(String message) {
                appendColoredText(message);
            }

            @Override
            public void onProcessComplete(int exitCode) {
                SwingUtilities.invokeLater(() -> {
                    appendColoredText("\nAgent process finished with exit code: " + exitCode);
                    appendColoredText("Configurations saved to: " + configDir.getAbsolutePath());
                    
                    String currentArgs = additionalArgsField.getText().trim();
                    if (!currentArgs.contains("ConfigurationFileDirectories=native-image-configs")) {
                        additionalArgsField.setText(currentArgs + (currentArgs.isEmpty() ? "" : " ") + "-H:ConfigurationFileDirectories=native-image-configs");
                    }
                    resetButtons();
                });
            }

            @Override
            public void onProcessFailed(Exception e) {
                SwingUtilities.invokeLater(() -> {
                    appendColoredText("\nAgent process failed to execute: " + e.getMessage());
                    resetButtons();
                });
            }
        });
    }

    private void resetButtons() {
        buildButton.setEnabled(true);
        runAgentButton.setEnabled(true);
        buildProgressBar.setIndeterminate(false);
        buildProgressBar.setValue(100);
        buildProgressBar.setString("Ready");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GraalCompilerDashboard().setVisible(true));
    }
}
