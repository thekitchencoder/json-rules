# JSON Specification Evaluator

[![Maven Central](https://img.shields.io/maven-central/v/uk.codery/jspec.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22uk.codery%22%20AND%20a:%22jspec%22)
[![CI](https://github.com/thekitchencoder/jspec/workflows/CI/badge.svg)](https://github.com/thekitchencoder/jspec/actions)
[![codecov](https://codecov.io/gh/thekitchencoder/jspec/branch/main/graph/badge.svg)](https://codecov.io/gh/thekitchencoder/jspec)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fthekitchencoder%2Fjspec.svg?type=shield&issueType=license)](https://app.fossa.com/projects/git%2Bgithub.com%2Fthekitchencoder%2Fjspec?ref=badge_shield&issueType=license)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fthekitchencoder%2Fjspec.svg?type=shield&issueType=security)](https://app.fossa.com/projects/git%2Bgithub.com%2Fthekitchencoder%2Fjspec?ref=badge_shield&issueType=security)
[![Java](https://img.shields.io/badge/Java-21+-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Javadoc](https://javadoc.io/badge2/uk.codery/jspec/javadoc.svg)](https://javadoc.io/doc/uk.codery/jspec)

A lightweight, Spring-independent Java library for evaluating business criteria against JSON/YAML documents using MongoDB-style query operators.

## Features

- **13 MongoDB-style operators** - Familiar query syntax for developers.
- **Tri-state evaluation model** - Distinguishes between `MATCHED`, `NOT_MATCHED`, and `UNDETERMINED` states.
- **Graceful error handling** - One failed criterion never stops the evaluation of others.
- **Zero framework dependencies** - Works with or without Spring.
- **Thread-safe parallel evaluation** - Efficiently process multiple criteria.
- **Deep document navigation** - Query nested structures with dot notation.
- **Java 21** - Built with modern language features and an immutable, record-based design.

## Quick Start

### Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>uk.codery</groupId>
    <artifactId>jspec</artifactId>
    <version>0.4.1</version>
</dependency>
```

### Basic Usage

1.  **Define a `Specification`** containing your criteria. This can be done in YAML or programmatically.

    ```yaml
    # specification.yaml
    id: order-validation
    criteria:
      - id: minimum-order
        query:
          order.total:
            $gte: 25.00
      - id: customer-verified
        query:
          customer.verified: true
    criteriaGroups:
      - id: express-shipping-eligible
        junction: AND
        criteria: [minimum-order, customer-verified]
    ```

2.  **Create an evaluator** and evaluate a document against the specification.

    ```java
    import uk.codery.jspec.evaluator.SpecificationEvaluator;
    import uk.codery.jspec.model.Specification;
    import uk.codery.jspec.result.EvaluationOutcome;
    import com.fasterxml.jackson.databind.ObjectMapper;
    import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

    // Load specification from YAML
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Specification spec = mapper.readValue(new File("specification.yaml"), Specification.class);

    // Create an evaluator bound to the specification
    SpecificationEvaluator evaluator = new SpecificationEvaluator(spec);

    // Create a sample document to evaluate
    Map<String, Object> document = Map.of(
        "order", Map.of("total", 50.00),
        "customer", Map.of("verified", true)
    );

    // Evaluate the document
    EvaluationOutcome outcome = evaluator.evaluate(document);

    // Check the results
    System.out.println("Evaluation Summary: " + outcome.summary());
    outcome.criterionResults().forEach(result ->
        System.out.println(" - " + result.criterion().id() + ": " + result.state())
    );
    ```

## Documentation

For more detailed information, please refer to the documentation in the `docs/` folder:

-   **[Supported Operators](docs/OPERATORS.md)**: A full list of the 13 supported MongoDB-style operators.
-   **[Evaluation Model](docs/EVALUATION_MODEL.md)**: An in-depth explanation of the tri-state evaluation model and the graceful error handling philosophy.
-   **[Architecture](docs/ARCHITECTURE.md)**: A guide to the core design principles, layers, and thread-safety model.
-   **[Use Cases](docs/usecases.md)**: Examples of how JSPEC can be used in different scenarios.
-   **[JavaDoc](https://javadoc.io/doc/uk.codery/jspec)**: Full API documentation.

## Building from Source

To build the project locally:

```bash
# Clone the repository
git clone https://github.com/thekitchencoder/jspec.git
cd jspec

# Build with Maven
mvn clean install

# Run tests
mvn test
```

### Requirements
- Java 21 or higher
- Maven 3.6+

### Dependencies
- **Jackson DataFormat YAML**: For JSON/YAML parsing.
- **Lombok**: To reduce boilerplate code.
- **SLF4J API**: For logging.

## Demo

To run the demo application and see the engine in action:

```bash
mvn test-compile exec:java -Dexec.mainClass="uk.codery.jspec.demo.Main"
```

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

