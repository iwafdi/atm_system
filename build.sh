#!/usr/bin/env bash
# Compile the ATM system with plain javac (no Maven needed).
# Output goes to ./out, with resources copied alongside the classes.
set -e
cd "$(dirname "$0")"

CP="lib/postgresql-42.7.4.jar"

echo "Compiling..."
rm -rf out
mkdir -p out
find src/main/java -name '*.java' > sources.txt
javac -cp "$CP" -d out @sources.txt
rm -f sources.txt

# Resources (db.properties, schema.sql) must sit on the classpath.
cp src/main/resources/* out/

echo "Done. Run with ./run.sh"
