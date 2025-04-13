/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package io.cdap.directives.aggregates;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.RecipeException;
import io.cdap.wrangler.api.Row;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Tests {@link AggregateStats}
 */
public class AggregateStatsTest {

    @Test
    public void testBasicAggregation() throws Exception {
        String[] recipe = new String[] {
                "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec;"
        };

        // Sample rows
        List<Row> rows = Arrays.asList(
                new Row("data_transfer_size", "10485760KB").add("response_time", "5000000ms"),
                new Row("data_transfer_size", "20971520KB").add("response_time", "6000000ms"),
                new Row("data_transfer_size", "5242880KB").add("response_time", "4000000ms")
        );

        rows = TestingRig.execute(recipe, rows);
        Assert.assertEquals(1, rows.size());

        // Check the output values
        Assert.assertEquals("35840.000 MB", rows.get(0).getValue("total_size_mb"));
        Assert.assertEquals("15000.000 s", rows.get(0).getValue("total_time_sec"));
    }

    @Test
    public void testDiffColumnNames() throws Exception {
        String[] recipe1 = new String[] {
                "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec;"
        };

        String[] recipe2 = new String[] {
                "aggregate-stats :data_transfer_size :response_time my_size my_time;"
        };

        // Sample rows
        List<Row> rows = Arrays.asList(
                new Row("data_transfer_size", "10485760KB").add("response_time", "5000000ms"),
                new Row("data_transfer_size", "20971520KB").add("response_time", "6000000ms"),
                new Row("data_transfer_size", "5242880KB").add("response_time", "4000000ms")
        );

        List<Row> rows1 = TestingRig.execute(recipe1, rows);
        List<Row> rows2 = TestingRig.execute(recipe2, rows);

        Assert.assertEquals(1, rows1.size());
        Assert.assertEquals(1, rows2.size());

        // Check the output values
        Assert.assertEquals(rows2.get(0).getValue("my_size"), rows1.get(0).getValue("total_size_mb"));
        Assert.assertEquals(rows2.get(0).getValue("my_time"), rows1.get(0).getValue("total_time_sec"));
    }


    @Test
    public void testAverageCalculation() throws Exception {
        String[] recipe = new String[] {
                "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec 'Gb' 'min' true"
        };

        // Sample rows
        List<Row> rows = Arrays.asList(
                new Row("data_transfer_size", "10485760KB").add("response_time", "5000000ms"),
                new Row("data_transfer_size", "20971520KB").add("response_time", "6000000ms"),
                new Row("data_transfer_size", "5242880KB").add("response_time", "4000000ms")
        );

        rows = TestingRig.execute(recipe, rows);
        Assert.assertEquals(1, rows.size());

        // Check the average values
        Assert.assertEquals("11.667 GB", rows.get(0).getValue("total_size_mb"));
        Assert.assertEquals("83.333 min", rows.get(0).getValue("total_time_sec"));
    }

    @Test
    public void testUnitConversion() throws Exception {
        String[] recipe = new String[] {
                "aggregate-stats :data_transfer_size :response_time total_size_kb total_time_ms 'KB' 'ms' false"
        };

        // Sample rows
        List<Row> rows = Arrays.asList(
                new Row("data_transfer_size", "10.48mb").add("response_time", "50.9seconds"),
                new Row("data_transfer_size", "20.97mB").add("response_time", "60sec")
        );

        rows = TestingRig.execute(recipe, rows);
        Assert.assertEquals(1, rows.size());

        // Check the unit conversion and totals
        Assert.assertEquals("32204.799 KB", rows.get(0).getValue("total_size_kb"));
        Assert.assertEquals("110900.000 ms", rows.get(0).getValue("total_time_ms"));
    }

    @Test(expected = DirectiveExecutionException.class)
    public void testZeroValuesWithException() throws Exception {
        String[] recipe = new String[] {
                "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec 'MB' 'seconds' false"
        };

        // Sample rows with zero values that should cause an exception
        List<Row> rows = Arrays.asList(
                new Row("data_transfer_size", "0KB").add("response_time", "0ms")
        );

        try {
            TestingRig.execute(recipe, rows); // Should throw DirectiveExecutionException
        } catch (RecipeException e) {
            System.out.println(e.getMessage());
            throw new DirectiveExecutionException(e.getMessage(), e);
        }
    }

    @Test(expected = RecipeException.class)
    public void testNegativeValues() throws Exception {
        String[] recipe = new String[] {
                "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec 'MB' 'seconds' false"
        };

        // Sample rows with negative values
        List<Row> rows = Arrays.asList(
                new Row("data_transfer_size", "-10485760KB").add("response_time", "5000000ms"),
                new Row("data_transfer_size", "20971520KB").add("response_time", "-6000000ms")
        );

        TestingRig.execute(recipe, rows); // Should throw DirectiveExecutionException
    }
}
