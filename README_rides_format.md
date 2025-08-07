# Sample Rides Excel Format

This Excel file is used for manually testing the Mediroute ride ingestion and scheduling system.

## Columns

| Column      | Required | Description |
|-------------|----------|-------------|
| NAME        | ✅        | Patient's full name |
| PHONE       | ✅        | Contact phone number |
| PICK UP     | ✅        | Pickup address |
| DROP OFF    | ✅        | Drop-off address |
| PURPOSE     | ⛔        | Purpose of visit (e.g., Therapy, Methadone) |
| TIME        | ✅        | Appointment time (e.g., "08:30" or Excel numeric format) |
| DISTANCE    | ✅        | Distance in miles |
| NOTE        | ⛔        | Additional notes or instructions |
| CANCELLED   | ⛔        | "YES" to skip the ride |
| RETURN      | ⛔        | "YES" to generate return ride 1 hour later |
| SKILLS      | ⛔        | Comma-separated required driver skills (e.g., wheelchair, bilingual)

## Behavior

- Rows marked as `CANCELLED=YES` will be skipped.
- If `RETURN=YES`, a return ride from drop-off to pickup will be created +1 hour.
- Missing or malformed `TIME` values cause the ride to be skipped.
- Required fields: NAME, PHONE, PICK UP, DROP OFF, TIME, DISTANCE.