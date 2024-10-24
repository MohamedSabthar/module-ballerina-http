import ballerina/io;
import ballerina/lang.regexp;
import ballerina/os;
import ballerina/test;

@test:Config
isolated function testStaticCode() returns error? {
    os:Command command = {value: "../../target/ballerina-runtime/bin/bal", arguments: ["scan"]};
    os:Process process = check os:exec(command);
    byte[] output = check process.output(io:stdout);
    string out = check string:fromBytes(output);
    regexp:Span? jsonArray = re `\[[^\]]*\]`.find(out);
    if jsonArray is () {
        return error("invalid");
    }
    json jsonOutput = check jsonArray.substring().fromJsonString();
    io:println(jsonOutput);
}
