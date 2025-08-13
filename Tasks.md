UNIT TESTS
- [ ] Write unit tests for all services and components.
- [ ] Ensure high test coverage (aim for 80%+).
- [ ] Use JUnit 5 and Mockito for testing.(should be in the depenedency already)
- [ ] Test all edge cases, especially for ride assignment and optimization logic.

##### Observability
Structured JSON logs (Logback JSON), correlation IDs.
Micrometer metrics to CloudWatch (or Prometheus/Grafana) + basic SLOs/alerts.
OpenTelemetry tracing (X-Ray/OTel Collector).