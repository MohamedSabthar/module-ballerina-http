import ballerina/io;

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
            string eventText = getEventText(event.value);
            return {value: eventText.toBytes()};
        } on fail error e {
            return error io:Error("Unable to obtain byte array", e);
        }
    }

    public isolated function close() returns error? {
        check self.eventStream.close();
        self.isClosed = true;
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
        eventText += string `id: ${id}` + LINE_BREAK;
    }
    string? eventName = event.event;
    if eventName is string {
        eventText += string `event: ${eventName}` + LINE_BREAK;
    }
    int? 'retry = event.'retry;
    if 'retry is int {
        eventText += string `retry: ${'retry.toString()}` + LINE_BREAK;
    }
    anydata data = event.data;
    if data !is () {
        string dataString = data.toString();
        string[] lines = re `\r\n|\n|\r`.split(dataString);
        foreach string line in lines {
            eventText += string `data: ${line}` + LINE_BREAK;
        }
    } else {
        eventText += string `data: ` + LINE_BREAK;
    }
    eventText += "\n";
    return eventText;
}
