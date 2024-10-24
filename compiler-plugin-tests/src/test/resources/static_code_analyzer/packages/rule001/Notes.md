1. Set the JAVA_HOME and BALLERINA_HOME properly
    - BALLERINA_HOME=/Library/Ballerina  
2. Use distribution version 2201.10.1
3. Build the scan tool project: https://github.com/MohamedSabthar/static-code-analysis-tool/tree/sca
4. Publish the scan tool to local repository
    - bal pack & bal push --repoistory=local
5. Pull from local repository
    - bal tool pull scan --repoistory=local
6. Publish it to local maven
7. Add a as dependency implementation in ballerina standard library module's compiler plugin
    -     implementation group: 'io.ballerina.scan', name: 'scan-command', version: '0.1.0'
8.     requires io.ballerina.scan; in compilier plugin module.java file