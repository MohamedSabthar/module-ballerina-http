// Copyright (c) 2024 WSO2 LLC. (https://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/io;
import ballerina/log;

const LINE_BREAK = "\n";

class SseEventToByteStreamGenerator {
    private final stream<SseEvent, error?> eventStream;
    private boolean isClosed = false;

    isolated function init(stream<SseEvent, error?> eventStream) {
        self.eventStream = eventStream;
    }

    public isolated function next() returns record {|byte[] value;|}|io:Error? {
        if self.isClosed {
            return ();
        }
        do {
            record {SseEvent value;}? event = check self.eventStream.next();
            if event is () {
                check self.close();
                return ();
            }
            check validateSseEvent(event.value);
            string eventText = getEventText(event.value);
            return {value: eventText.toBytes()};
        } on fail error e {
            io:Error err = error io:Error("Unable to obtain byte array", e);
            log:printError(err.message(), e);
            return err;
        }
    }

    public isolated function close() returns error? {
        check self.eventStream.close();
        self.isClosed = true;
    }
}

isolated function validateSseEvent(SseEvent event) returns error? {
    if !event.hasKey("event") && !event.hasKey("id")
        && !event.hasKey("retry") && !event.hasKey("comment")
        && !event.hasKey("data") {
        return error("Invalid value provided as event: "
                + "at least one field is expected to be present in the SseEvent record.");
    }
}

isolated function getEventText(SseEvent event) returns string {
    string eventText = "";
    string? comment = event.comment;
    if comment is string {
        eventText += string `: ${comment}` + LINE_BREAK;
    }
    string? id = event.id;
    if id is string {
        eventText += string `${ID}: ${id}` + LINE_BREAK;
    }
    string? eventName = event.event;
    if eventName is string {
        eventText += string `${EVENT}: ${eventName}` + LINE_BREAK;
    }
    int? 'retry = event.'retry;
    if 'retry is int {
        eventText += string `${RETRY}: ${'retry.toString()}` + LINE_BREAK;
    }
    string? data = event.data;
    if data is string {
        string[] lines = re `\r\n|\n|\r`.split(data);
        foreach string line in lines {
            eventText += string `${DATA}: ${line}` + LINE_BREAK;
        }
    }
    eventText += LINE_BREAK;
    return eventText;
}
