# JSpec QuickStart

Minimal standalone example demonstrating basic jspec usage.

## Prerequisites

- Java 21
- Maven 3.8+

## Running

```bash
# Compile and run
mvn compile exec:java
```

Or run directly in your IDE by opening `QuickStart.java` and running the main method.

## What it Demonstrates

- Creating criteria with the fluent builder API
- Comparison operators (`$gte`, `$eq`)
- Collection operators (`$in`)
- Regex matching (`$regex`)
- Nested field access (dot notation)
- Composite criteria with AND logic
- Evaluating documents and inspecting results
- Handling missing data with tri-state evaluation

## Expected Output

```
=== JSpec Quick Start Example ===

Document to evaluate:
  {name=Alice, age=28, ...}

Evaluation Results:
-------------------
  age-check       : PASSED
  status-check    : PASSED
  admin-role      : PASSED
  email-format    : PASSED
  uk-resident     : PASSED
  eligibility     : PASSED

Summary:
  Matched:      6
  Not Matched:  0
  Undetermined: 0

All criteria passed: true
...
```

## License

This example is part of the jspec project and is available under the MIT License.
