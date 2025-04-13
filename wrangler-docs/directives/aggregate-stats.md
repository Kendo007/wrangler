## Aggregate Stats

The `aggregate-stats` directive aggregates byte size and time duration columns, returning total or average values with optional unit conversion.

---

### Syntax

```plaintext
aggregate-stats <size-column> <time-column> <total-size-col> <total-time-col> [<size-unit>] [<time-unit>] [<is-average>]
```

---

### Parameters

| Name              | Type       | Required | Description                                                                 |
|-------------------|------------|----------|-----------------------------------------------------------------------------|
| `size-column`     | Column     | Yes      | Column name that contains byte size values (e.g., "10KB", "1.5MB").        |
| `time-column`     | Column     | Yes      | Column name that contains time duration values (e.g., "2s", "100ms").      |
| `total-size-col`  | Identifier | Yes      | Name of the output column to store total or average size (e.g., `total_mb`). |
| `total-time-col`  | Identifier | Yes      | Name of the output column to store total or average time.                  |
| `size-unit`       | Text       | No       | Desired output unit for size (`"MB"`, `"KB"`). Defaults to `"MB"`.         |
| `time-unit`       | Text       | No       | Desired output unit for time (`"seconds"`, `"ms"`). Defaults to `"s"`.     |
| `is-average`      | Boolean    | No       | Whether to output average instead of total. Defaults to `false`.           |

---

### Behavior

- Each input row's byte and time values are converted to canonical units (`bytes`, `nanoseconds`) and summed.
- If `is-average` is true, results are averaged over row count.
- The result is a single output row with converted, rounded results in the specified units.

---

### Example

Given input rows:

```json
[
  { "data_transfer_size": "10MB", "response_time": "1s" },
  { "data_transfer_size": "5MB", "response_time": "2s" }
]
```

And the directive:

```plaintext
aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec "MB" "seconds" false
```

The output would be:

```json
[
  {
    "total_size_mb": "15.000 MB",
    "total_time_sec": "3.000 seconds"
  }
]
```