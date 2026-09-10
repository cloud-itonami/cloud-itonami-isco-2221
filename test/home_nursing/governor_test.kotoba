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

(deftest holds-on-orphaned-care-plan-with-no-patient-record
  ;; register-patient!/register-care-plan! are two independent Store
  ;; writes with no atomic combined operation -- a care-plan can exist
  ;; for a patient-id that was never registered. hard-violations used to
  ;; check ONLY care-plan-fn, so this orphaned state slipped through as a
  ;; clean pass. Same gap already found and fixed in the sibling
  ;; ISCO-1212 HR-management governor (:no-employee-record).
  (let [st (store/mem-store)
        _ (store/register-care-plan! st {:patient-id "orphan-pat" :protocol "post-op-checkin"})
        env (governor/env-for-store st)
        proposal {:action :visit :patient-id "orphan-pat" :safety-class :low
                   :effect :propose :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-patient-record (:rule %)) (:violations result)))))

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

(deftest holds-on-unrecognized-safety-class-instead-of-silently-proceeding
  ;; safety-rank used .indexOf, which returns -1 (not an exception) for a
  ;; value outside safety-classes, silently ranked as 0 == :none -- an
  ;; unrecognized safety-class (typo, wrong type, or unexpected Advisor
  ;; output) used to bypass the mandatory human-approval gate for
  ;; :high/:safety-critical proposals instead of failing closed.
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :visit :patient-id "pat-1" :safety-class :extreme
                   :effect :propose :confidence 1.0}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :invalid-safety-class (:rule %)) (:violations result)))))

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

(deftest a-proposal-without-confidence-does-not-proceed
  (testing "確信度を言っていない提案は、確信していると言っていないので auto-proceed
            させない。この既定は 2026-07-30 まで 1.0 で、:confidence を持たない提案が
            :proceed していた（ADR-2607309100）。fleet の boolean 方言 346 件はすべて
            0.0 既定で、うち isco-5419 はそれを明示的にテストしている。"
    (let [st (fresh-store)
          env (governor/env-for-store st)
          proposal {:action :visit :patient-id "pat-1" :safety-class :low :effect :propose}
          result (governor/assess env proposal)]
      (is (= 0.0 (:confidence result))
          "欠落した :confidence は 0.0 であって 1.0 ではない")
      (is (not= :proceed (:decision result))
          "確信度不明の提案が自動で通ってはならない"))))
