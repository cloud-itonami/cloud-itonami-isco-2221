# Operator Guide

## First Deployment

1. Define the operator's service area and intake process.
2. Define consent and purpose categories.
3. Run synthetic operating cases.
4. Enable human-reviewed sign-off for `:high`/`:safety-critical` actions.
5. Measure operating outcomes and audit coverage.

## A Day in the Life: intake → propose → approve → execute → audit

This is what the loop looks like for one actual home-nursing round, not an
abstract "task." Nurse Aiko runs a solo `cloud-itonami-isco-2221` practice
covering eight patients today, plus whatever comes in urgent.

1. **Intake.** A referring clinic sends a post-hip-surgery patient's referral.
   `:identity` binds the patient record to this practice; `:forms` captures
   consent + purpose and the clinician-authored care plan (scheduled vitals
   capture, a wound check, and a mobility transfer assist using the
   practice's telepresence/mobility robot). No visit is on the day's route
   until this exists.
2. **Propose.** At the start of the round, the Care Advisor reads the day's
   `:bpmn`-modeled visit workflow and proposes the next action: "capture
   vitals + assist transfer to chair at Patient 3's home, per care plan." It
   never proposes a medication change or a diagnosis — that stays with Aiko.
3. **Approve.** Before *any* patient-facing or robot action, the proposal
   goes to `:home-nursing-governor`. Routine, in-protocol vitals capture with
   consent on file clears automatically. The mobility/lifting transfer is
   `:safety-critical` — it holds until Aiko, the licensed clinician,
   signs off *for this visit specifically*. This is the equivalent of
   restocking at the clinic before a round: the sign-off is good for one
   visit, never a standing permit.
4. **Execute.** Only after sign-off does the robot perform the transfer
   assist and vitals capture; `:telemetry` streams the readings live. If a
   reading comes back outside protocol range — say an irregular heart rate
   mid-visit — that's an urgent-risk signal and it escalates to Aiko
   immediately; the governor will not let it be suppressed or queued for
   later, no matter what else is in progress.
5. **Audit.** Every governor decision (auto-approved, held, escalated), every
   clinician sign-off, and every disclosure (e.g. sending the visit summary
   back to the referring clinic) lands in the `:audit-ledger` with
   consent + purpose attached. This is what a certification reviewer or
   insurer later inspects (see Certification below).

Skip step 3 — visit the patient for a safety-critical action without a
same-visit sign-off — and that's a governor violation: the action is
refused, not just logged after the fact.

### Feel the loop hands-on

`network-isekai` has a playable prototype of exactly this approval-gate
loop: **ITONAMI: Home Nursing Rounds**
(`network-isekai/public/games/itonami/home-nursing-rounds`, served at
`/itonami/home-nursing-rounds`). You play the nurse: touch the **clinic**
depot to get this round's `:home-nursing-governor` care-plan sign-off, then
visit a **patient** to clear it — the sign-off is spent on that one visit,
so you have to go back to the clinic before the next one. Skip the clinic
and visit a patient anyway and it's an off-plan visit (you lose a life),
directly modeling "governor violation." One **critical-patient** per round
is worth 3x score and uses the exact same sign-off gate — the game's
version of an urgent-risk case that still has to clear the governor, it's
just higher-stakes. Clearing all 8 visits mirrors closing out a full round
with clean audit coverage. It's a fast, hands-on way for a new operator (or
a reviewer) to feel why the sign-off has to happen *before* the action, not
after.

## Minimum Production Controls

- consent and disclosure log
- safety-critical escalation path
- provenance for all operating records
- human review for high-risk cases
- audit export for all gated actions

## Certification

Certified operators must prove that the governor gates every safety-critical
robot action, and that safety-critical risks escalate to humans.
