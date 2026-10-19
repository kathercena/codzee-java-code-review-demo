# Codzee assessment plan

This document defines a sequence of independent changes to review with Codzee after the initial baseline has been inspected. No Codzee review has been run or is claimed here.

## Baseline

The default branch contains the task CRUD API, DTO mapping, validation, centralized error responses, bounded pagination, H2 configuration, and unit/integration coverage. Establish and review this baseline before starting feature branches. Keep the default branch free of the three proposed enhancements until it is approved.

## Proposed pull requests

### PR 1 — Business rules and validation

Branch: `feature/task-business-rules`

Add explicit domain rules around task creation and updates, such as allowed status transitions and any agreed title/description normalization rules. Cover boundaries and invalid transitions with focused service tests, including tests proving rejected operations do not persist changes. Keep validation behavior deterministic and avoid embedding policy in controllers.

### PR 2 — REST API and exception handling

Branch: `feature/api-error-contract`

Refine and document the public error contract, including field-level validation details, malformed JSON and enum values, missing resources, and invalid query/path parameters. Add MockMvc tests for response status, headers where relevant, and JSON shape. Verify no internal exception messages or stack traces leak through responses.

### PR 3 — Authorization and data integrity

Branch: `feature/task-ownership`

Introduce task ownership using a small, isolated authenticated-user abstraction. For a standalone demo without an identity provider, use a clearly labeled test/development principal adapter behind a replaceable interface; do not treat a caller-controlled or hard-coded identity as production authentication. Add tests for unauthenticated access, cross-user read/update/delete isolation, and owner-scoped listing. Document that production use requires a real authentication provider and trusted principal.

## Test strategy

- PR 1: service unit tests for business rules, boundaries, and repository interactions; run the full Maven test suite.
- PR 2: MockMvc integration tests for valid and invalid HTTP requests and the stable error JSON contract; run `mvn clean verify`.
- PR 3: service/controller tests with distinct principals and persisted owners to prove isolation; run `mvn clean verify` and inspect schema behavior.

Each pull request should change one concern, include tests that fail for the relevant regression, and preserve all prior tests. Do not add intentional defects to production code.

## Evaluating Codzee reviews

For each pull request, record the review output and assess whether findings are valid, actionable, correctly prioritized, and tied to changed lines. Check whether Codzee catches correctness, security, data-integrity, and test gaps without reporting speculative or irrelevant issues. Compare findings with an independent human review, note missed and false-positive findings, and do not infer review quality from whether the tool ran successfully. No review result should be attributed to Codzee until the tool is installed and an actual review has been observed.
