@echo off
setlocal

echo ==============================================================
echo GraalVM Offline Docker Image Downloader
echo ==============================================================
echo.
echo This script will download the ghcr.io/graalvm/native-image:ol8-java11-22.3.3
echo Docker image and save it as a .tar file into the resources folder.
echo This file is required for the "Linux (ELF via Docker)" build feature
echo to work completely offline (Airgapped).
echo.
echo Please ensure Docker Desktop is running.
echo.
pause

echo.
echo [1/2] Pulling the Docker Image from GitHub Container Registry...
docker pull ghcr.io/graalvm/native-image:ol8-java11-22.3.3
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Failed to pull the Docker image. Please check your internet connection and Docker status.
    pause
    exit /b %errorlevel%
)

echo.
echo [2/2] Saving the Docker Image to src/main/resources/native-image-docker.tar ...
echo This might take a few minutes as the image is ~400MB. Please wait...
docker save -o "graal-compiler-ui\src\main\resources\native-image-docker.tar" ghcr.io/graalvm/native-image:ol8-java11-22.3.3
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Failed to save the Docker image.
    pause
    exit /b %errorlevel%
)

echo.
echo ==============================================================
echo [SUCCESS] Offline Docker Image has been successfully saved!
echo Path: graal-compiler-ui\src\main\resources\native-image-docker.tar
echo.
echo You can now run build_project.bat to embed this file into the final application.
echo ==============================================================
pause
endlocal
