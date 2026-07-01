# cloud-itonami-isco-2221

Open Occupation Blueprint for **ISCO-08 2221**: Nursing Professionals.

This repository designs a forkable OSS business for an independently licensed nurse running a home-visit nursing practice: a mobility/telepresence robot performs the physical in-home support work under an actor that proposes care actions and an independent governor that gates them.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a mobility and telepresence robot assists with in-home vitals capture, lifting assist and remote check-ins under an actor that proposes
actions and an independent **Home Nursing Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
medication handling, wound care near patients, or lifting/transfer assist) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
patient consent + care plan + clinical protocol
        |
        v
Care Advisor -> Home Nursing Governor -> care, hold for clinician review, or escalate
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `2221`). Required capabilities:

- :robotics
- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger
- :telemetry

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation

`src/home_nursing/{store,governor}.cljc` is a minimal but real
implementation of the Core Contract above (pure cljc, no external deps):

- `home-nursing.store` — `Store` protocol + `MemStore`: patients, care
  plans, visits, medication events. A visit/medication event can only be
  recorded against a registered patient with a registered care plan (care-plan
  provenance).
- `home-nursing.governor` — `HomeNursingGovernor`: `assess` gates a proposal
  against the care-plan env. Hard invariants force `:hold` (no care plan, or
  a direct-write instead of `:propose`); `:urgent? true` proposals and
  medication-administration proposals **always** escalate to
  `:human-approval` regardless of safety-class or confidence — this cannot
  be suppressed; `:high`/`:safety-critical` and low-confidence proposals
  also escalate.

```bash
clojure -M:test   # 8 tests, 15 assertions, green
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation) —
the second `cloud-itonami-isco-*` occupation to reach that tier, after
`cloud-itonami-isco-6112` (ADR-2607012000).

## License

AGPL-3.0-or-later.
