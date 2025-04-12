package io.cdap.directives.aggregates;

import io.cdap.wrangler.TestingRig;
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
                "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec 'MB' 'seconds' false"
        };

        // Sample rows
        List<Row> rows = Arrays.asList(
                new Row("data_transfer_size", 10485760).add("response_time", 5000000000L),
                new Row("data_transfer_size", 20971520).add("response_time", 6000000000L),
                new Row("data_transfer_size", 5242880).add("response_time", 4000000000L)
        );

        rows = TestingRig.execute(recipe, rows);
        Assert.assertEquals(1, rows.size());

        // Check the output values
        Assert.assertEquals("35.0 MB", rows.get(0).getValue("total_size_mb"));
        Assert.assertEquals("15.0 seconds", rows.get(0).getValue("total_time_sec"));
    }

    @Test
    public void testAverageCalculation() throws Exception {
        String[] recipe = new String[] {
                "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec 'MB' 'seconds' true"
        };

        // Sample rows
        List<Row> rows = Arrays.asList(
                new Row("data_transfer_size", 10485760).add("response_time", 5000000000L),
                new Row("data_transfer_size", 20971520).add("response_time", 6000000000L),
                new Row("data_transfer_size", 5242880).add("response_time", 4000000000L)
        );

        rows = TestingRig.execute(recipe, rows);
        Assert.assertEquals(1, rows.size());

        // Check the average values
        Assert.assertEquals("11.67 MB", rows.get(0).getValue("total_size_mb"));
        Assert.assertEquals("5.0 seconds", rows.get(0).getValue("total_time_sec"));
    }

    @Test
    public void testUnitConversion() throws Exception {
        String[] recipe = new String[] {
                "aggregate-stats :data_transfer_size :response_time total_size_kb total_time_ms 'KB' 'ms' false"
        };

        // Sample rows
        List<Row> rows = Arrays.asList(
                new Row("data_transfer_size", 10485760).add("response_time", 5000000000L),
                new Row("data_transfer_size", 20971520).add("response_time", 6000000000L)
        );

        rows = TestingRig.execute(recipe, rows);
        Assert.assertEquals(1, rows.size());

        // Check the unit conversion and totals
        Assert.assertEquals("10240.0 KB", rows.get(0).getValue("total_size_kb"));
        Assert.assertEquals("11.0 ms", rows.get(0).getValue("total_time_ms"));
    }

    @Test
    public void testEmptyRows() throws Exception {
        String[] recipe = new String[] {
                "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec 'MB' 'seconds' false"
        };

        // Sample empty rows
        List<Row> rows = Arrays.asList(
                new Row("data_transfer_size", 0).add("response_time", 0),
                new Row("data_transfer_size", 0).add("response_time", 0)
        );

        rows = TestingRig.execute(recipe, rows);
        Assert.assertEquals(1, rows.size());

        // Check if the result for empty values is correctly zero
        Assert.assertEquals("0.0 MB", rows.get(0).getValue("total_size_mb"));
        Assert.assertEquals("0.0 seconds", rows.get(0).getValue("total_time_sec"));
    }

    @Test
    public void testNoRows() throws Exception {
        String[] recipe = new String[] {
                "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec 'MB' 'seconds' false"
        };

        // Sample no rows
        List<Row> rows = Arrays.asList();

        rows = TestingRig.execute(recipe, rows);
        Assert.assertEquals(0, rows.size());  // No rows should be processed
    }
}
