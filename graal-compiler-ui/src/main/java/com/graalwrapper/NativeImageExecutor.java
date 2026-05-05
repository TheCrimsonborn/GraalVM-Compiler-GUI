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
     * @param classPath    The full classpath including the target jar and any external folders
     * @param mainClass    The fully qualified name of the main class
     * @param workingDir   The working directory for the process
     * @param packageStandalone Whether to package the output as a standalone AWT/Swing app
     * @param listener     The callback listener for logs and process events
     */
    @SuppressWarnings("squid:S107") // Ignore too many parameters warning to keep code simple
    public void execute(String vcvarsPath, String javaHome, String options, String classPath, String mainClass, File workingDir, boolean packageStandalone, LogListener listener) {
        new Thread(() -> {
            try {
                // Construct the compound cmd command using absolute path to the binary in the portable folder
                String nativeImageBin = javaHome + "\\bin\\native-image.cmd";
                String command = String.format(
                        "call \"%s\" && set \"JAVA_HOME=%s\" && \"%s\" %s -cp \"%s\" %s",
                        vcvarsPath, javaHome, nativeImageBin, options == null ? "" : options, classPath, mainClass
                );

                listener.onLogMessage("Starting build process...");
                listener.onLogMessage("Command: cmd.exe /c " + command);

                int exitCode = runProcess(command, workingDir, listener);
                
                if (exitCode == 0 && packageStandalone) {
                    packageAsStandalone(javaHome, workingDir, listener);
                }
                
                listener.onProcessComplete(exitCode);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                listener.onProcessFailed(e);
            } catch (IOException e) {
                listener.onProcessFailed(e);
            }
        }, "NativeImage-Execution-Thread").start();
    }

    /**
     * Executes the target jar with the native-image-agent to generate configurations.
     *
     * @param javaHome     Path to the GraalVM installation directory
     * @param agentArgs    Additional JVM arguments for the agent process
     * @param classPath    The full classpath including the target jar and any external folders
     * @param mainClass    The fully qualified name of the main class
     * @param workingDir   The working directory for the process
     * @param listener     The callback listener for logs and process events
     */
    public void executeAgent(String javaHome, String agentArgs, String classPath, String mainClass, File workingDir, boolean mergeConfigs, LogListener listener) {
        new Thread(() -> {
            try {
                String javaBin = javaHome + "\\bin\\java.exe";
                java.util.List<String> cmdList = new java.util.ArrayList<>();
                cmdList.add(javaBin);
                if (mergeConfigs) {
                    cmdList.add("-agentlib:native-image-agent=config-merge-dir=native-image-configs");
                } else {
                    cmdList.add("-agentlib:native-image-agent=config-output-dir=native-image-configs");
                }
                
                if (agentArgs != null && !agentArgs.trim().isEmpty()) {
                    // Simple split by space (does not handle quoted spaces)
                    String[] args = agentArgs.trim().split("\\s+");
                    java.util.Collections.addAll(cmdList, args);
                }
                
                cmdList.add("-cp");
                cmdList.add(classPath);
                cmdList.add(mainClass);
                
                ProcessBuilder pb = new ProcessBuilder(cmdList);
                if (workingDir != null) {
                    pb.directory(workingDir);
                }

                listener.onLogMessage("Starting Tracing Agent process...");
                listener.onLogMessage("Please interact with your application and then close it normally to generate configurations.");
                listener.onLogMessage("Command: " + String.join(" ", pb.command()));

                Process process = pb.start();

                // Thread for reading Standard Output
                Thread stdoutThread = new Thread(() -> readStream(process.getInputStream(), listener));
                stdoutThread.setName("TracingAgent-Stdout-Thread");
                
                // Thread for reading Standard Error
                Thread stderrThread = new Thread(() -> readStream(process.getErrorStream(), listener));
                stderrThread.setName("TracingAgent-Stderr-Thread");

                stdoutThread.start();
                stderrThread.start();

                stdoutThread.join();
                stderrThread.join();

                int exitCode = process.waitFor();
                listener.onProcessComplete(exitCode);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                listener.onProcessFailed(e);
            } catch (IOException e) {
                listener.onProcessFailed(e);
            }
        }, "TracingAgent-Execution-Thread").start();
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

    private void packageAsStandalone(String javaHome, File workingDir, LogListener listener) throws IOException, InterruptedException {
        listener.onLogMessage("\nPackaging as Standalone Application...");
        File distDir = new File(workingDir, "Standalone_Distribution");
        if (!distDir.exists() && !distDir.mkdirs()) {
            listener.onLogMessage("Failed to create Standalone_Distribution folder.");
            return;
        }
        
        // Move exe and dlls
        String moveCmd = String.format("move /Y \"*.exe\" \"%s\\\" && move /Y \"*.dll\" \"%s\\\"", distDir.getAbsolutePath(), distDir.getAbsolutePath());
        // Copy lib
        String copyLibCmd = String.format("xcopy /E /I /Y \"%s\\lib\" \"%s\\lib\"", javaHome, distDir.getAbsolutePath());
        
        // Create run_app.bat
        File runBat = new File(distDir, "run_app.bat");
        try (java.io.PrintWriter out = new java.io.PrintWriter(runBat)) {
            out.println("@echo off");
            out.println("cd /d \"%~dp0\"");
            out.println("echo Starting Application...");
            out.println("for %%f in (*.exe) do set APP_EXE=%%f");
            out.println("start \"\" \"%APP_EXE%\" -Djava.home=\".\" -Djava.library.path=\".;natives\"");
            out.println("exit");
        }
        
        listener.onLogMessage("Moving binaries and copying GraalVM lib folder. This might take a moment...");
        String packageCommand = moveCmd + " && " + copyLibCmd;
        int pkgExit = runProcess(packageCommand, workingDir, listener);
        
        if (pkgExit == 0) {
            listener.onLogMessage("Standalone packaging complete! Folder: " + distDir.getAbsolutePath());
        } else {
            listener.onLogMessage("Warning: Packaging command failed with exit code " + pkgExit);
        }
    }
}
