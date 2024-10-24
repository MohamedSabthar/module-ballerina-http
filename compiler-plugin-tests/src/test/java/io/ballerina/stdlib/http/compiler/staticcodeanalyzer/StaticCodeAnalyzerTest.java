/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.http.compiler.staticcodeanalyzer;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.testng.internal.ExitCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class includes tests for Ballerina Http static code analyzer.
 */
public class StaticCodeAnalyzerTest {

    private static final Path RESOURCE_PACKAGES_DIRECTORY = Paths
            .get("src", "test", "resources", "static_code_analyzer", "packages").toAbsolutePath();
    private static final Path EXPECTED_JSON_OUTPUT_DIRECTORY = Paths.
            get("src", "test", "resources", "static_code_analyzer", "expected_output").toAbsolutePath();
    private static final Path BALLERINA_PATH = Paths
            .get("../", "target", "ballerina-runtime", "bin", "bal").toAbsolutePath();

    @BeforeSuite
    public void pullScanTool() throws IOException, InterruptedException {
        String scanToolWithVersion = "scan:0.1.0";
        ProcessBuilder processBuilder = new ProcessBuilder(BALLERINA_PATH.toString(), "tool", "pull", scanToolWithVersion, "--repository=local");
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        String output = convertInputStreamToString(process.getInputStream());
        if (output.startsWith("tool '" + scanToolWithVersion + "' is already active.")) {
            return;
        }
        Assert.assertFalse(ExitCode.hasFailure(exitCode));
    }

    @Test
    public void testTool() throws IOException, InterruptedException {
        String output = executeScanProcess("rule001");
        Pattern pattern = Pattern.compile("\\[[^]]*]");
        Matcher match = pattern.matcher(output);
        Assert.assertTrue(match.find());
        String jsonOutput = match.group();
        String expectedJson = Files.readString(EXPECTED_JSON_OUTPUT_DIRECTORY.resolve("rule001.json"));
        assertJsonEqual(jsonOutput, expectedJson);
    }

    public static String executeScanProcess(String targetPackage) throws IOException, InterruptedException {
        ProcessBuilder processBuilder2 = new ProcessBuilder(BALLERINA_PATH.toString(), "scan");
        processBuilder2.directory(RESOURCE_PACKAGES_DIRECTORY.resolve(targetPackage).toFile());
        Process process2 = processBuilder2.start();
        int exitCode = process2.waitFor();
        Assert.assertFalse(ExitCode.hasFailure(exitCode));
        return convertInputStreamToString(process2.getInputStream());
    }

    public static String convertInputStreamToString(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append(System.lineSeparator());
            }
        }
        return stringBuilder.toString();
    }

    private void assertJsonEqual(String actual, String expected) {
        Assert.assertEquals(normalizeJson(actual), normalizeJson(expected));
    }

    private static String normalizeJson(String json) {
        return json.replaceAll("\\s*\"\\s*", "\"")
                .replaceAll("\\s*:\\s*", ":")
                .replaceAll("\\s*,\\s*", ",")
                .replaceAll("\\s*\\{\\s*", "{")
                .replaceAll("\\s*}\\s*", "}")
                .replaceAll("\\s*\\[\\s*", "[")
                .replaceAll("\\s*]\\s*", "]")
                .replaceAll("\n", "");
    }
}
