package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

@PublicEvolving
public class ByteSize implements Token {
    /**
     * The {@code ByteSize} object that represents the value held by the token.
     */
    private final String raw;
    private long value = 0L;

    /**
     * Parses the string and returns the value in bytes.
     *
     * @param raw the string to parse
     */
    private long parseBytes(String raw) {
        int index = 0;

        while (index < raw.length() && Character.isDigit(raw.charAt(index)))
            index++;

        String unitPart = raw.substring(index).toUpperCase();
        double number = Double.parseDouble(raw.substring(0, index));   // parse the number part

        switch (unitPart) {
            case "KB":
                value = (long) (number * 1024);
                break;
            case "MB":
                value = (long) (number * 1024 * 1024);
                break;
        };

        return value;
    }

    public ByteSize(String raw) {
        this.value = parseBytes(raw);
        this.raw = raw;
    }

    @Override
    public Object value() {
        return raw;
    }

    /**
     * Returns the value of this {@code ByteSize} object as a long in bytes
     */
    public long getBytes() {
        return value;
    }

    @Override
    public TokenType type() {
        return TokenType.BYTE_SIZE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", TokenType.BYTE_SIZE.name());
        object.addProperty("value", raw);
        return null;
    }
}
