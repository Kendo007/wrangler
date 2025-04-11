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
    private double value;

    public TimeDuration(String raw) {
        if (raw.startsWith("-")) {
            throw new IllegalArgumentException("Negative time duration: " + raw);
        }

        this.value = parseTime(raw);

        if (value <= 0) {
            throw new IllegalArgumentException("Invalid time: " + raw);
        }

        this.raw = raw;
    }

    /**
     * Parses the string and returns the value in milliseconds.
     *
     * @param raw the string to parse
     */
    private double parseTime(String raw) {
        int index = 0;
        while (index < raw.length() && (Character.isDigit(raw.charAt(index)) || raw.charAt(index) == '.')) {
            index++;
        }

        String unitPart = raw.substring(index).toLowerCase();
        double number = Double.parseDouble(raw.substring(0, index));

        switch (unitPart) {
            case "s":
            case "sec":
            case "seconds":
                return number * 1000;
            case "ms":
                return number;
            default:
                throw new IllegalArgumentException("Unknown time unit: " + unitPart);
        }
    }

    /**
     * Returns the value of this {@code TimeDuration} object as a double in milliseconds.
     */
    public double getMilliSeconds() {
        return value;
    }

    @Override
    public Object value() {
        return raw;
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
        return object;
    }
}
