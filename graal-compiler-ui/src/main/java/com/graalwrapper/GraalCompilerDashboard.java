package com.graalwrapper;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private JTextArea consoleArea;
    private final JButton buildButton;
    private final JButton runAgentButton;
    
    private File currentProjectFile = new File("last-build.graalproj");

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

        setupMenuBar();

        // Create main container with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top section: Inputs
        mainPanel.add(createInputPanel(), BorderLayout.NORTH);

        // Center section: Console output
        mainPanel.add(createConsolePanel(), BorderLayout.CENTER);

        // Bottom section: Build button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        
        runAgentButton = new JButton("Run Tracing Agent");
        runAgentButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        runAgentButton.setPreferredSize(new Dimension(200, 40));
        runAgentButton.addActionListener(e -> startTracingAgent());
        buttonPanel.add(runAgentButton);

        buildButton = new JButton("Build Native Image");
        buildButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        buildButton.setPreferredSize(new Dimension(200, 40));
        buildButton.addActionListener(e -> startBuildProcess());
        buttonPanel.add(buildButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
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
        
        // Auto-load last build if exists
        SwingUtilities.invokeLater(() -> {
            if (currentProjectFile.exists()) {
                loadProject(currentProjectFile);
            }
        });
    }

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
            if (consoleArea != null) {
                consoleArea.append("Project saved to: " + file.getAbsolutePath() + "\n");
            }
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
            
            if (consoleArea != null) {
                consoleArea.append("Project loaded from: " + file.getAbsolutePath() + "\n");
            }
        } catch (Exception ex) {
            if (consoleArea != null) {
                consoleArea.append("Failed to load project: " + ex.getMessage() + "\n");
            }
        }
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

        // Main Class
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        panel.add(new JLabel("Main Class:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        mainClassField = new JTextField();
        mainClassField.setToolTipText("Auto-detected from JAR, or type manually (e.g. com.example.Main)");
        panel.add(mainClassField, gbc);
        gbc.gridwidth = 1;

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
        gbc.gridx = 1; gbc.weightx = 1.0;
        additionalArgsField = new JTextField("--no-fallback");
        panel.add(additionalArgsField, gbc);
        gbc.gridx = 2; gbc.weightx = 0.0;
        JButton reachabilityBtn = new JButton("Reachability Metadata");
        reachabilityBtn.addActionListener(e -> openReachabilityDialog());
        panel.add(reachabilityBtn, gbc);

        row++;

        // Agent JVM Args
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        panel.add(new JLabel("Agent JVM Args:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        agentArgsField = new JTextField("");
        agentArgsField.setToolTipText("e.g. -Djava.library.path=natives");
        panel.add(agentArgsField, gbc);
        gbc.gridwidth = 1;

        row++;

        // External Resources (Folders)
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        panel.add(new JLabel("External Resources (Folders):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        externalResourcesField = new JTextField();
        externalResourcesField.setToolTipText("Select folders to embed into .exe (separated by ;)");
        panel.add(externalResourcesField, gbc);
        gbc.gridx = 2; gbc.weightx = 0.0;
        JButton browseExtResBtn = new JButton(BROWSE_LABEL);
        browseExtResBtn.addActionListener(e -> browseMultipleDirectories(externalResourcesField));
        panel.add(browseExtResBtn, gbc);

        row++;

        // Include Resources
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        panel.add(new JLabel("Include Resources (Regex):"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        includeResourcesField = new JTextField();
        includeResourcesField.setToolTipText("e.g. .* (Leave empty to include all selected folders)");
        panel.add(includeResourcesField, gbc);
        gbc.gridwidth = 1;

        row++;

        // Build-Time Init
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        panel.add(new JLabel("Build-Time Init Packages:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        buildTimeInitField = new JTextField();
        buildTimeInitField.setToolTipText("Comma-separated list of packages/classes (e.g. com.example, org.jogl)");
        panel.add(buildTimeInitField, gbc);
        gbc.gridwidth = 1;

        row++;

        // Run-Time Init
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        panel.add(new JLabel("Run-Time Init Packages:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        runTimeInitField = new JTextField();
        runTimeInitField.setToolTipText("Comma-separated list of packages/classes (e.g. com.example.runtime)");
        panel.add(runTimeInitField, gbc);
        gbc.gridwidth = 1;

        row++;

        // Quick Flags
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        panel.add(new JLabel("Quick Flags:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        
        JPanel flagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        standalonePackagerCb = new JCheckBox("Package as Standalone");
        standalonePackagerCb.setToolTipText("Automatically creates a distribution folder with GraalVM DLLs and lib folder for AWT/Swing compatibility.");
        standalonePackagerCb.setSelected(true); // Default to true since it's very helpful
        enableHttpsCb = new JCheckBox("Enable HTTPS");
        enableHttpsCb.setToolTipText("Adds --enable-https --enable-http");
        staticBuildCb = new JCheckBox("Static Build");
        staticBuildCb.setToolTipText("Adds --static");
        verboseCb = new JCheckBox("Verbose");
        verboseCb.setToolTipText("Adds --verbose");
        exitHandlersCb = new JCheckBox("Exit Handlers");
        exitHandlersCb.setToolTipText("Adds --install-exit-handlers");
        diagnosticsCb = new JCheckBox("Diagnostics");
        diagnosticsCb.setToolTipText("Adds --diagnostics-mode");
        mergeAgentConfigsCb = new JCheckBox("Merge Agent Configs");
        mergeAgentConfigsCb.setToolTipText("Merges new Tracing Agent configs into existing ones instead of overwriting.");
        mergeAgentConfigsCb.setSelected(true);
        
        flagsPanel.add(standalonePackagerCb);
        flagsPanel.add(enableHttpsCb);
        flagsPanel.add(staticBuildCb);
        flagsPanel.add(verboseCb);
        flagsPanel.add(exitHandlersCb);
        flagsPanel.add(diagnosticsCb);
        flagsPanel.add(mergeAgentConfigsCb);
        
        panel.add(flagsPanel, gbc);
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

        row++;

        // Target OS
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        panel.add(new JLabel("Target OS:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        String[] osOptions = {"Windows (.exe)", "Linux (ELF via Docker)"};
        targetOsComboBox = new JComboBox<>(osOptions);
        targetOsComboBox.setSelectedIndex(0);
        targetOsComboBox.setToolTipText("Select the target operating system for the Native Image.");
        panel.add(targetOsComboBox, gbc);
        gbc.gridwidth = 1;

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
            File selected = chooser.getSelectedFile();
            targetField.setText(selected.getAbsolutePath());
            
            // Auto-detect Main-Class if JAR is selected
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
        
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            StringBuilder sb = new StringBuilder(targetField.getText().trim());
            for (File f : files) {
                if (sb.length() > 0 && !sb.toString().endsWith(";")) {
                    sb.append(";");
                }
                sb.append(f.getAbsolutePath());
            }
            targetField.setText(sb.toString());
        }
    }

    private void openReachabilityDialog() {
        String targetJarPath = targetJarField.getText().trim();
        File targetJar = null;
        File workingDir = new File(".");
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

    private void startBuildProcess() {
        // Validate inputs
        String targetJar = targetJarField.getText().trim();
        String mainClass = mainClassField.getText().trim();
        if (targetJar.isEmpty() || mainClass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a Target JAR File and ensure Main Class is specified.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Auto-save project settings
        saveProject(currentProjectFile);

        String graalHome = graalHomeField.getText().trim();
        String vcvarsPath = vcvarsField.getText().trim();
        
        // Combine options
        String ramOption = (String) ramComboBox.getSelectedItem();
        String extraArgs = additionalArgsField.getText().trim();
        
        StringBuilder optionsBuilder = new StringBuilder();
        optionsBuilder.append(ramOption).append(" ").append(extraArgs);
        
        String externalRes = externalResourcesField.getText().trim();
        StringBuilder classPathBuilder = new StringBuilder(targetJar);
        if (!externalRes.isEmpty()) {
            String[] folders = externalRes.split(";");
            for (String folder : folders) {
                if (!folder.trim().isEmpty()) {
                    classPathBuilder.append(";").append(folder.trim());
                }
            }
        }
        String classPath = classPathBuilder.toString();
        
        String includeRes = includeResourcesField.getText().trim();
        if (!includeRes.isEmpty()) {
            optionsBuilder.append(" -H:IncludeResources=\"").append(includeRes).append("\"");
        } else if (!externalRes.isEmpty()) {
            // If external resources selected but regex is empty, default to embed all
            optionsBuilder.append(" -H:IncludeResources=\".*\"");
        }

        // Init Packages Parse
        String buildTimeStr = buildTimeInitField.getText().trim();
        if (!buildTimeStr.isEmpty()) {
            buildTimeStr = buildTimeStr.replaceAll("\\s+", "");
            optionsBuilder.append(" --initialize-at-build-time=").append(buildTimeStr);
        }

        String runTimeStr = runTimeInitField.getText().trim();
        if (!runTimeStr.isEmpty()) {
            runTimeStr = runTimeStr.replaceAll("\\s+", "");
            optionsBuilder.append(" --initialize-at-run-time=").append(runTimeStr);
        }

        if (enableHttpsCb.isSelected()) optionsBuilder.append(" --enable-https --enable-http");
        if (staticBuildCb.isSelected()) optionsBuilder.append(" --static");
        if (verboseCb.isSelected()) optionsBuilder.append(" --verbose");
        if (exitHandlersCb.isSelected()) optionsBuilder.append(" --install-exit-handlers");
        if (diagnosticsCb.isSelected()) optionsBuilder.append(" --diagnostics-mode");
        
        String combinedOptions = optionsBuilder.toString().trim();

        // UI state update
        buildButton.setEnabled(false);
        buildButton.setText("Building...");
        runAgentButton.setEnabled(false);
        consoleArea.setText("");

        File workingDir = new File(targetJar).getParentFile();

        boolean packageStandalone = standalonePackagerCb.isSelected();
        String targetOs = (String) targetOsComboBox.getSelectedItem();
        
        NativeImageExecutor executor = new NativeImageExecutor();
        
        // We use the executor which already spawns a background thread.
        // SwingUtilities.invokeLater is used to safely update the UI from the background thread.
        executor.execute(vcvarsPath, graalHome, combinedOptions, classPath, mainClass, workingDir, packageStandalone, targetOs, new NativeImageExecutor.LogListener() {
            @Override
            public void onLogMessage(String message) {
                SwingUtilities.invokeLater(() -> consoleArea.append(message + "\n"));
            }

            @Override
            public void onProcessComplete(int exitCode) {
                SwingUtilities.invokeLater(() -> {
                    consoleArea.append("\nProcess finished with exit code: " + exitCode + "\n");
                    resetButtons();
                });
            }

            @Override
            public void onProcessFailed(Exception e) {
                SwingUtilities.invokeLater(() -> {
                    consoleArea.append("\nProcess failed to execute: " + e.getMessage() + "\n");
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

        // Auto-save project settings
        saveProject(currentProjectFile);

        String graalHome = graalHomeField.getText().trim();
        File workingDir = new File(targetJar).getParentFile();
        File configDir = new File(workingDir, "native-image-configs");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        buildButton.setEnabled(false);
        runAgentButton.setEnabled(false);
        runAgentButton.setText("Agent Running...");
        consoleArea.setText("");

        String classPath = targetJar;
        String externalRes = externalResourcesField.getText().trim();
        if (!externalRes.isEmpty()) {
            classPath += ";" + externalRes;
        }

        NativeImageExecutor executor = new NativeImageExecutor();
        String agentArgs = agentArgsField.getText().trim();
        boolean mergeConfigs = mergeAgentConfigsCb.isSelected();
        executor.executeAgent(graalHome, agentArgs, classPath, mainClass, workingDir, mergeConfigs, new NativeImageExecutor.LogListener() {
            @Override
            public void onLogMessage(String message) {
                SwingUtilities.invokeLater(() -> consoleArea.append(message + "\n"));
            }

            @Override
            public void onProcessComplete(int exitCode) {
                SwingUtilities.invokeLater(() -> {
                    consoleArea.append("\nAgent process finished with exit code: " + exitCode + "\n");
                    consoleArea.append("Configurations saved to: " + configDir.getAbsolutePath() + "\n");
                    
                    // Auto-append config to Additional Arguments if not present
                    String currentArgs = additionalArgsField.getText().trim();
                    if (!currentArgs.contains("ConfigurationFileDirectories=native-image-configs")) {
                        if (!currentArgs.isEmpty()) {
                            currentArgs += " ";
                        }
                        currentArgs += "-H:ConfigurationFileDirectories=native-image-configs";
                        additionalArgsField.setText(currentArgs);
                    }
                    
                    resetButtons();
                });
            }

            @Override
            public void onProcessFailed(Exception e) {
                SwingUtilities.invokeLater(() -> {
                    consoleArea.append("\nAgent process failed to execute: " + e.getMessage() + "\n");
                    resetButtons();
                });
            }
        });
    }

    private void resetButtons() {
        buildButton.setEnabled(true);
        buildButton.setText("Build Native Image");
        runAgentButton.setEnabled(true);
        runAgentButton.setText("Run Tracing Agent");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GraalCompilerDashboard().setVisible(true));
    }
}
