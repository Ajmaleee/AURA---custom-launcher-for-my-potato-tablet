#!/bin/sh
# Optional Gradle-free build using raw SDK tools. UNTESTED: written without an SDK available.
# Needs: ANDROID_HOME (platform >= 19 + build-tools), a JDK, zip.
set -e
: "${ANDROID_HOME:?set ANDROID_HOME}"
BT=${BUILD_TOOLS:-$(ls "$ANDROID_HOME/build-tools" | sort -V | tail -1)}
PL=${PLATFORM:-$(ls "$ANDROID_HOME/platforms" | sort -V | tail -1)}
JAR=$ANDROID_HOME/platforms/$PL/android.jar
T=$ANDROID_HOME/build-tools/$BT
O=build; rm -rf $O; mkdir -p $O/res $O/cls $O/dex
$T/aapt2 compile --dir app/src/main/res -o $O/res
$T/aapt2 link -o $O/base.apk -I "$JAR" --manifest app/src/main/AndroidManifest.xml \
  --min-sdk-version 19 --target-sdk-version 19 --version-code 1 --version-name 1.0 $O/res/*.flat
javac -source 8 -target 8 -bootclasspath "$JAR" -d $O/cls $(find app/src/main/java -name '*.java')
$T/d8 --min-api 19 --lib "$JAR" --output $O/dex $(find $O/cls -name '*.class')
cp $O/base.apk $O/aura-unsigned.apk
(cd $O/dex && zip -q ../aura-unsigned.apk classes.dex)
$T/zipalign -f 4 $O/aura-unsigned.apk $O/aura-aligned.apk
[ -f debug.keystore ] || keytool -genkeypair -keystore debug.keystore -storepass android -keypass android \
  -alias aura -dname "CN=AURA" -keyalg RSA -keysize 2048 -validity 10000
$T/apksigner sign --ks debug.keystore --ks-pass pass:android --out $O/aura-debug.apk $O/aura-aligned.apk
echo "Built $O/aura-debug.apk"
