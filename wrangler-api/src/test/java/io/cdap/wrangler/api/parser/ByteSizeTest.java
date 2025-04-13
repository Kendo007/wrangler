package io.cdap.wrangler.api.parser;

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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ByteSizeTest {

    @Test
    public void testValidSizes() {
        assertEquals(1024, new ByteSize("1KB").getBytes());
        assertEquals(1024 * 1024, new ByteSize("1MB").getBytes());
        assertEquals(1024L * 1024 * 1024, new ByteSize("1GB").getBytes());
        assertEquals(1024L * 1024 * 1024 * 1024, new ByteSize("1TB").getBytes());
        assertEquals(123, new ByteSize("123B").getBytes());
        assertEquals(123, new ByteSize("123").getBytes()); // Default is bytes
    }

    @Test
    public void testDecimalValues() {
        assertEquals(512, new ByteSize("0.5KB").getBytes());
        assertEquals(1536, new ByteSize("1.5KB").getBytes());
        assertEquals(1572864, new ByteSize("1.5MB").getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeValue() {
        new ByteSize("-10MB");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUnit() {
        new ByteSize("10XB");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyString() {
        new ByteSize("");
    }

    @Test
    public void testGetValueInConversions() {
        assertEquals(1.0, ByteSize.getValueIn(1024, "KB"), 0.001);
        assertEquals(1.0, ByteSize.getValueIn(1024 * 1024, "MB"), 0.001);
        assertEquals(1.0, ByteSize.getValueIn(1024 * 1024 * 1024L, "GB"), 0.001);
        assertEquals(1.0, ByteSize.getValueIn(1024L * 1024 * 1024 * 1024, "TB"), 0.001);
        assertEquals(1024.0, ByteSize.getValueIn(1024, "B"), 0.001); // Raw bytes
        assertEquals(1.0, ByteSize.getValueIn(1024, "invalid"), 0.001); // fallback to KB
    }

    @Test
    public void testUtilityConversions() {
        assertEquals(1.0, ByteSize.getKiloBytes(1024), 0.001);
        assertEquals(1.0, ByteSize.getMegaBytes(1024 * 1024), 0.001);
        assertEquals(1.0, ByteSize.getGigaBytes(1024 * 1024 * 1024), 0.001);
        assertEquals(1.0, ByteSize.getTeraBytes(1024L * 1024 * 1024 * 1024), 0.001);
    }

    @Test
    public void testJsonSerialization() {
        ByteSize size = new ByteSize(" 1  MB ");
        JsonElement json = size.toJson();
        assertTrue(json instanceof JsonObject);
        JsonObject obj = json.getAsJsonObject();
        assertEquals("BYTE_SIZE", obj.get("type").getAsString());
        assertEquals(1024 * 1024, obj.get("value").getAsLong());
    }

    @Test
    public void testValueAndTypeMethods() {
        ByteSize size = new ByteSize("2KB");
        assertEquals("2048 B", size.value());
        assertEquals(TokenType.BYTE_SIZE, size.type());
    }
}
