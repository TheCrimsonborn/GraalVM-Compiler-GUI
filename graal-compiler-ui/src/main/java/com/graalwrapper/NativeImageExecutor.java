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
    public void execute(String vcvarsPath, String javaHome, String options, String classPath, String mainClass, File workingDir, boolean packageStandalone, String targetOs, LogListener listener) {
        new Thread(() -> {
            try {
                String command;
                if ("Linux (ELF via Docker)".equals(targetOs)) {
                    if (!ensureDockerImageExists(workingDir, listener)) {
                        listener.onProcessFailed(new IOException("Docker image initialization failed."));
                        return;
                    }

                    String linuxCp = translateToLinuxPath(classPath, workingDir);
                    String dockerOptions = translateToLinuxPath(options == null ? "" : options, workingDir);
                    
                    command = String.format(
                            "docker run --rm -v \"%s:/app\" -w /app ghcr.io/graalvm/native-image:ol8-java11-22.3.3 %s -cp \"%s\" %s",
                            workingDir.getAbsolutePath(), dockerOptions, linuxCp, mainClass
                    );
                    listener.onLogMessage("Starting Docker Build Process for Linux...");
                } else {
                    String nativeImageBin = javaHome + "\\bin\\native-image.cmd";
                    command = String.format(
                            "call \"%s\" && set \"JAVA_HOME=%s\" && \"%s\" %s -cp \"%s\" %s",
                            vcvarsPath, javaHome, nativeImageBin, options == null ? "" : options, classPath, mainClass
                    );
                    listener.onLogMessage("Starting build process for Windows...");
                }

                listener.onLogMessage("Command: " + ("Linux (ELF via Docker)".equals(targetOs) ? command : "cmd.exe /c " + command));

                int exitCode = runProcess(command, workingDir, listener);
                
                if (exitCode == 0 && packageStandalone && !"Linux (ELF via Docker)".equals(targetOs)) {
                    packageAsStandalone(javaHome, workingDir, listener);
                } else if (exitCode == 0 && "Linux (ELF via Docker)".equals(targetOs)) {
                    listener.onLogMessage("\nLinux build complete! You will find the ELF executable in: " + workingDir.getAbsolutePath());
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

    private String translateToLinuxPath(String input, File workingDir) {
        if (input == null || input.isEmpty()) return input;
        String workDirPath = workingDir.getAbsolutePath();
        String result = input.replace(workDirPath, "/app");
        result = result.replace("\\", "/");
        // Also handle the classpath separator (; to :)
        if (result.contains(";")) {
            result = result.replace(";", ":");
        }
        return result;
    }

    private boolean ensureDockerImageExists(File workingDir, LogListener listener) {
        String imageName = "ghcr.io/graalvm/native-image:ol8-java11-22.3.3";
        try {
            Process process = new ProcessBuilder("docker", "images", "-q", imageName).start();
            java.util.Scanner s = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A");
            String output = s.hasNext() ? s.next().trim() : "";
            process.waitFor();

            if (!output.isEmpty()) {
                return true;
            }

            listener.onLogMessage("\nOffline Docker Image not found on system. Extracting and installing from embedded resources...");
            
            java.io.InputStream is = getClass().getResourceAsStream("/native-image-docker.tar");
            if (is == null) {
                listener.onLogMessage("Error: native-image-docker.tar not found in resources!");
                return false;
            }

            File tempTar = new File(workingDir, "temp-docker-image.tar");
            listener.onLogMessage("Extracting ~400MB image to disk. Please wait...");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempTar)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
            }

            listener.onLogMessage("Loading Docker image into daemon. This may take a few minutes...");
            int loadExit = runProcess("docker load -i \"" + tempTar.getAbsolutePath() + "\"", workingDir, listener);
            
            tempTar.delete();

            if (loadExit == 0) {
                listener.onLogMessage("Offline Docker Image successfully installed!");
                return true;
            } else {
                listener.onLogMessage("Error loading Docker image. Exit code: " + loadExit);
                return false;
            }

        } catch (Exception e) {
            listener.onLogMessage("Error checking/loading Docker image: " + e.getMessage());
            return false;
        }
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
