@echo off
REM Run script for SecureVault Password Manager on Windows
REM Requires JDK 17+ with JavaFX SDK

set JAR=%~dp0password-manager-1.0.0.jar
set FX_PATH=C:\javafx-sdk\lib

IF EXIST "%FX_PATH%" (
    java --module-path "%FX_PATH%" ^
         --add-modules javafx.controls,javafx.fxml ^
         -jar "%JAR%"
) ELSE (
    REM Try plain launch (works if JavaFX is on the classpath)
    java -jar "%JAR%"
)
