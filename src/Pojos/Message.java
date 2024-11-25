package Pojos;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;  // Add serialVersionUID
    private final MessageType type;
    private final Object content;

    public Message(MessageType type, Object content) {
        this.type = type;
        this.content = content;
    }

    public MessageType getType() {
        return type;
    }

    public Object getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "Message{type=" + type + ", content=" + content + '}';
    }
}