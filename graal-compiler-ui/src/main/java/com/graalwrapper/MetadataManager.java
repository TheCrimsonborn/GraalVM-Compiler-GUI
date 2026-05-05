package com.graalwrapper;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class MetadataManager {

    public interface ProgressListener {
        void onProgress(String status, int percentage);
    }

    public static class LibraryConfig {
        public String groupId;
        public String artifactId;
        public List<String> versions;
        public String latestVersion;
        public String metadataPath; // Path relative to the working dir or absolute path

        public LibraryConfig(String groupId, String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.versions = new ArrayList<>();
        }
        
        public String getDisplayName() {
            return groupId + ":" + artifactId;
        }
    }

    private static final String REPO_URL = "https://github.com/oracle/graalvm-reachability-metadata/archive/refs/heads/master.zip";

    /**
     * Extracts the metadata repository from the bundled resources to the specified target directory.
     */
    public void downloadAndExtractMetadata(File targetDir, ProgressListener listener) throws IOException {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        listener.onProgress("Extracting Offline Repository...", 0);
        try (InputStream is = getClass().getResourceAsStream("/metadata.zip")) {
            if (is == null) {
                throw new IOException("metadata.zip not found in application resources!");
            }
            try (ZipInputStream zis = new ZipInputStream(is)) {
                ZipEntry zipEntry = zis.getNextEntry();
                while (zipEntry != null) {
                    File newFile = newFile(targetDir, zipEntry);
                    if (zipEntry.isDirectory()) {
                        if (!newFile.isDirectory() && !newFile.mkdirs()) {
                            throw new IOException("Failed to create directory " + newFile);
                        }
                    } else {
                        File parent = newFile.getParentFile();
                        if (!parent.isDirectory() && !parent.mkdirs()) {
                            throw new IOException("Failed to create directory " + parent);
                        }
                        try (FileOutputStream fos = new FileOutputStream(newFile)) {
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                    zipEntry = zis.getNextEntry();
                }
                zis.closeEntry();
            }
        }
        
        listener.onProgress("Ready", 100);
    }

    private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }

    /**
     * Parses the available libraries from the extracted metadata directory.
     * Expects structure: targetDir/graalvm-reachability-metadata-master/metadata/<groupId>/<artifactId>/<version>
     */
    public List<LibraryConfig> parseAvailableLibraries(File targetDir) {
        List<LibraryConfig> libraries = new ArrayList<>();
        File metadataRoot = new File(targetDir, "graalvm-reachability-metadata-master/metadata");
        
        if (!metadataRoot.exists() || !metadataRoot.isDirectory()) {
            return libraries;
        }

        File[] groupDirs = metadataRoot.listFiles(File::isDirectory);
        if (groupDirs == null) return libraries;

        for (File groupDir : groupDirs) {
            String groupId = groupDir.getName();
            File[] artifactDirs = groupDir.listFiles(File::isDirectory);
            if (artifactDirs == null) continue;

            for (File artifactDir : artifactDirs) {
                String artifactId = artifactDir.getName();
                File[] versionDirs = artifactDir.listFiles(File::isDirectory);
                if (versionDirs == null || versionDirs.length == 0) continue;

                LibraryConfig config = new LibraryConfig(groupId, artifactId);
                for (File vDir : versionDirs) {
                    config.versions.add(vDir.getName());
                }
                // Sort versions to find latest (simple string sort, though semantic is better)
                Collections.sort(config.versions);
                config.latestVersion = config.versions.get(config.versions.size() - 1);
                
                // Set metadata path to the latest version folder
                File latestDir = new File(artifactDir, config.latestVersion);
                config.metadataPath = latestDir.getAbsolutePath();
                
                libraries.add(config);
            }
        }
        
        libraries.sort(Comparator.comparing(LibraryConfig::getDisplayName));
        return libraries;
    }

    /**
     * Scans a target Fat JAR to auto-detect included libraries by reading META-INF/maven/pom.properties.
     */
    public List<LibraryConfig> autoDetectLibraries(File targetJar, List<LibraryConfig> availableLibs) {
        List<LibraryConfig> detected = new ArrayList<>();
        if (targetJar == null || !targetJar.exists() || !targetJar.getName().endsWith(".jar")) {
            return detected;
        }

        Set<String> foundKeys = new HashSet<>();

        try (ZipFile zip = new ZipFile(targetJar)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                
                // Standard Maven pom.properties location
                if (name.startsWith("META-INF/maven/") && name.endsWith("/pom.properties")) {
                    try (InputStream is = zip.getInputStream(entry)) {
                        Properties props = new Properties();
                        props.load(is);
                        String groupId = props.getProperty("groupId");
                        String artifactId = props.getProperty("artifactId");
                        
                        if (groupId != null && artifactId != null) {
                            foundKeys.add(groupId + ":" + artifactId);
                        }
                    } catch (Exception e) {
                        // Ignore properties parsing errors
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Match found keys with available libraries
        for (LibraryConfig lib : availableLibs) {
            if (foundKeys.contains(lib.groupId + ":" + lib.artifactId)) {
                detected.add(lib);
            }
        }

        return detected;
    }
}
