@echo off
setlocal ENABLEEXTENSIONS

set java_path=%JAVA_HOME%

if "%java_path%" == "" (
    @echo JAVA_HOME is not set, searching registry for Java.
    goto RegistryCheck
) else (
    goto JAVA_INSTALLED
)

:RegistryCheck
set KEY_NAME="HKEY_LOCAL_MACHINE\Software\JavaSoft\JDK"
set VALUE_NAME=CurrentVersion

FOR /F "usebackq skip=2 tokens=1-3" %%A IN (`REG QUERY %KEY_NAME% /v %VALUE_NAME% 2^>nul`) DO (
    set Version=
    for /f "delims=. tokens=1-3" %%a in ("%%C") do (
      set Version.Major=%%a
      set Version.Minor=%%b
      set Version.Build=%%c
    )
    set ValueName=%%A
    set ValueType=%%B
    set ValueValue=%%C
)

IF "%ValueValue%" == "" (
    goto JAVA_NOT_INSTALLED
)

IF "%Version.Major%" LSS "11" (
    goto JAVA_NOT_INSTALLED
)

SET KEY_NAME="%KEY_NAME:~1,-1%\%ValueValue%"
SET VALUE_NAME=JavaHome
if defined ValueName (
    FOR /F "usebackq skip=2 tokens=1,2*" %%A IN (`REG QUERY %KEY_NAME% /v %VALUE_NAME% 2^>nul`) DO (
        set ValueName2=%%A
        set ValueType2=%%B
        set ValueValue2=%%C
    )
)
IF "%ValueValue2%" == "" (
    goto JAVA_NOT_INSTALLED
) else (
    set java_path=%ValueValue2%
    goto JAVA_INSTALLED
)

:JAVA_NOT_INSTALLED
echo Java 11 or higher is required and not installed on this machine. Please install Java 11 or higher (https://www.oracle.com/java/technologies/downloads).
exit /b 1

:JAVA_INSTALLED
"%java_path%\bin\java.exe" -jar "selenium-server-4.18.1.jar" standalone