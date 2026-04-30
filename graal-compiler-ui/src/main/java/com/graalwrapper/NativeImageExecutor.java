package com.graalwrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

public class NativeImageExecutor {

    /**
     * Callback interface to receive real-time updates from the native-image build process.
     */
    public interface LogListener {
        void onLogMessage(String message);
        void onProcessComplete(int exitCode);
        void onProcessFailed(Exception e);
    }

    /**
     * Executes the native-image compilation process in a background thread.
     *
     * @param vcvarsPath   Path to the vcvars64.bat file (e.g., "C:\Program Files\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat")
     * @param javaHome     Path to the GraalVM installation directory
     * @param options      Additional options for native-image (e.g., "-O3 --no-fallback")
     * @param targetJar    Path to the target jar file to compile
     * @param workingDir   The working directory for the process
     * @param listener     The callback listener for logs and process events
     */
    public void execute(String vcvarsPath, String javaHome, String options, String targetJar, File workingDir, LogListener listener) {
        new Thread(() -> {
            try {
                // Construct the compound cmd command using absolute path to the binary in the portable folder
                String nativeImageBin = javaHome + "\\bin\\native-image.cmd";
                String command = String.format(
                        "call \"%s\" && set \"JAVA_HOME=%s\" && \"%s\" %s -jar \"%s\"",
                        vcvarsPath, javaHome, nativeImageBin, options == null ? "" : options, targetJar
                );

                listener.onLogMessage("Starting build process...");
                listener.onLogMessage("Command: cmd.exe /c " + command);

                int exitCode = runProcess(command, workingDir, listener);
                listener.onProcessComplete(exitCode);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                listener.onProcessFailed(e);
            } catch (IOException e) {
                listener.onProcessFailed(e);
            }
        }, "NativeImage-Execution-Thread").start();
    }

    private int runProcess(String command, File workingDir, LogListener listener) throws IOException, InterruptedException {
        String cmdPath = System.getenv("ComSpec");
        if (cmdPath == null) {
            cmdPath = "cmd.exe";
        }
        ProcessBuilder pb = new ProcessBuilder(cmdPath, "/c", command);
        if (workingDir != null) {
            pb.directory(workingDir);
        }

        // Start the process
        Process process = pb.start();

        // Thread for reading Standard Output
        Thread stdoutThread = new Thread(() -> readStream(process.getInputStream(), listener));
        stdoutThread.setName("NativeImage-Stdout-Thread");
        
        // Thread for reading Standard Error
        Thread stderrThread = new Thread(() -> readStream(process.getErrorStream(), listener));
        stderrThread.setName("NativeImage-Stderr-Thread");

        stdoutThread.start();
        stderrThread.start();

        // Wait for streams to finish reading
        stdoutThread.join();
        stderrThread.join();

        // Wait for the process to exit
        return process.waitFor();
    }

    /**
     * Helper method to continuously read an InputStream line by line.
     */
    private void readStream(InputStream inputStream, LogListener listener) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                listener.onLogMessage(line);
            }
        } catch (java.io.IOException e) {
            listener.onLogMessage("Stream reading error: " + e.getMessage());
        }
    }
}
