package com.example.jspecdemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import uk.codery.jspec.formatter.JsonResultFormatter;
import uk.codery.jspec.formatter.ResultFormatter;
import uk.codery.jspec.model.CompositeCriterion;
import uk.codery.jspec.model.QueryCriterion;
import uk.codery.jspec.model.Specification;

import java.io.IOException;

/**
 * Spring configuration for jspec components.
 *
 * <p>Configures:
 * <ul>
 *   <li>YAML ObjectMapper for parsing specifications</li>
 *   <li>Pre-built specifications as beans</li>
 *   <li>Result formatters</li>
 * </ul>
 */
@Configuration
public class JSpecConfig {

    /**
     * ObjectMapper configured for YAML parsing.
     * Use this to load specifications from YAML files.
     */
    @Bean
    public ObjectMapper yamlMapper() {
        return new ObjectMapper(new YAMLFactory());
    }

    /**
     * JSON result formatter for API responses.
     */
    @Bean
    public ResultFormatter jsonFormatter() {
        return new JsonResultFormatter(true); // pretty-printed
    }

    /**
     * Load specification from classpath resource.
     *
     * <p>The specification file is at: src/main/resources/specifications/user-eligibility.yaml
     */
    @Bean
    public Specification eligibilitySpec(
            ObjectMapper yamlMapper,
            @Value("classpath:specifications/user-eligibility.yaml") Resource specResource
    ) throws IOException {
        return yamlMapper.readValue(specResource.getInputStream(), Specification.class);
    }

    /**
     * Build loan approval specification programmatically.
     *
     * <p>This approach is useful when criteria are defined in code.
     */
    @Bean
    public Specification loanApprovalSpec() {
        // Define individual criteria
        QueryCriterion ageCheck = QueryCriterion.builder()
                .id("age-check")
                .field("age").gte(18).and().lte(65)
                .build();

        QueryCriterion incomeCheck = QueryCriterion.builder()
                .id("income-check")
                .field("annualIncome").gte(30000)
                .build();

        QueryCriterion employmentCheck = QueryCriterion.builder()
                .id("employment-check")
                .field("employmentStatus").in("employed", "self-employed")
                .build();

        QueryCriterion creditScore = QueryCriterion.builder()
                .id("credit-score")
                .field("creditScore").gte(650)
                .build();

        QueryCriterion noDefaultHistory = QueryCriterion.builder()
                .id("no-defaults")
                .field("hasDefaultHistory").eq(false)
                .build();

        // Composite: All criteria must match
        CompositeCriterion loanEligibility = CompositeCriterion.builder()
                .id("loan-eligibility")
                .and()
                .criteria(ageCheck, incomeCheck, employmentCheck, creditScore, noDefaultHistory)
                .build();

        // Build the complete specification
        return Specification.builder()
                .id("loan-approval")
                .addCriterion(ageCheck)
                .addCriterion(incomeCheck)
                .addCriterion(employmentCheck)
                .addCriterion(creditScore)
                .addCriterion(noDefaultHistory)
                .addCriterion(loanEligibility)
                .build();
    }
}
