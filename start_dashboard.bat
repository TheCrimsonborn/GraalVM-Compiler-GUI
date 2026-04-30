@echo off
setlocal

:: Define GRAALVM_HOME relative to the batch file location
set "GRAALVM_HOME=.\graalvm-ce-java11-22.3.3"

:: Execute the Java UI using javaw to prevent the console window from staying open
start "" "%GRAALVM_HOME%\bin\javaw.exe" -jar "graal-compiler-ui\target\graal-compiler-ui-jar-with-dependencies.jar"

endlocal
