import ballerina/io;
import ballerina/lang.regexp;

const byte LINE_FEED = 10;
const byte CARRIAGE_RETURN = 13;

enum FieldName {
    COMMENT = "",
    ID = "id",
    RETRY = "retry",
    EVENT = "event",
    DATA = "data"
};

class BytesToEventStreamGenerator {
    private final stream<byte[], io:Error?> byteStream;
    private boolean isClosed = false;

    // Properties obtained from last event.
    private string? lastId = ();
    private int? 'retry = ();

    isolated function init(stream<byte[], io:Error?> byteStream) {
        self.byteStream = byteStream;
    }

    public isolated function getLastId() returns string? {
        return self.lastId;
    }

    public isolated function getRetryValue() returns int? {
        return self.'retry;
    }

    public isolated function next() returns record {|SseEvent value;|}|error? {
        string? sseEvent = check self.readSseStreamUntilLineBreak();
        if sseEvent is () {
            check self.close();
            return ();
        }
        SseEvent event = check parseSseEvent(sseEvent);
        self.setProperties(event);
        return {value: event};
    }

    public isolated function close() returns error? {
        check self.byteStream.close();
        self.isClosed = true;
    }

    private isolated function readSseStreamUntilLineBreak() returns string|error? {
        byte[] buffer = [];
        byte prevByte = 0;
        byte? currentByte = ();
        boolean foundCariageReturnWithNewLine = false;
        while !self.isClosed {
            currentByte = check self.getNextByte();
            if currentByte is () {
                return ();
            }
            if foundCariageReturnWithNewLine && currentByte != CARRIAGE_RETURN {
                foundCariageReturnWithNewLine = false;
            }
            if foundCariageReturnWithNewLine {
                byte? nextByte = check self.getNextByte();
                if nextByte is byte && nextByte == LINE_FEED {
                    buffer.push(currentByte);
                    buffer.push(nextByte);
                    return string:fromBytes(buffer);
                }
            }
            if (currentByte == LINE_FEED || currentByte == CARRIAGE_RETURN) && prevByte == currentByte {
                buffer.push(currentByte);
                return string:fromBytes(buffer);
            }
            if currentByte == LINE_FEED && prevByte == CARRIAGE_RETURN {
                foundCariageReturnWithNewLine = true;
            }
            buffer.push(currentByte);
            prevByte = currentByte;
        }
        return ();
    }

    private isolated function setProperties(SseEvent event) {
        if event.id is string {
            self.lastId = event.id;
        }
        if event.'retry is int {
            self.'retry = event.'retry;
        }
    }

    private isolated function getNextByte() returns byte|error? {
        record {byte[] value;}? nextValue = check self.byteStream.next();
        return nextValue is () ? () : nextValue.value[0];
    }
}

isolated function parseSseEvent(string event) returns SseEvent|error {
    string[] lines = re `\r\n|\n|\r`.split(event);
    string? id = ();
    string? comment = ();
    string? data = ();
    int? 'retry = ();
    string? eventName = ();

    foreach string line in lines {
        if line == "" {
            continue;
        }
        regexp:Groups? groups = re `(.*?):(.*)`.findGroups(line);
        string filedName = line;
        string fieldValue = "";
        if (groups is regexp:Groups && groups.length() == 3) {
            regexp:Span? filedNameSpan = groups[1];
            regexp:Span? filedValueSpan = groups[2];
            if filedNameSpan is () || filedValueSpan is () {
                continue;
            }
            filedName = filedNameSpan.substring().trim();
            fieldValue = removeLeadingSpace(filedValueSpan.substring());
        }
        if filedName == ID {
            id = fieldValue;
        } else if filedName == COMMENT {
            comment = fieldValue;
        } else if filedName == RETRY {
            int|error retryValue = int:fromString(fieldValue);
            'retry = retryValue is error ? () : retryValue;
        } else if filedName == EVENT {
            eventName = fieldValue;
        } else if filedName == DATA {
            if data is () {
                data = fieldValue;
            } else {
                data += fieldValue;
            }
        }
    }
    SseEvent sseEvent = {data};
    if id is string {
        sseEvent.id = id;
    }
    if comment is string {
        sseEvent.comment = comment;
    }
    if 'retry is int {
        sseEvent.'retry = 'retry;
    }
    if eventName is string {
        sseEvent.event = eventName;
    }
    return sseEvent;
}

isolated function removeLeadingSpace(string line) returns string {
    return line.startsWith(" ") ? line.substring(1) : line;
}
