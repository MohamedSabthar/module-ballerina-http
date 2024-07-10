package io.ballerina.stdlib.http.transport.message;

public class SseEventParser {
    public static SseEvent parse(String event) {
        if (event.isEmpty()) {
            return new SseEvent(null);
        }

        String eventId = null;
        String eventName = null;
        boolean hasData = false;
        String eventRetryValue = null;
        String commentValue = null;
        StringBuilder dataBuilder = new StringBuilder();

        String[] lines = event.split(System.lineSeparator());

        for (String line : lines) {
            if (line.isBlank() || line.isEmpty()) {
                continue;
            }
            String[] tokens = line.split(":", 2);
            String fieldKey = tokens[0] = tokens[0].trim();
            String fieldValue = tokens[1].startsWith(" ") ? tokens[1].substring(1) : tokens[1];
            if (fieldKey.trim().equals(SseFieldNames.ID.toString())) {
                eventId = fieldValue;
            } else if (fieldKey.trim().equals(SseFieldNames.EVENT.toString())) {
                eventName = fieldValue;
            } else if (fieldKey.trim().equals(SseFieldNames.RETRY.toString())) {
                eventRetryValue = fieldValue;
            } else if (fieldKey.trim().equals(SseFieldNames.COMMENT.toString())) {
                commentValue = fieldValue;
            } else if (fieldKey.trim().equals(SseFieldNames.DATA.toString())) {
                hasData = true;
                if (!dataBuilder.isEmpty()) {
                    dataBuilder.append(System.lineSeparator());
                }
                dataBuilder.append(fieldValue.stripLeading());
            }
        }
        return new SseEvent(eventName, eventId, hasData ? dataBuilder.toString() : null, eventRetryValue, commentValue);
    }
}
