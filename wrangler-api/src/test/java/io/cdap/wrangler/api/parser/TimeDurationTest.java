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

public class TimeDurationTest {

    @Test
    public void testValidTimeParsing() {
        assertEquals(1_000_000_000L, new TimeDuration("1s").getNanoSeconds());
        assertEquals(1_000_000_000L, new TimeDuration("1sec").getNanoSeconds());
        assertEquals(1_000_000_000L, new TimeDuration("1seconds").getNanoSeconds());
        assertEquals(60_000_000_000L, new TimeDuration("1min").getNanoSeconds());
        assertEquals(1_000_000L, new TimeDuration("1ms").getNanoSeconds());
        assertEquals(1_000_000L, new TimeDuration("1").getNanoSeconds()); // defaults to ms
    }

    @Test
    public void testDecimalTimeParsing() {
        assertEquals(1_500_000_000L, new TimeDuration("1.5s").getNanoSeconds());
        assertEquals(90_000_000_000L, new TimeDuration("1.5min").getNanoSeconds());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeDuration() {
        new TimeDuration("-5s");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUnit() {
        new TimeDuration("10years");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyString() {
        new TimeDuration("");
    }

    @Test
    public void testUnitConversionStaticMethods() {
        long oneSecondInNs = 1_000_000_000L;
        long oneMinuteInNs = 60_000_000_000L;
        long oneMsInNs = 1_000_000L;

        assertEquals(1.0, TimeDuration.getSeconds(oneSecondInNs), 0.001);
        assertEquals(1.0, TimeDuration.getMinutes(oneMinuteInNs), 0.001);
        assertEquals(1.0, TimeDuration.getMilliSeconds(oneMsInNs), 0.001);
    }

    @Test
    public void testGetValueInDispatcher() {
        assertEquals(1.0, TimeDuration.getValueIn(60_000_000_000L, "min"), 0.001);
        assertEquals(1.0, TimeDuration.getValueIn(1_000_000_000L, "sec"), 0.001);
        assertEquals(1.0, TimeDuration.getValueIn(1_000_000L, "ms"), 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetValueInFail() {
        assertEquals(1.0, TimeDuration.getValueIn(1_000_000L, "invalid"), 0.001);
    }

    @Test
    public void testValueAndType() {
        TimeDuration duration = new TimeDuration("2s");
        assertEquals("2000000000 ns", duration.value());
        assertEquals(TokenType.TIME_DURATION, duration.type());
    }

    @Test
    public void testJsonSerialization() {
        TimeDuration duration = new TimeDuration("2s");
        JsonElement json = duration.toJson();
        assertTrue(json instanceof JsonObject);
        JsonObject obj = json.getAsJsonObject();
        assertEquals("TIME_DURATION", obj.get("type").getAsString());
        assertEquals(2_000_000_000L, obj.get("value").getAsLong());
    }
}
