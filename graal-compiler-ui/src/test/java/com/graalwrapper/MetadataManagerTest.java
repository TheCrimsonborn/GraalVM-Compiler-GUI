package com.graalwrapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MetadataManagerTest {

    @TempDir
    Path tempDir;

    @Test
    public void testParseAvailableLibraries() throws IOException {
        MetadataManager manager = new MetadataManager();

        // Setup mock metadata structure
        File targetDir = tempDir.toFile();
        File metadataRoot = new File(targetDir, "graalvm-reachability-metadata-master/metadata");
        assertTrue(metadataRoot.mkdirs());

        // Create mock library 1
        File lib1VersionDir = new File(metadataRoot, "com.example/my-lib/1.0.0");
        assertTrue(lib1VersionDir.mkdirs());

        // Create mock library 2 with multiple versions
        File lib2Version1Dir = new File(metadataRoot, "org.test/test-lib/2.0.0");
        assertTrue(lib2Version1Dir.mkdirs());
        File lib2Version2Dir = new File(metadataRoot, "org.test/test-lib/2.1.0");
        assertTrue(lib2Version2Dir.mkdirs());

        List<MetadataManager.LibraryConfig> libraries = manager.parseAvailableLibraries(targetDir);

        assertEquals(2, libraries.size());

        // Libraries should be sorted by display name
        MetadataManager.LibraryConfig lib1 = libraries.get(0);
        assertEquals("com.example", lib1.groupId);
        assertEquals("my-lib", lib1.artifactId);
        assertEquals(1, lib1.versions.size());
        assertEquals("1.0.0", lib1.latestVersion);

        MetadataManager.LibraryConfig lib2 = libraries.get(1);
        assertEquals("org.test", lib2.groupId);
        assertEquals("test-lib", lib2.artifactId);
        assertEquals(2, lib2.versions.size());
        assertEquals("2.1.0", lib2.latestVersion);
    }

    @Test
    public void testAutoDetectLibraries() throws IOException {
        MetadataManager manager = new MetadataManager();

        // Create a mock target JAR with pom.properties
        File mockJar = new File(tempDir.toFile(), "target.jar");
        try (ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(mockJar))) {
            zout.putNextEntry(new ZipEntry("META-INF/maven/com.example/my-lib/pom.properties"));
            Properties props = new Properties();
            props.setProperty("groupId", "com.example");
            props.setProperty("artifactId", "my-lib");
            props.store(zout, null);
            zout.closeEntry();
        }

        // Mock available libraries list
        MetadataManager.LibraryConfig mockLib = new MetadataManager.LibraryConfig("com.example", "my-lib");
        List<MetadataManager.LibraryConfig> availableLibs = List.of(mockLib);

        List<MetadataManager.LibraryConfig> detected = manager.autoDetectLibraries(mockJar, availableLibs);

        assertEquals(1, detected.size());
        assertEquals(mockLib, detected.get(0));
    }
}
