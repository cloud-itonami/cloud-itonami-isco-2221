(ns home-nursing.governor
  "HomeNursingGovernor — the independent safety/traceability layer for the
  ISCO-08 2221 independent home-nursing actor. The Care Advisor proposes
  actions (visit, administer medication); it has no notion of care-plan
  provenance or urgent-risk escalation, so this MUST be a separate system
  able to *reject* a proposal and fall back to HOLD — the itonami-actor
  pattern (independent Governor gates a proposing actor) applied to this
  occupation.

  Charter (mirrors ADR-2607011000 robotics premise + ADR-2607012000
  cloud-itonami-isco): the actor never dispatches a robot action or writes an
  operating record the governor refuses. Medication administration and
  urgent-risk cases ALWAYS require human sign-off, even when every hard
  invariant passes — the itonami analog of a no-diagnosis-by-LLM charter.

  HARD invariants for :nursing/propose:
    1. Care-plan provenance — a visit or medication event must reference a
       registered patient with a registered care-plan.
    2. No-actuation         — the proposal must not directly mutate a visit
       or medication record outside the record-visit!/record-medication!
       path (effect must be :propose, never a raw store write).
    3. Urgent-risk escalation — a proposal flagged `:urgent? true` can never
       be `:proceed`; it is always routed to `:human-approval`, regardless
       of safety-class or confidence. This cannot be suppressed.
  SOFT:
    4. Medication administration always escalates to human sign-off (no
       autonomous medication administration, robot or otherwise).
    5. Confidence floor → escalate."
  (:require [home-nursing.store :as store]))

(def confidence-floor 0.6)
(def safety-classes [:none :low :medium :high :safety-critical])

(defn- safety-rank [safety-class]
  (let [idx (.indexOf safety-classes safety-class)]
    (if (neg? idx) 0 idx)))

(defn- hard-violations [{:keys [care-plan-fn]} proposal]
  (let [{:keys [patient-id effect]} proposal
        care-plan (care-plan-fn patient-id)]
    (cond-> []
      (nil? care-plan)
      (conj {:rule :no-care-plan :detail (str "未登録 care-plan " patient-id)})

      (not= :propose effect)
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"}))))

(defn assess
  "Assess a proposal against `env` (a map with `:care-plan-fn` lookup,
  decoupled from any concrete Store so this stays pure). Returns
  `{:decision :proceed|:hold|:human-approval :violations [...] :confidence n}`."
  [env proposal]
  (let [violations (hard-violations env proposal)
        safety-class (or (:safety-class proposal) :none)
        confidence (or (:confidence proposal) 1.0)
        urgent? (boolean (:urgent? proposal))
        medication? (= :medication (:action proposal))]
    (cond
      (seq violations)
      {:decision :hold :violations violations :confidence confidence}

      urgent?
      {:decision :human-approval :violations [] :confidence confidence
       :reason :urgent-risk}

      medication?
      {:decision :human-approval :violations [] :confidence confidence
       :reason :medication-administration}

      (>= (safety-rank safety-class) (safety-rank :high))
      {:decision :human-approval :violations [] :confidence confidence}

      (< confidence confidence-floor)
      {:decision :human-approval :violations [] :confidence confidence
       :reason :low-confidence}

      :else
      {:decision :proceed :violations [] :confidence confidence})))

(defn env-for-store
  "Build the decoupled env map `assess` needs from a concrete
  `home-nursing.store/Store` implementation."
  [store]
  {:care-plan-fn #(store/care-plan-of store %)})
