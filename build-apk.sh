#!/usr/bin/env bash
# Builds Sprout IDE release APK and copies it to D:\expensive\out
set -euo pipefail
TASK="${1:-:app:assembleRelease}"
export JAVA_HOME='D:\expensive\tools\jdk17'
export ANDROID_HOME='D:\expensive\tools\sdk'
export GRADLE_USER_HOME='D:\expensive\tools\gradle-home'
export TMP='D:\expensive\tools\tmp' TEMP='D:\expensive\tools\tmp'
cd /d/OtherExplorer
/d/expensive/tools/jdk17/bin/java.exe -Xmx64m -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain "$TASK" --console=plain
[[ "$TASK" == *assemble* ]] || exit 0
apk=$(ls -t app/build/outputs/apk/release/*.apk | head -1)
mkdir -p /d/expensive/out
version=$(grep -o 'versionName = "[^"]*"' app/build.gradle.kts | cut -d'"' -f2)
dest="/d/expensive/out/SproutIDE-${version}-$(date +%Y%m%d-%H%M).apk"
cp "$apk" "$dest"
echo "APK: $(cygpath -w "$dest")"
