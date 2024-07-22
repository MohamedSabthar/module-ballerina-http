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

import ballerina/http;
import ballerina/lang.runtime;
import ballerina/test;
import ballerina/io;

listener http:Listener http1SseListener = new http:Listener(http1SsePort, httpVersion = http:HTTP_1_1);
listener http:Listener http2SseListener = new http:Listener(http2SsePort, httpVersion = http:HTTP_2_0);

class SseEventGenerator {
    private final int eventCount;
    private int currentEventCount = 0;

    function init(int eventCount = 10) {
        self.eventCount = eventCount;
    }

    public isolated function next() returns record {|http:SseEvent value;|}|error? {
        runtime:sleep(0.1);
        http:SseEvent sseEvent = {data: string `count: ${self.currentEventCount}`, id: self.currentEventCount.toString()};
        if self.currentEventCount == 0 {
            sseEvent.event = "start";
        } else if self.currentEventCount == self.eventCount {
            sseEvent.event = "end";
        } else {
            sseEvent.event = "continue";
            sseEvent.'retry = 10;
        }
        if self.currentEventCount > self.eventCount {
            return ();
        }
        self.currentEventCount += 1;
        return {value: sseEvent};
    }
}

service /sse on http1SseListener {
    resource function 'default [string... paths](http:Request req) returns stream<http:SseEvent, error?> {
        stream<http:SseEvent, error?> sseEventStream = new (new SseEventGenerator());
        return sseEventStream;
    }
}

service /sse on http2SseListener {
    resource function post .(http:Request req) returns http:Response {
        http:Response response = new;
        stream<http:SseEvent, error?> sseEventStream = new (new SseEventGenerator());
        response.setSseEventStream(sseEventStream);
        return response;
    }
}

final http:Client http1SseClient = check new ("http://localhost:" + http1SsePort.toString(), httpVersion = http:HTTP_1_1);

@test:Config {}
function testHttp1ResponseHeadersForSseEventStream() returns error? {
    http:Response response = check http1SseClient->/sse;
    stream<http:SseEvent, error?> actualSseEvents = check response.getSseEventStream();
    test:assertEquals(check response.getHeader("Connection"), "keep-alive");
    test:assertEquals(check response.getHeader("Content-Type"), "text/event-stream");
    test:assertEquals(check response.getHeader("Transfer-Encoding"), "chunked");
    test:assertTrue((check response.getHeader("Cache-Control")).startsWith("no-cache"), "text/event-stream");
    stream<http:SseEvent, error?> expectedSseEvents = new (new SseEventGenerator());
    check assertEventStream(actualSseEvents, expectedSseEvents);
}

@test:Config {}
function testClientDataBindingForSseEventStream() returns error? {
    stream<http:SseEvent, error?> actualSseEvents = check http1SseClient->/sse;
    stream<http:SseEvent, error?> expectedSseEvents = new (new SseEventGenerator());
    check assertEventStream(actualSseEvents, expectedSseEvents);
}

@test:Config {}
function testClientRequestMethodsWithStreamType() returns error? {
    stream<http:SseEvent, error?> actualSseEvents = check http1SseClient->/sse.get();
    stream<http:SseEvent, error?> expectedSseEvents = new (new SseEventGenerator());
    check assertEventStream(actualSseEvents, expectedSseEvents);

    actualSseEvents = check http1SseClient->/sse.delete();
    expectedSseEvents = new (new SseEventGenerator());
    check assertEventStream(actualSseEvents, expectedSseEvents);

    actualSseEvents = check http1SseClient->/sse.options();
    expectedSseEvents = new (new SseEventGenerator());
    check assertEventStream(actualSseEvents, expectedSseEvents);

    actualSseEvents = check http1SseClient->/sse.post({});
    expectedSseEvents = new (new SseEventGenerator());
    check assertEventStream(actualSseEvents, expectedSseEvents);

    actualSseEvents = check http1SseClient->/sse.put({});
    expectedSseEvents = new (new SseEventGenerator());
    check assertEventStream(actualSseEvents, expectedSseEvents);

    actualSseEvents = check http1SseClient->/sse.patch({});
    expectedSseEvents = new (new SseEventGenerator());
    check assertEventStream(actualSseEvents, expectedSseEvents);
}

function assertEventStream(stream<http:SseEvent, error?> actualSseEvents, stream<http:SseEvent, error?> expectedSseEvents) returns error? {
    check from http:SseEvent expectedEvent in expectedSseEvents
        do {
            record {|http:SseEvent value;|}? valueRecord = check actualSseEvents.next();
            http:SseEvent? actualEvent = valueRecord !is () ? valueRecord.value : valueRecord;
            io:println(actualEvent !is () ? actualEvent.data : (), expectedEvent.data);
            test:assertEquals(actualEvent, expectedEvent);
        };
}
