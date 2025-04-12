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

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.annotations.Categories;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.ArrayList;
import java.util.List;

@Plugin(type = Directive.TYPE)
@Name(AggregateStats.NAME)
@Categories(categories = {"aggregate"})
@Description("Aggregates byte size and time duration columns, returning total or average values.")
public class AggregateStats implements Directive {

    public static final String NAME = "aggregate-stats";
    private String byteSizeColumn;
    private String timeColumn;
    private String totalSizeColumn;
    private String totalTimeColumn;
    private String sizeUnit;
    private String timeUnit;
    private boolean isAverage;

    @Override
    public UsageDefinition define() {
        UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
        builder.define("byteSizeColumn", TokenType.COLUMN_NAME);
        builder.define("timeColumn", TokenType.COLUMN_NAME);
        builder.define("totalSizeColumn", TokenType.COLUMN_NAME);
        builder.define("totalTimeColumn", TokenType.COLUMN_NAME);
        builder.define("sizeUnit", TokenType.TEXT);
        builder.define("timeUnit", TokenType.TEXT);
        builder.define("isAverage", TokenType.BOOLEAN);
        return builder.build();
    }

    @Override
    public void initialize(Arguments args) throws DirectiveParseException {
        this.byteSizeColumn = args.value("byteSizeColumn").toString();
        this.timeColumn = args.value("timeColumn").toString();
        this.totalSizeColumn = args.value("totalSizeColumn").toString();
        this.totalTimeColumn = args.value("totalTimeColumn").toString();
        this.sizeUnit = args.value("sizeUnit").toString();
        this.timeUnit = args.value("timeUnit").toString();
        this.isAverage = Boolean.parseBoolean(args.value("isAverage").toString());
    }

    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
        TransientStore store = context.getTransientStore();

        // For each row, process the byte size and time duration
        for (Row row : rows) {
            String byteSizeStr = row.getValue(byteSizeColumn).toString();
            String timeStr = row.getValue(timeColumn).toString();

            ByteSize byteSize;
            TimeDuration timeDuration;
            try {
                // Parse the byte size and time duration
                byteSize = new ByteSize(byteSizeStr);
                timeDuration = new TimeDuration(timeStr);
            } catch (Exception e) {
                throw new DirectiveExecutionException(e.getMessage());
            }

            // Convert byte size to canonical units (bytes) and time to nanoseconds
            long byteValue = byteSize.getBytes();
            long timeValue = timeDuration.getNanoSeconds();

            // Add the values to the store
            store.increment(TransientVariableScope.LOCAL, totalSizeColumn, byteValue);
            store.increment(TransientVariableScope.LOCAL, totalTimeColumn, timeValue);

            // Optionally track the row count for averaging
            store.increment(TransientVariableScope.LOCAL, "rowCount", 1);
        }

        // Retrieve accumulated totals from the store
        double totalSize = store.get(totalSizeColumn);
        double totalTime = store.get(totalTimeColumn);
        long rowCount = store.get("rowCount");

        // Handle unit conversion if necessary
        if ("MB".equalsIgnoreCase(sizeUnit)) {
            totalSize = totalSize / (1024 * 1024); // Convert bytes to MB
        } else if ("KB".equalsIgnoreCase(sizeUnit)) {
            totalSize = totalSize / 1024; // Convert bytes to KB
        }

        if ("seconds".equalsIgnoreCase(timeUnit)) {
            totalTime = totalTime / 1_000_000_000; // Convert nanoseconds to seconds
        } else if ("ms".equalsIgnoreCase(timeUnit)) {
            totalTime = totalTime / 1_000_000; // Convert nanoseconds to milliseconds
        }

        // If averaging is required, divide by the number of rows
        if (isAverage) {
            totalSize /= rowCount;  // Since totalSize and totalTime are doubles, this division keeps decimals
            totalTime /= rowCount;  // Same for totalTime
        }

        // Add units to the results
        String totalSizeWithUnit = totalSize + " " + sizeUnit;  // Append unit to byte size
        String totalTimeWithUnit = totalTime + " " + timeUnit;  // Append unit to time duration

        // Create a new row for the aggregated results
        Row aggregatedRow = new Row();
        aggregatedRow.add(totalSizeColumn, totalSizeWithUnit);
        aggregatedRow.add(totalTimeColumn, totalTimeWithUnit);

        ArrayList<Row> result = new ArrayList<>();
        result.add(aggregatedRow);

        return result;
    }

    @Override
    public void destroy() {
        // no-op
    }

    public String getName() {
        return NAME;
    }
}
