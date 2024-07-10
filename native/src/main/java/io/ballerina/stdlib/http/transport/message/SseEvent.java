package io.ballerina.stdlib.http.transport.message;

public class SseEvent {
    private final String event;
    private final String id;
    private final String data;
    private final Integer retry;
    private final String comment;

    SseEvent(String event, String id, String data, String retry, String comment) {
        this.event = event;
        this.id = id;
        this.data = data;
        this.retry = retry == null ? null : Integer.parseInt(retry);
        this.comment = comment;
    }

    SseEvent(String data) {
        this(null, null, data, null, null);
    }

    public String getEvent() {
        return event;
    }

    public String getId() {
        return id;
    }

    public String getData() {
        return data;
    }

    public int getRetry() {
        return retry;
    }

    public String getComment() {
        return comment;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (comment != null) {
            sb.append(":").append(comment).append(System.lineSeparator());
        }
        if (event != null) {
            sb.append(SseFieldNames.EVENT).append(": ").append(event).append(System.lineSeparator());
        }
        if (id != null) {
            sb.append(SseFieldNames.ID).append(": ").append(id).append(System.lineSeparator());
        }
        if (retry != null) {
            sb.append(SseFieldNames.RETRY).append(": ").append(retry).append(System.lineSeparator());
        }
        data.lines().forEach(line -> sb.append(SseFieldNames.DATA).append(": ").append(line).append(System.lineSeparator()));
        sb.append(System.lineSeparator());
        return sb.toString();
    }
}
