$ErrorActionPreference='Stop'
$testRepo="$env:USERPROFILE/.m2/repository"
$testJars=@(
'org/junit/platform/junit-platform-launcher/1.11.4/junit-platform-launcher-1.11.4.jar'
'org/junit/platform/junit-platform-engine/1.11.4/junit-platform-engine-1.11.4.jar'
'org/junit/platform/junit-platform-commons/1.11.4/junit-platform-commons-1.11.4.jar'
'org/junit/jupiter/junit-jupiter-api/5.11.4/junit-jupiter-api-5.11.4.jar'
'org/junit/jupiter/junit-jupiter-engine/5.11.4/junit-jupiter-engine-5.11.4.jar'
'org/junit/jupiter/junit-jupiter-params/5.11.4/junit-jupiter-params-5.11.4.jar'
'org/mockito/mockito-core/5.14.2/mockito-core-5.14.2.jar'
'org/mockito/mockito-junit-jupiter/5.14.2/mockito-junit-jupiter-5.14.2.jar'
'net/bytebuddy/byte-buddy/1.15.4/byte-buddy-1.15.4.jar'
'net/bytebuddy/byte-buddy-agent/1.15.4/byte-buddy-agent-1.15.4.jar'
'org/objenesis/objenesis/3.3/objenesis-3.3.jar'
'org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar'
'org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar'
)
$testCp=($testJars | ForEach-Object { Join-Path $testRepo $_ }) -join ';'
$testAgent=Join-Path $testRepo $testJars[6]
$testSources=@(rg --files src -g '*.java')
javac -encoding UTF-8 -cp $testCp -d target/tdd-local/classes @testSources target/tdd-local/ExecutarTestes.java
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
java "-javaagent:$testAgent" -cp "target/tdd-local/classes;$testCp" ExecutarTestes
exit $LASTEXITCODE
