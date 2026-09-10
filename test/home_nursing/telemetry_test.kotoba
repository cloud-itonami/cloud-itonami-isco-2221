(ns home-nursing.telemetry-test
  (:require [clojure.test :refer [deftest is testing]]
            [home-nursing.store :as store]
            [home-nursing.telemetry :as telemetry]))

(deftest clean-playthrough-proceeds-and-appends-visit
  (let [st (store/mem-store)
        record {:score 8 :picked 8 :lives-remaining 3 :violations 0 :outcome "victory" :ts 111}
        result (telemetry/ingest-playthrough! st record)]
    (is (= :proceed (:decision result)))
    (is (true? (:ingested? result)))
    (is (= 1 (count (store/visits-of st telemetry/session-patient-id))))
    (is (= 8 (get-in (first (store/visits-of st telemetry/session-patient-id)) [:vitals :rounds-cleared])))))

(deftest violated-playthrough-is-held-and-not-silently-appended
  (let [st (store/mem-store)
        record {:score 5 :picked 5 :lives-remaining 1 :violations 2 :outcome "gameover" :ts 222}
        result (telemetry/ingest-playthrough! st record)]
    (is (= :human-approval (:decision result)))
    (is (false? (:ingested? result)))
    (is (empty? (store/visits-of st telemetry/session-patient-id)))))

(deftest session-patient-is-registered-idempotently
  (let [st (store/mem-store)]
    (telemetry/ingest-playthrough! st {:score 1 :picked 1 :violations 0 :ts 1})
    (telemetry/ingest-playthrough! st {:score 2 :picked 2 :violations 0 :ts 2})
    (is (some? (store/patient st telemetry/session-patient-id)))
    (is (some? (store/care-plan-of st telemetry/session-patient-id)))
    (is (= 2 (count (store/visits-of st telemetry/session-patient-id))))))
