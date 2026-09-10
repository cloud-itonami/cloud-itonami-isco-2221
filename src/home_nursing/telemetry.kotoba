(ns home-nursing.telemetry
  "ITONAMI play-data -> operating-ledger bridge (ADR-2607031000 addendum,
  2026-07-04): ingest a network-isekai `Home Nursing Rounds` playthrough
  record as a REAL operating record, through the SAME governor/store path a
  live proposal would use — never a parallel bypass. network-isekai has no
  server (static, CodePen-style host); the bridge is a JSON record the game
  host persists to localStorage (`isekai.itonami.log.<blueprint-id>`,
  `src/isekai/game.cljc` in network-isekai) that an operator exports and
  feeds to `ingest-playthrough!` here.

  A playthrough with governor violations (an unequipped/un-reviewed visit
  logged by the game) is submitted at :high safety-class / low confidence —
  the SAME hard invariant that forces human sign-off on a risky field visit
  forces human sign-off on ingesting a risky playthrough, too. This is
  deliberately a routine session summary, never `:urgent? true` — that flag
  is reserved for real urgent-risk cases, not simulated play sessions."
  (:require [home-nursing.store :as store]
            [home-nursing.governor :as governor]))

(def session-patient-id "network-isekai-session")

(defn ensure-session-patient!
  "Idempotently register the synthetic patient/care-plan a playthrough files
  its visit against — distinct from any real patient the operator tracks."
  [st]
  (when-not (store/patient st session-patient-id)
    (store/register-patient! st {:patient-id session-patient-id
                                  :consented-at "2026-07-04"}))
  (when-not (store/care-plan-of st session-patient-id)
    (store/register-care-plan! st {:patient-id session-patient-id
                                    :protocol "network-isekai-session"
                                    :requires-clinician-review? false})))

(defn ingest-playthrough!
  "Run a network-isekai playthrough record
  (`{:score :picked :lives-remaining :violations :outcome :ts}`, matching the
  JSON shape `src/isekai/game.cljc` persists) through the real
  HomeNursingGovernor and, only on :proceed, append it to the ledger via
  `record-visit!`. Returns the governor's decision map plus `:ingested?` and
  the original `:record`."
  [st {:keys [picked violations] :as record}]
  (ensure-session-patient! st)
  (let [env (governor/env-for-store st)
        proposal {:action :visit
                  :patient-id session-patient-id
                  :effect :propose
                  :urgent? false
                  :safety-class (if (pos? (or violations 0)) :high :low)
                  :confidence (if (pos? (or violations 0)) 0.4 0.9)}
        decision (governor/assess env proposal)
        proceed? (= :proceed (:decision decision))]
    (when proceed?
      (store/record-visit! st {:visit-id (str "playthrough-" (or (:ts record) picked))
                                :patient-id session-patient-id
                                :vitals {:rounds-cleared picked}
                                :urgent? false
                                :source :network-isekai
                                :raw record}))
    (assoc decision :ingested? proceed? :record record)))
