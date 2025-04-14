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
 * The {@code ByteSize} object that represents the value held by the token.
 */
@PublicEvolving
public class ByteSize implements Token {
    private final long value;

    public ByteSize(String raw) {
        raw = raw.trim();

        if (raw.startsWith("-")) {
            throw new IllegalArgumentException("Negative Size: " + raw);
        }

        this.value = parseBytes(raw);

        if (value <= 0L || raw.isEmpty()) {
            throw new IllegalArgumentException("Invalid Size: " + raw);
        }
    }

    /**
     * Parses the string and returns the value in bytes.
     * @param raw the string to parse
     */
    private static long parseBytes(String raw) {
        int index = 0;

        while (index < raw.length() &&
                (Character.isDigit(raw.charAt(index)) || raw.charAt(index) == '.')) {
            index++;
        }

        String unitPart = raw.substring(index).toUpperCase().trim();
        String numberPart = raw.substring(0, index);

        double number = Double.parseDouble(numberPart);
        long value;

        switch (unitPart) {
            case "":
            case "B":
                value = (long) number;
                break;
            case "KB":
                value = (long) (number * 1024);
                break;
            case "MB":
                value = (long) (number * 1024 * 1024);
                break;
            case "GB":
                value = (long) (number * 1024 * 1024 * 1024);
                break;
            case "TB":
                value = (long) (number * 1024 * 1024 * 1024 * 1024);
                break;
            default:
                throw new IllegalArgumentException("Unknown byte size unit: " + unitPart);
        }

        return value;
    }

    /**
      * Converts the byte size value to the specified unit and returns it.
      * @param unit the unit to convert the value to (e.g., "B", "KB", "MB", "GB", "TB")
     */
    public static double getValueIn(long bytes, String unit) {
        switch (unit.trim().toUpperCase()) {
            case "B":
                return (double) bytes;
            case "KB":
                return getKiloBytes(bytes);
            case "MB":
                return getMegaBytes(bytes);
            case "GB":
                return getGigaBytes(bytes);
            case "TB":
                return getTeraBytes(bytes);
            default:
                throw new IllegalArgumentException("Unknown byte size unit: " + unit);
        }
    }

    /**
     * Returns the value of this {@code ByteSize} object as a long in bytes
     */
    public long getBytes() {
        return value;
    }

    /** Return value in KB */
    public static double getKiloBytes(long bytes) {
        return (double) bytes / 1024;
    }

    /** Return value in MB */
    public static double getMegaBytes(long bytes) {
        return (double) bytes / 1024 / 1024;
    }

    /** Return value in GB */
    public static double getGigaBytes(long bytes) {
        return (double) bytes / 1024 / 1024 / 1024;
    }

    /** Return value in TB */
    public static double getTeraBytes(long bytes) {
        return (double) bytes / 1024 / 1024 / 1024 / 1024;
    }

    @Override
    public Object value() {
        return getBytes() + " B";
    }

    @Override
    public TokenType type() {
        return TokenType.BYTE_SIZE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", TokenType.BYTE_SIZE.name());
        object.addProperty("value", value);
        return object;
    }
}
