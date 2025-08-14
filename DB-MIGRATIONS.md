# Database Migrations (Liquibase)

## Policy
- Forward-only migrations. Do not modify past changesets after merge.
- Fix mistakes via new changesets only.
- Tag after each prod deploy to mark state.

## CI Steps
- Validate changelog: `./mvnw liquibase:validate`
- Show pending: `./mvnw liquibase:status -Dliquibase.status.verbose=true`
- Dry-run SQL: `./mvnw liquibase:updateSQL`

## Preconditions & Drift Detection
- Add preconditions with onFail="HALT" on critical objects:
```
<preConditions onFail="HALT">
  <tableExists tableName="drivers"/>
  <columnExists tableName="drivers" columnName="org_id"/>
</preConditions>
```
- Optionally add `<validCheckSum>` to pin a changeset checksum when necessary.

## Tagging Example
```
<changeSet id="2025-08-15-tag" author="ci">
  <tagDatabase tag="v2025_08_15_01"/>
</changeSet>
```

## Roll-forward Plan
- If a release must be undone, add new changesets to revert schema/data forward (no rollback scripts in prod).

## Environments
- `src/main/resources/liquibase.properties` is committed; CI overrides URL/user/pass for ephemeral Postgres.
- Staging/prod use the same master changelog; apply by tag.
