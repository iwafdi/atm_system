@echo off
REM Compile the ATM system with plain javac (no Maven needed).
REM Output goes to .\out, with resources copied alongside the classes.
cd /d "%~dp0"

set CP=lib\postgresql-42.7.4.jar

echo Compiling...
if exist out rmdir /s /q out
mkdir out

dir /s /b src\main\java\*.java > sources.txt
javac -cp "%CP%" -d out @sources.txt
if errorlevel 1 (
  del sources.txt
  echo Build failed.
  exit /b 1
)
del sources.txt

REM Resources (db.properties, schema.sql) must sit on the classpath.
copy /y src\main\resources\* out\ >nul

echo Done. Run with run.bat
