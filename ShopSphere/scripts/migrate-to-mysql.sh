#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
./mvnw -q -DskipTests package dependency:build-classpath \
  -Dmdep.outputFile=target/classpath.txt -DincludeScope=compile
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}java"
if [ -z "${JAVA_HOME:-}" ] && [ -x /opt/homebrew/opt/openjdk@17/bin/java ]; then
  JAVA_BIN=/opt/homebrew/opt/openjdk@17/bin/java
fi
exec "$JAVA_BIN" -cp "target/classes:$(cat target/classpath.txt)" \
  com.shopsphere.util.MigrateH2ToMySql
