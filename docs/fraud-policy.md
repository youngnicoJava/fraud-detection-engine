# Demonstration fraud policy 1.0.0

The policy ID/version is `loan-origination-fraud` / `1.0.0`. Thresholds are demonstration fraud policy values, not a bank's proprietary thresholds.

| Signal | Trigger | Points |
|---|---|---:|
| HIGH_APPLICATION_VELOCITY | At least 3 applications per customer reference in the inclusive 24-hour window | 35 |
| APPLICATION_AMOUNT_ESCALATION | Current amount is more than 50% above the latest prior application | 20 |
| MATERIAL_INCOME_CHANGE | Declared monthly income changes by more than 50% | 20 |
| MATERIAL_DEBT_CHANGE | Declared monthly debt changes by more than 50% | 15 |
| RAPID_RESUBMISSION_AFTER_BLOCK | Prior BLOCK for that reference within 24 hours | 60 |
| EXTREME_VELOCITY_WITH_AMOUNT_ESCALATION | At least 6 applications in 24 hours and current amount >50% above the latest | 60 |

The total is clamped to 0–100; it is not a calibrated fraud probability. LOW is 0–24, MEDIUM 25–59, HIGH 60–100. LOW maps to PASS, MEDIUM/HIGH to REVIEW. A recent resubmission after BLOCK or the combined extreme-velocity-and-escalation rule produces BLOCK. The combination makes the initial BLOCK decision reachable from observed history; individual profile changes are review signals only. There is no ML, external identity/device/bureau source or case workflow yet.
