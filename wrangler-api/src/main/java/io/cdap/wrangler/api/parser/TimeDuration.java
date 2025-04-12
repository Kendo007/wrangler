package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * The {@code TimeDuration} object that represents the value held by the token.
*/
@PublicEvolving
public class TimeDuration implements Token {
    private final String raw;
    private final long value;

    public TimeDuration(String raw) {
        if (raw.startsWith("-")) {
            throw new IllegalArgumentException("Negative time duration: " + raw);
        }

        this.value = parseTime(raw);

        if (value <= 0 || raw.isEmpty()) {
            throw new IllegalArgumentException("Invalid time: " + raw);
        }

        this.raw = raw;
    }

    /**
     * Parses the string and returns the value in milliseconds.
     *
     * @param raw the string to parse
     */
    private long parseTime(String raw) {
        int index = 0;
        while (index < raw.length() && (Character.isDigit(raw.charAt(index)) || raw.charAt(index) == '.')) {
            index++;
        }

        raw = raw.trim();
        String unitPart = raw.substring(index).toLowerCase();
        double number = Double.parseDouble(raw.substring(0, index));

        switch (unitPart) {
            case "s":
            case "sec":
            case "seconds":
                return (long) (number * 1000 * 1000);
            case "ms":
                return (long) (number * 1000);
            default:
                throw new IllegalArgumentException("Unknown time unit: " + unitPart);
        }
    }

    /**
     * Returns the value of this {@code TimeDuration} object as a double in milliseconds.
     */
    public long getNanoSeconds() {
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
