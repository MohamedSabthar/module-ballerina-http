package io.ballerina.stdlib.http.transport.message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class SseEventStream {
    private final BlockingDeque<Character> sseCharStream = new LinkedBlockingDeque<>();

    public void addSseEvent(String text) {
        char[] chars = text.toCharArray();
        List<Character> charList = new ArrayList<>(chars.length);
        for (char c : chars) {
            charList.add(c);
        }
        this.sseCharStream.addAll(charList);
    }

    private String readNextSseEvent() throws InterruptedException {
        StringBuilder eventBuilder = new StringBuilder();
        Character prevChar = this.sseCharStream.take();
        eventBuilder.append(prevChar);
        while (true) {
            Character currentChar = this.sseCharStream.take();
            eventBuilder.append(currentChar);
            if ((prevChar == '\n' || prevChar == '\r') && prevChar == currentChar) {
                return eventBuilder.toString();
            }
            if (prevChar == '\r' && currentChar == '\n') {
                Character nextFirstChar = this.sseCharStream.take();
                Character nextSecondChar = this.sseCharStream.take();
                if (nextFirstChar == '\r' && nextSecondChar == '\n') {
                    eventBuilder.append(nextFirstChar);
                    eventBuilder.append(nextSecondChar);
                    return eventBuilder.toString();
                }
                this.sseCharStream.addFirst(nextSecondChar);
                this.sseCharStream.addFirst(nextFirstChar);
            }
            prevChar = currentChar;
        }
    }

    public SseEvent getNextSseEvent() throws InterruptedException {
        String nextEvent = this.readNextSseEvent();
        return SseEventParser.parse(nextEvent);
    }
}
