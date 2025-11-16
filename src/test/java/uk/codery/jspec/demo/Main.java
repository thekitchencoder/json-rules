package uk.codery.jspec.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import uk.codery.jspec.evaluator.SpecificationEvaluator;
import uk.codery.jspec.formatter.JsonResultFormatter;
import uk.codery.jspec.formatter.TextResultFormatter;
import uk.codery.jspec.formatter.YamlResultFormatter;
import uk.codery.jspec.model.Specification;
import uk.codery.jspec.result.CompositeResult;
import uk.codery.jspec.result.EvaluationOutcome;
import uk.codery.jspec.result.EvaluationResult;

import java.io.File;
import java.util.Arrays;

import static java.util.function.Predicate.not;

/**
 * Demo CLI application for JSON Specification Evaluator.
 *
 * <p>Demonstrates the use of formatters to output evaluation results in different formats:
 * <ul>
 *   <li>JSON - Structured JSON output</li>
 *   <li>YAML - Human-readable YAML output</li>
 *   <li>Text - Formatted text with summary and detailed results</li>
 * </ul>
 *
 * <p>Usage: java Main &lt;criteria.yaml&gt; &lt;document.yaml&gt; [options]
 *
 * <h2>Options</h2>
 * <ul>
 *   <li>--json     Output results in JSON format (pretty-printed)</li>
 *   <li>--yaml     Output results in YAML format</li>
 *   <li>--text     Output results in formatted text (verbose mode)</li>
 *   <li>--summary  Only show summary of results (legacy format)</li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <pre>{@code
 * # JSON output
 * java Main specification.yaml document.yaml --json
 *
 * # YAML output
 * java Main specification.yaml document.yaml --yaml
 *
 * # Text output (verbose)
 * java Main specification.yaml document.yaml --text
 *
 * # Summary only
 * java Main specification.yaml document.yaml --summary
 * }</pre>
 */
public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java Main <criteria.yaml> <document.yaml> [options]");
            System.err.println("\nOptions:");
            System.err.println("  --json     Output results in JSON format (pretty-printed)");
            System.err.println("  --yaml     Output results in YAML format");
            System.err.println("  --text     Output results in formatted text (verbose mode)");
            System.err.println("  --summary  Only show summary of results (legacy format)");
            System.err.println("\nDefault: Formatted text output (non-verbose)");
            System.exit(1);
        }

        String specFile = args[0];
        String docFile = args[1];
        boolean jsonOutput = Arrays.asList(args).contains("--json");
        boolean yamlOutput = Arrays.asList(args).contains("--yaml");
        boolean textOutput = Arrays.asList(args).contains("--text");
        boolean summaryOnly = Arrays.asList(args).contains("--summary");

        try {
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

            Object doc = yamlMapper.readValue(new File(docFile), Object.class);
            Specification specification = yamlMapper.readValue(new File(specFile), Specification.class);

            SpecificationEvaluator evaluator = new SpecificationEvaluator();
            EvaluationOutcome outcome = evaluator.evaluate(doc, specification);

            if (jsonOutput) {
                outputJson(outcome);
            } else if (yamlOutput) {
                outputYaml(outcome);
            } else if (textOutput) {
                outputText(outcome, true);  // Verbose mode
            } else if (summaryOnly) {
                outputSummary(outcome);
            } else {
                // Default: formatted text (non-verbose)
                outputText(outcome, false);
            }

            boolean allMatched = outcome.results().stream()
                    .allMatch(EvaluationResult::matched);
            System.exit(allMatched ? 0 : 1);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Outputs results in JSON format using JsonResultFormatter.
     *
     * @param outcome the evaluation outcome to format
     */
    private static void outputJson(EvaluationOutcome outcome) {
        JsonResultFormatter formatter = new JsonResultFormatter(true);  // Pretty-print
        System.out.println(formatter.format(outcome));
    }

    /**
     * Outputs results in YAML format using YamlResultFormatter.
     *
     * @param outcome the evaluation outcome to format
     */
    private static void outputYaml(EvaluationOutcome outcome) {
        YamlResultFormatter formatter = new YamlResultFormatter();
        System.out.println(formatter.format(outcome));
    }

    /**
     * Outputs results in formatted text using TextResultFormatter.
     *
     * @param outcome the evaluation outcome to format
     * @param verbose true to include verbose details (child results, missing paths, etc.)
     */
    private static void outputText(EvaluationOutcome outcome, boolean verbose) {
        TextResultFormatter formatter = new TextResultFormatter(verbose);
        System.out.print(formatter.format(outcome));
    }

    /**
     * Outputs a legacy summary format.
     *
     * @param outcome the evaluation outcome to summarize
     * @deprecated Use {@link #outputText(EvaluationOutcome, boolean)} instead
     */
    @Deprecated
    private static void outputSummary(EvaluationOutcome outcome) {
        long passedQueries = outcome.queryResults().stream()
                .filter(EvaluationResult::matched)
                .count();
        long failedQueries = outcome.queryResults().stream()
                .filter(not(EvaluationResult::matched))
                .count();
        long passedComposites = outcome.compositeResults().stream()
                .filter(EvaluationResult::matched)
                .count();
        long failedComposites = outcome.compositeResults().stream()
                .filter(not(EvaluationResult::matched))
                .count();

        System.out.println(outcome.specificationId() + " evaluation summary.");
        System.out.println("Queries: " + (passedQueries + failedQueries) + " total, " + passedQueries + " passed, " + failedQueries + " failed");
        System.out.println("Composites: " + (passedComposites + failedComposites) + " total, " + passedComposites + " passed, " + failedComposites + " failed");

        if (failedQueries > 0) {
            System.out.println("\nFailed Queries:");
            outcome.queryResults().stream()
                    .filter(not(EvaluationResult::matched))
                    .forEach(r -> System.out.println("  " + r.id() + ": " + r.reason()));
        }

        if (failedComposites > 0) {
            System.out.println("\nFailed Composites:");
            outcome.compositeResults().stream()
                    .filter(not(EvaluationResult::matched))
                    .forEach(composite -> {
                        System.out.println("  " + composite.id() + " (" + composite.junction() + ")");
                        composite.childResults().stream()
                                .filter(not(EvaluationResult::matched))
                                .forEach(child -> System.out.println("    - " + child.id() + ": " + child.reason()));
                    });
        }
    }
}
