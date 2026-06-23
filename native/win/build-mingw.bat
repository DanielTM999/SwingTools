@echo off
setlocal EnableDelayedExpansion

if "%JAVA_HOME%"=="" (
    echo ERROR: JAVA_HOME is not set
    exit /b 1
)

set GPP=g++
where g++ >nul 2>nul
if errorlevel 1 (
    if exist "C:\ProgramData\mingw64\mingw64\bin\g++.exe" (
        set "GPP=C:\ProgramData\mingw64\mingw64\bin\g++.exe"
        set "PATH=C:\ProgramData\mingw64\mingw64\bin;%PATH%"
    ) else if exist "C:\tools\mingw64\bin\g++.exe" (
        set "GPP=C:\tools\mingw64\bin\g++.exe"
        set "PATH=C:\tools\mingw64\bin;%PATH%"
    ) else (
        echo ERROR: g++ not found in PATH or default MinGW locations.
        echo         Install MinGW-w64 or open a new terminal.
        exit /b 1
    )
)

set OUT_DIR=%~dp0build
if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"
set OUT_FILE=%OUT_DIR%\osfilepicker.dll

"%GPP%" -std=c++17 -O2 -shared -Wl,--kill-at ^
    -static-libgcc -static-libstdc++ ^
    -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" ^
    "%~dp0OsFilePicker.cpp" ^
    -o "%OUT_FILE%" ^
    -lole32 -luuid -lshell32

if errorlevel 1 (
    echo Build FAILED
    exit /b 1
)

set "GPP_PATH="
if exist "%GPP%" (
    set "GPP_PATH=%GPP%"
) else (
    for /f "delims=" %%I in ('where "%GPP%" 2^>nul') do if not defined GPP_PATH set "GPP_PATH=%%I"
)

if defined GPP_PATH (
    for %%I in ("%GPP_PATH%") do set "GPP_DIR=%%~dpI"
    if exist "!GPP_DIR!libwinpthread-1.dll" (
        copy /Y "!GPP_DIR!libwinpthread-1.dll" "%OUT_DIR%\libwinpthread-1.dll" >nul
        echo Copied: %OUT_DIR%\libwinpthread-1.dll
    ) else (
        echo WARNING: libwinpthread-1.dll not found next to g++. osfilepicker.dll may depend on PATH.
    )
) else (
    echo WARNING: Could not resolve g++ path. osfilepicker.dll may depend on PATH.
)

echo Built: %OUT_FILE%
endlocal
