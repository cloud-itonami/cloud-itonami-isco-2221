(ns home-nursing.store
  "SSoT for the ISCO-08 2221 independent home-nursing sole-proprietor actor,
  behind a `Store` protocol so the backend is a swap (MemStore default ‖ a
  real Datomic/kotoba-server backend, per the itonami actor pattern).

  Domain = independent home-visit nursing practice:

    patient      — a client under care (patientId, consentedAt)
    care-plan    — the clinical protocol for a patient (planId, patientId,
                   protocol, requiresClinicianReview?)
    visit        — a scheduled/completed home visit (visitId, patientId,
                   vitals, urgent? boolean)
    medication   — a medication-administration event (medEventId, patientId,
                   drug, dose, administeredBy #{:robot :nurse})

  The append-only records are the operating ledger: a visit or medication
  event must reference a registered patient with a registered care-plan, and
  visits/medication events are never mutated in place, only appended.")

(defprotocol Store
  (patient [st patient-id])
  (care-plan-of [st patient-id])
  (visits-of [st patient-id])
  (medications-of [st patient-id])
  (register-patient! [st patient])
  (register-care-plan! [st care-plan])
  (record-visit! [st visit])
  (record-medication! [st medication]))

(defrecord MemStore [state]
  Store
  (patient [_ patient-id]
    (get-in @state [:patients patient-id]))
  (care-plan-of [_ patient-id]
    (get-in @state [:care-plans patient-id]))
  (visits-of [_ patient-id]
    (filter #(= patient-id (:patient-id %)) (:visits @state)))
  (medications-of [_ patient-id]
    (filter #(= patient-id (:patient-id %)) (:medications @state)))
  (register-patient! [_ patient]
    (swap! state assoc-in [:patients (:patient-id patient)] patient))
  (register-care-plan! [_ care-plan]
    (swap! state assoc-in [:care-plans (:patient-id care-plan)] care-plan))
  (record-visit! [_ visit]
    (swap! state update :visits (fnil conj []) visit))
  (record-medication! [_ medication]
    (swap! state update :medications (fnil conj []) medication)))

(defn mem-store
  ([] (mem-store {}))
  ([seed]
   (->MemStore (atom (merge {:patients {} :care-plans {} :visits [] :medications []} seed)))))
