package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

@PublicEvolving
public class TimeDuration implements Token {
    /**
     * The {@code TimeDuration} object that represents the value held by the token.
     */
    private final String raw;
    private double value = 0L;

    /**
     * Parses the string and returns the value in milliseconds.
     *
     * @param raw the string to parse
     */
    private double parseBytes(String raw) {
        int index = 0;

        while (index < raw.length() && Character.isDigit(raw.charAt(index)))
            index++;

        String unitPart = raw.substring(index).toLowerCase();
        double number = Double.parseDouble(raw.substring(0, index));   // parse the number part

        switch (unitPart) {
            case "s":
            case "sec":
            case "seconds":
                value = (number * 1000);
                break;
            case "ms":
                value = number;
                break;
        };

        return value;
    }

    public TimeDuration(String raw) {
        this.value = parseBytes(raw);
        this.raw = raw;
    }

    @Override
    public Object value() {
        return raw;
    }

    /**
     * Returns the value of this {@code TimeDuration} object as a double in milliseconds.
     */
    public double getMilliSeconds() {
        return value;
    }

    @Override
    public TokenType type() {
        return TokenType.TIME_DURATION;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", TokenType.TIME_DURATION.name());
        object.addProperty("value", raw);
        return null;
    }
}
