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
    private long value;

    public ByteSize(String raw) {
        if (raw.startsWith("-")) {
            throw new IllegalArgumentException("Negative Size: " + raw);
        }

        this.value = parseBytes(raw);

        if (value <= 0L) {
            throw new IllegalArgumentException("Invalid Size: " + raw);
        }

        this.raw = raw;
    }

    /**
     * Parses the string and returns the value in bytes.
     *
     * @param raw the string to parse
     */
    private long parseBytes(String raw) {
        int index = 0;

        while (index < raw.length() &&
                (Character.isDigit(raw.charAt(index)) || raw.charAt(index) == '.')) {
            index++;
        }

        String unitPart = raw.substring(index).toUpperCase();
        String numberPart = raw.substring(0, index);

        double number = Double.parseDouble(numberPart);

        long value;

        switch (unitPart) {
            case "KB":
                value = (long) (number * 1024);
                break;
            case "MB":
                value = (long) (number * 1024 * 1024);
                break;
            default:
                throw new IllegalArgumentException("Unknown byte size unit: " + unitPart);
        }

        return value;
    }

    /**
     * Returns the value of this {@code ByteSize} object as a long in bytes
     */
    public long getBytes() {
        return value;
    }

    @Override
    public Object value() {
        return raw;
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
        return object;
    }
}
