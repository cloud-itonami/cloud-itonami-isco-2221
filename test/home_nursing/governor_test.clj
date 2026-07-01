(ns home-nursing.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [home-nursing.store :as store]
            [home-nursing.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-patient! st {:patient-id "pat-1" :consented-at "2026-01-01"})
    (store/register-care-plan! st {:patient-id "pat-1" :protocol "post-op-checkin"})
    st))

(deftest proceeds-on-clean-routine-visit
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :visit :patient-id "pat-1" :safety-class :low
                   :effect :propose :confidence 0.9}]
    (is (= :proceed (:decision (governor/assess env proposal))))))

(deftest holds-on-unregistered-care-plan
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :visit :patient-id "no-such-patient" :safety-class :low
                   :effect :propose :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-care-plan (:rule %)) (:violations result)))))

(deftest holds-on-no-actuation-violation
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :visit :patient-id "pat-1" :safety-class :low
                   :effect :direct-write :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-actuation (:rule %)) (:violations result)))))

(deftest urgent-risk-always-escalates-even-when-clean-and-confident
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :visit :patient-id "pat-1" :safety-class :none
                   :effect :propose :confidence 1.0 :urgent? true}
        result (governor/assess env proposal)]
    (is (= :human-approval (:decision result)))
    (is (= :urgent-risk (:reason result)))))

(deftest medication-administration-always-escalates
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :medication :patient-id "pat-1" :safety-class :none
                   :effect :propose :confidence 1.0}
        result (governor/assess env proposal)]
    (is (= :human-approval (:decision result)))
    (is (= :medication-administration (:reason result)))))

(deftest human-approval-on-high-safety-class-even-when-clean
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :visit :patient-id "pat-1" :safety-class :high
                   :effect :propose :confidence 0.9}]
    (is (= :human-approval (:decision (governor/assess env proposal))))))

(deftest human-approval-on-low-confidence
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :visit :patient-id "pat-1" :safety-class :none
                   :effect :propose :confidence 0.2}
        result (governor/assess env proposal)]
    (is (= :human-approval (:decision result)))
    (is (= :low-confidence (:reason result)))))

(deftest store-records-append-only
  (let [st (fresh-store)]
    (store/record-visit! st {:visit-id "v1" :patient-id "pat-1" :vitals {:hr 72}})
    (store/record-medication! st {:med-event-id "m1" :patient-id "pat-1" :drug "acetaminophen"})
    (is (= 1 (count (store/visits-of st "pat-1"))))
    (is (= 1 (count (store/medications-of st "pat-1"))))
    (is (empty? (store/visits-of st "pat-2")))))
