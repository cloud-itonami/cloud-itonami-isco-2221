# Business Model: Independent Home Nursing Practice

## Classification

- Repository: `cloud-itonami-isco-2221`
- ISCO-08: `2221`
- Occupation: Nursing Professionals
- Social impact: care-access, patient-safety, aging-in-place

## Customer

- home-care patients and families
- clinics referring home-visit care
- insurers and benefit programs

## Offer

- consented intake and care planning
- scheduled home visits
- vitals and medication documentation
- urgent-risk escalation
- care outcome reporting

## Revenue

- per-visit fee
- monthly care-plan management fee
- insurance billing support fee

## Trust Controls

- no diagnosis or medication change by LLM
- urgent-risk escalation cannot be suppressed
- care actions near the patient require clinician sign-off
- health data disclosure requires consent and purpose

## `:home-nursing-governor` — the decision rule

The governor is the independent checkpoint between the Care Advisor's
*proposal* and any robot action, disclosure, or record write. It never
dispatches hardware itself (see the Core Contract in the README); it only
approves, holds for clinician review, or escalates. Concretely, for
`cloud-itonami-isco-2221` (Independent Home Nursing Practice) it applies:

**Approves** — a proposed action if all of:
- it is on the patient's signed care plan and within the clinical protocol
  for that visit (e.g. scheduled vitals capture, a documented medication
  reminder, a mobility/lifting transfer using the practice's telepresence
  robot),
- consent + purpose are on file for the data the action touches, and
- for `:high`/`:safety-critical` steps (wound care near the patient,
  lifting/transfer assist, anything the robot does within arm's reach of the
  patient) a licensed clinician has signed off *for that specific visit* —
  a standing blanket approval does not count.

**Rejects / holds**:
- any medication change or diagnosis the LLM/Care Advisor proposes on its
  own — nursing judgment and prescribing authority stay with the licensed
  clinician, never the model (`:health/home-nursing` is a licensed
  scope-of-practice domain, not a general chat assistant),
- any safety-critical robot action attempted without a same-visit clinician
  sign-off,
- any health-data disclosure (to an insurer, referring clinic, or family
  member) that lacks matching consent + stated purpose.

**Cannot be suppressed** — an urgent-risk signal (e.g. vitals telemetry
outside protocol range, a fall event) always escalates to a human, even if
that means overriding an in-progress, already-approved action.

This rule is a direct read of the blueprint's `:social-impact` tags:
- **`:patient-safety`** — the reject/hold branch: nothing safety-critical
  moves without a same-visit human sign-off, and escalation can't be
  silenced.
- **`:care-access`** — the approve branch: routine, protocol-conformant,
  already-consented visits clear the governor without manual friction, so
  the practice can actually serve patients at volume instead of every visit
  waiting on a human.
- **`:aging-in-place`** — the reason the robot exists at all: lifting,
  transfer, and remote-presence assist are gated *robot* actions precisely
  so a patient can keep receiving hands-on-adjacent care at home instead of
  being moved to a facility.

## Required Technologies — what each is for in this business

`blueprint.edn`'s `:required-technologies` resolve via
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation) for
ISCO-08 `2221`. In this specific practice each one has a concrete job:

- **`:robotics`** — the mobility/telepresence robot that performs the
  physical in-home work (positioning for vitals capture, lifting/transfer
  assist, remote check-in presence) strictly under actor-propose /
  governor-gate control; it is the reason a solo licensed nurse can safely
  cover a home-visit caseload.
- **`:identity`** — binds the patient's consent record to *this* patient and
  binds each dispatched action to *this* licensed nurse/clinician's
  credentials, so sign-off and disclosure both have a verifiable, non-forgeable
  actor behind them.
- **`:forms`** — captures intake/consent, the care plan itself, and
  point-of-care documentation (vitals readings, medication administration,
  wound-care notes) as structured records the governor and auditor can
  actually evaluate, not free text.
- **`:dmn`** — encodes the home-nursing-governor's approve/hold/escalate
  rules above as decision tables (e.g. which vitals ranges are in-protocol
  vs. trigger urgent-risk escalation), so the rule is inspectable and
  testable rather than buried in prompt text.
- **`:bpmn`** — models the intake → propose → approve → execute → audit
  visit workflow as an executable process: referral intake, visit
  scheduling, and the sequencing of robot-assisted steps within a visit.
- **`:audit-ledger`** — the append-only record of every governor decision,
  clinician sign-off, and data disclosure for this practice — what a
  certification review or insurer audit actually inspects (see
  Certification in `docs/operator-guide.md`).
- **`:telemetry`** — the live feed from the robot and any bedside sensors
  (vitals, fall detection) that feeds the urgent-risk escalation path; this
  is what makes "urgent-risk escalation cannot be suppressed" enforceable
  in real time rather than only at chart-review time.
