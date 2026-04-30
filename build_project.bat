@echo off
setlocal
echo ===========================================
echo GraalVM Wrapper - Otomatik Build Araci
echo ===========================================

:: Proje klasorune gir
cd graal-compiler-ui

echo Maven build baslatiliyor (SSL Bypass Aktif)...

:: Sertifika hatasini asan ozel Maven komutu
call mvn clean package "-Dmaven.resolver.transport=wagon" "-Dmaven.wagon.http.ssl.insecure=true" "-Dmaven.wagon.http.ssl.allowall=true" "-Dmaven.wagon.http.ssl.ignore.validity.dates=true"

if %ERRORLEVEL% equ 0 (
    echo.
    echo [BASARILI] Proje paketlendi! 
    echo Dosya: graal-compiler-ui\target\graal-compiler-ui-jar-with-dependencies.jar
) else (
    echo.
    echo [HATA] Build sirasinda bir sorun olustu.
)

echo ===========================================
pause
endlocal
