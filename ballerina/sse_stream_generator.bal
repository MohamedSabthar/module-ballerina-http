
class SseStreamGenerator {
    private boolean isClosed = false;

    public isolated function next(typedesc<record {|SseEvent value;|}> t = <>) returns t|error? = external;

    public isolated function close() returns error? {
        self.isClosed = true;
    }
}
