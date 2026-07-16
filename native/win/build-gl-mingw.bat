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
set OUT_FILE=%OUT_DIR%\graphicsgl.dll

"%GPP%" -std=c++17 -O2 -shared -static -Wl,--kill-at ^
    -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" ^
    -I"%~dp0..\shared" ^
    "%~dp0GraphicsGl.cpp" "%~dp0..\shared\GlBindings.cpp" ^
    -o "%OUT_FILE%" ^
    "%JAVA_HOME%\bin\jawt.dll" ^
    -lopengl32 -lgdi32

if errorlevel 1 (
    echo Build FAILED
    exit /b 1
)

echo Built: %OUT_FILE%
endlocal
