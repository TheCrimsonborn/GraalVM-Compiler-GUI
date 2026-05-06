package com.graalwrapper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ClassInitDialog extends JDialog {

    private final File targetJar;
    private final Set<String> initialBuildTimePackages;
    private final Set<String> initialRunTimePackages;

    private DefaultTableModel tableModel;
    private JTable packagesTable;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField searchField;

    private String resultBuildTime = "";
    private String resultRunTime = "";
    private boolean applied = false;

    private static final String PHASE_NONE = "None";
    private static final String PHASE_BUILD = "Build-Time";
    private static final String PHASE_RUN = "Run-Time";

    public ClassInitDialog(Frame parent, File targetJar, String currentBuildTimeStr, String currentRunTimeStr) {
        super(parent, "Visual Class Initialization Manager", true);
        this.targetJar = targetJar;
        
        this.initialBuildTimePackages = parseCommaSeparated(currentBuildTimeStr);
        this.initialRunTimePackages = parseCommaSeparated(currentRunTimeStr);

        initUI();
        loadPackagesFromJar();

        setSize(700, 600);
        setLocationRelativeTo(parent);
    }

    private Set<String> parseCommaSeparated(String str) {
        Set<String> set = new HashSet<>();
        if (str != null && !str.trim().isEmpty()) {
            for (String part : str.split(",")) {
                if (!part.trim().isEmpty()) {
                    set.add(part.trim());
                }
            }
        }
        return set;
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Top panel: Search
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        topPanel.add(new JLabel("Search Package:"), BorderLayout.WEST);
        searchField = new JTextField();
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String text = searchField.getText();
                if (text.trim().length() == 0) {
                    rowSorter.setRowFilter(null);
                } else {
                    rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0));
                }
            }
        });
        topPanel.add(searchField, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // Center panel: Table
        tableModel = new DefaultTableModel(new Object[]{"Package Name", "Initialization Phase"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; // Only the combo box column is editable
            }
        };

        packagesTable = new JTable(tableModel);
        packagesTable.setRowHeight(25);
        
        // Setup Combo Box editor for Phase column
        TableColumn phaseColumn = packagesTable.getColumnModel().getColumn(1);
        JComboBox<String> comboBox = new JComboBox<>();
        comboBox.addItem(PHASE_NONE);
        comboBox.addItem(PHASE_BUILD);
        comboBox.addItem(PHASE_RUN);
        phaseColumn.setCellEditor(new DefaultCellEditor(comboBox));
        
        rowSorter = new TableRowSorter<>(tableModel);
        packagesTable.setRowSorter(rowSorter);

        JScrollPane scrollPane = new JScrollPane(packagesTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel: Buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyBtn = new JButton("Apply Configuration");
        applyBtn.addActionListener(e -> applyAndClose());
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        
        bottomPanel.add(cancelBtn);
        bottomPanel.add(applyBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadPackagesFromJar() {
        if (targetJar == null || !targetJar.exists()) {
            JOptionPane.showMessageDialog(this, "Target JAR file not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Set<String> uniquePackages = new TreeSet<>();
        
        // First, add existing manually entered packages to the list even if not in JAR
        uniquePackages.addAll(initialBuildTimePackages);
        uniquePackages.addAll(initialRunTimePackages);

        // Then extract from JAR
        try (ZipFile zipFile = new ZipFile(targetJar)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!entry.isDirectory() && name.endsWith(".class")) {
                    int lastSlash = name.lastIndexOf('/');
                    if (lastSlash != -1) {
                        String pkg = name.substring(0, lastSlash).replace('/', '.');
                        uniquePackages.add(pkg);
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error reading JAR: " + e.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
        }

        // Populate table
        for (String pkg : uniquePackages) {
            String phase = PHASE_NONE;
            if (initialBuildTimePackages.contains(pkg)) {
                phase = PHASE_BUILD;
            } else if (initialRunTimePackages.contains(pkg)) {
                phase = PHASE_RUN;
            }
            tableModel.addRow(new Object[]{pkg, phase});
        }
    }

    private void applyAndClose() {
        // We must stop cell editing before grabbing data, otherwise the current combo box change might be lost
        if (packagesTable.isEditing()) {
            packagesTable.getCellEditor().stopCellEditing();
        }

        List<String> buildList = new ArrayList<>();
        List<String> runList = new ArrayList<>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String pkg = (String) tableModel.getValueAt(i, 0);
            String phase = (String) tableModel.getValueAt(i, 1);
            
            if (PHASE_BUILD.equals(phase)) {
                buildList.add(pkg);
            } else if (PHASE_RUN.equals(phase)) {
                runList.add(pkg);
            }
        }

        resultBuildTime = String.join(",", buildList);
        resultRunTime = String.join(",", runList);
        applied = true;
        dispose();
    }

    public boolean isApplied() {
        return applied;
    }

    public String getResultBuildTime() {
        return resultBuildTime;
    }

    public String getResultRunTime() {
        return resultRunTime;
    }
}
