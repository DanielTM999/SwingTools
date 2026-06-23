@echo off
setlocal

if "%JAVA_HOME%"=="" (
    echo ERROR: JAVA_HOME is not set
    exit /b 1
)

set OUT_DIR=%~dp0build
if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

set OUT_FILE=%OUT_DIR%\osfilepicker.dll

where cl >nul 2>nul
if errorlevel 1 (
    echo ERROR: MSVC 'cl' not found in PATH. Run from "x64 Native Tools Command Prompt for VS".
    exit /b 1
)

cl /nologo /LD /EHsc /std:c++17 /O2 ^
   /I"%JAVA_HOME%\include" /I"%JAVA_HOME%\include\win32" ^
   "%~dp0OsFilePicker.cpp" ^
   /Fe:"%OUT_FILE%" ^
   /link ole32.lib shell32.lib uuid.lib

if errorlevel 1 (
    echo Build FAILED
    exit /b 1
)

echo Built: %OUT_FILE%
endlocal
