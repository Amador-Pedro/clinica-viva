#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build/classes
find src -name '*.java' > build/sources.txt
javac --release 17 -encoding UTF-8 -d build/classes @build/sources.txt
jar --create --file build/clinica-viva.jar --main-class br.edu.ceub.clinica.Main -C build/classes .
if [[ "${1:-}" == "test" ]]; then
    mkdir -p build/test
    find test -name '*.java' > build/tests.txt
    javac --release 17 -encoding UTF-8 -cp build/classes -d build/test @build/tests.txt
    java -Djava.awt.headless=true -cp build/classes:build/test br.edu.ceub.clinica.ClinicaTest
elif [[ "${1:-}" == "run" ]]; then
    java -jar build/clinica-viva.jar
fi
