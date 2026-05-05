package com.graalwrapper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReachabilityDialog extends JDialog {

    private final File metadataDir;
    private final File targetJar;
    private final MetadataManager metadataManager;
    private List<MetadataManager.LibraryConfig> availableLibs;

    private DefaultTableModel tableModel;
    private JTable libsTable;
    private JButton downloadBtn;
    private JButton applyBtn;
    private JLabel statusLabel;

    private String resultArgs = "";

    public ReachabilityDialog(Frame parent, File workingDir, File targetJar) {
        super(parent, "Reachability Metadata", true);
        this.metadataDir = new File(workingDir, ".graalvm-metadata");
        this.targetJar = targetJar;
        this.metadataManager = new MetadataManager();

        initUI();
        checkMetadataStatus();

        setSize(600, 500);
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        JPanel topPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Status: Checking repository...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        downloadBtn = new JButton("Extract Offline Repository");
        downloadBtn.addActionListener(e -> downloadRepository());
        
        topPanel.add(statusLabel, BorderLayout.CENTER);
        topPanel.add(downloadBtn, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"Use", "Library", "Version"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };
        libsTable = new JTable(tableModel);
        libsTable.getColumnModel().getColumn(0).setMaxWidth(50);
        
        add(new JScrollPane(libsTable), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        applyBtn = new JButton("Apply Selected");
        applyBtn.setEnabled(false);
        applyBtn.addActionListener(e -> generateResult());
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> {
            resultArgs = "";
            dispose();
        });

        bottomPanel.add(cancelBtn);
        bottomPanel.add(applyBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void checkMetadataStatus() {
        File repoRoot = new File(metadataDir, "graalvm-reachability-metadata-master/metadata");
        if (repoRoot.exists() && repoRoot.isDirectory()) {
            statusLabel.setText("Status: Repository found. Scanning libraries...");
            downloadBtn.setText("Re-extract Repository");
            loadLibraries();
        } else {
            statusLabel.setText("Status: Repository not found. Please extract offline bundle.");
            downloadBtn.setText("Extract Offline Repository");
        }
    }

    private void downloadRepository() {
        downloadBtn.setEnabled(false);
        applyBtn.setEnabled(false);
        
        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                metadataManager.downloadAndExtractMetadata(metadataDir, (status, percentage) -> {
                    publish(status + " (" + percentage + "%)");
                });
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                statusLabel.setText(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                try {
                    get();
                    checkMetadataStatus();
                } catch (Exception e) {
                    statusLabel.setText("Error: " + e.getMessage());
                    e.printStackTrace();
                    downloadBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void loadLibraries() {
        new SwingWorker<List<MetadataManager.LibraryConfig>, Void>() {
            @Override
            protected List<MetadataManager.LibraryConfig> doInBackground() {
                availableLibs = metadataManager.parseAvailableLibraries(metadataDir);
                return availableLibs;
            }

            @Override
            protected void done() {
                tableModel.setRowCount(0);
                if (availableLibs.isEmpty()) {
                    statusLabel.setText("Status: Repository is empty or invalid.");
                    return;
                }

                List<MetadataManager.LibraryConfig> autoDetected = metadataManager.autoDetectLibraries(targetJar, availableLibs);
                
                for (MetadataManager.LibraryConfig lib : availableLibs) {
                    boolean isDetected = autoDetected.contains(lib);
                    tableModel.addRow(new Object[]{isDetected, lib.getDisplayName(), lib.latestVersion});
                }
                
                statusLabel.setText("Status: Found " + availableLibs.size() + " libraries. Auto-detected " + autoDetected.size() + " dependencies.");
                applyBtn.setEnabled(true);
                downloadBtn.setEnabled(true);
            }
        }.execute();
    }

    private void generateResult() {
        List<String> selectedPaths = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean isSelected = (Boolean) tableModel.getValueAt(i, 0);
            if (isSelected) {
                selectedPaths.add(availableLibs.get(i).metadataPath);
            }
        }

        if (selectedPaths.isEmpty()) {
            resultArgs = "";
        } else {
            resultArgs = "-H:ConfigurationFileDirectories=\"" + String.join(",", selectedPaths) + "\"";
        }
        dispose();
    }

    public String getResultArgs() {
        return resultArgs;
    }
}
