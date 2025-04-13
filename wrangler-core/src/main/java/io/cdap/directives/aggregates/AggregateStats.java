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
import io.cdap.wrangler.api.Optional;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.annotations.Categories;
import io.cdap.wrangler.api.parser.Bool;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Identifier;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Directive to aggregate byte size and time duration columns, returning total or average values.
 */
@Plugin(type = Directive.TYPE)
@Name(AggregateStats.NAME)
@Categories(categories = {"aggregate", "byte", "time"})
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
        builder.define("totalSizeColumn", TokenType.IDENTIFIER);
        builder.define("totalTimeColumn", TokenType.IDENTIFIER);
        builder.define("sizeUnit", TokenType.TEXT, Optional.TRUE);
        builder.define("timeUnit", TokenType.TEXT, Optional.TRUE);
        builder.define("isAverage", TokenType.BOOLEAN, Optional.TRUE);
        return builder.build();
    }

    @Override
    public void initialize(Arguments args) throws DirectiveParseException {
        this.byteSizeColumn = ((ColumnName) args.value("byteSizeColumn")).value();
        this.timeColumn = ((ColumnName) args.value("timeColumn")).value();
        this.totalSizeColumn = ((Identifier) args.value("totalSizeColumn")).value();
        this.totalTimeColumn = ((Identifier) args.value("totalTimeColumn")).value();

        this.sizeUnit = args.contains("sizeUnit") ? ((Text) args.value("sizeUnit")).value() : "MB";
        this.timeUnit = args.contains("timeUnit") ? ((Text) args.value("timeUnit")).value() : "s";
        this.isAverage = args.contains("isAverage") ? ((Bool) args.value("isAverage")).value() : false;
    }

    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
        TransientStore store = context.getTransientStore();

        // For each row, process the byte size and time duration
        for (Row row : rows) {
            try {
                String byteSizeStr = row.getValue(row.find(byteSizeColumn)).toString();
                String timeStr = row.getValue(row.find(timeColumn)).toString();

                // Parse the byte size and time duration
                ByteSize byteSize = new ByteSize(byteSizeStr);
                TimeDuration timeDuration = new TimeDuration(timeStr);
                // Convert byte size to canonical units (bytes) and time to nanoseconds
                long byteValue = byteSize.getBytes();
                long timeValue = timeDuration.getNanoSeconds();

                // Add the values to the store
                store.increment(TransientVariableScope.GLOBAL, totalSizeColumn, byteValue);
                store.increment(TransientVariableScope.GLOBAL, totalTimeColumn, timeValue);

                // Optionally track the row count for averaging
                store.increment(TransientVariableScope.GLOBAL, "rowCount", 1);
            } catch (Exception e) {
                throw new DirectiveExecutionException(e.getMessage());
            }
        }

        Object currRowCountObject = store.get("rowCount");
        long rowCount = (currRowCountObject instanceof Number) ? ((Number) currRowCountObject).longValue()
                : Long.parseLong(currRowCountObject.toString());

        Object totalRowCountObject = store.get("_totalRowCount_");
        long total = (totalRowCountObject instanceof Number) ? ((Number) totalRowCountObject).longValue()
                : Long.parseLong(totalRowCountObject.toString());

        if (rowCount == total) {
            return finalize(context);
        }

        return new ArrayList<>();
    }

    /**
     * This is the method used to return the final result with the help of the context
     * @param context the context of the directive
     * @return the final row containing the result
     */
    private List<Row> finalize(ExecutorContext context) {
        TransientStore store = context.getTransientStore();

        // Retrieve accumulated totals from the store
        Object totalSizeObject = store.get(totalSizeColumn);
        long totalSizeBytes = (totalSizeObject instanceof Number) ? ((Number) totalSizeObject).longValue()
                : Long.parseLong(totalSizeObject.toString());

        Object totalTimeObject = store.get(totalTimeColumn);
        long totalTimeNano = (totalTimeObject instanceof Number) ? ((Number) totalTimeObject).longValue()
                : Long.parseLong(totalTimeObject.toString());

        Object rowCountObject = store.get("rowCount");
        long rowCount = (rowCountObject instanceof Number) ? ((Number) rowCountObject).longValue()
                : Long.parseLong(rowCountObject.toString());

        // If averaging is required, divide by the number of rows
        if (isAverage) {
            totalSizeBytes /= rowCount;
            totalTimeNano /= rowCount;  // Same for totalTime
        }

        // Handle unit conversion if necessary
        double totalSize = ByteSize.getValueIn(totalSizeBytes, sizeUnit);
        double totalTime = TimeDuration.getValueIn(totalTimeNano, timeUnit);

        // Add units to the results
        String totalSizeWithUnit = String.format("%.3f", totalSize) + " " + sizeUnit.toUpperCase();
        String totalTimeWithUnit = String.format("%.3f", totalTime) + " " + timeUnit.toLowerCase();

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
