package com.graalwrapper;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class NativeImageExecutorTest {

    @Test
    public void testTranslateToLinuxPath() {
        NativeImageExecutor executor = new NativeImageExecutor();
        File workingDir = new File("C:\\myproject");
        String workingDirPath = workingDir.getAbsolutePath();

        // Null input
        assertNull(executor.translateToLinuxPath(null, workingDir));

        // Empty input
        assertEquals("", executor.translateToLinuxPath("", workingDir));

        // Path within working directory
        String inputPath = workingDirPath + "\\target\\myapp.jar";
        String expectedLinuxPath = "/app/target/myapp.jar";
        assertEquals(expectedLinuxPath, executor.translateToLinuxPath(inputPath, workingDir));

        // Classpath with multiple entries
        String classPath = workingDirPath + "\\target\\myapp.jar;" + workingDirPath + "\\lib\\dep.jar";
        String expectedClassPath = "/app/target/myapp.jar:/app/lib/dep.jar";
        assertEquals(expectedClassPath, executor.translateToLinuxPath(classPath, workingDir));
    }
}
