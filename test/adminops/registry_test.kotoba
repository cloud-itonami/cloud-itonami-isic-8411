(ns adminops.registry-test
  (:require [clojure.test :refer [deftest is]]
            [adminops.registry :as r]))

;; ----------------------------- assessed-fee-matches-claim? -----------------------------

(deftest matches-when-claim-equals-recompute
  (is (r/assessed-fee-matches-claim?
       {:base-amount 10000 :fee-rate 1.5 :claimed-fee 15000.0})))

(deftest mismatches-when-claim-differs-from-recompute
  (is (not (r/assessed-fee-matches-claim?
            {:base-amount 12000 :fee-rate 1.5 :claimed-fee 20000.0}))))

(deftest compute-assessed-fee-is-a-flat-base-times-rate
  (is (= 15000.0 (r/compute-assessed-fee {:base-amount 10000 :fee-rate 1.5}))))

;; ----------------------------- register-decision -----------------------------

(deftest decision-is-a-draft-not-a-real-decision
  (let [result (r/register-decision "case-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest decision-assigns-decision-number
  (let [result (r/register-decision "case-1" "JPN" 7)]
    (is (= (get result "decision_number") "JPN-DEC-000007"))
    (is (= (get-in result ["record" "case_id"]) "case-1"))
    (is (= (get-in result ["record" "kind"]) "decision-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest decision-validation-rules
  (is (thrown? Exception (r/register-decision "" "JPN" 0)))
  (is (thrown? Exception (r/register-decision "case-1" "" 0)))
  (is (thrown? Exception (r/register-decision "case-1" "JPN" -1))))

;; ----------------------------- register-notification -----------------------------

(deftest notification-is-a-draft-not-a-real-notification
  (let [result (r/register-notification "case-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest notification-assigns-notification-number
  (let [result (r/register-notification "case-1" "JPN" 7)]
    (is (= (get result "notification_number") "JPN-NTF-000007"))
    (is (= (get-in result ["record" "case_id"]) "case-1"))
    (is (= (get-in result ["record" "kind"]) "notification-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest notification-validation-rules
  (is (thrown? Exception (r/register-notification "" "JPN" 0)))
  (is (thrown? Exception (r/register-notification "case-1" "" 0)))
  (is (thrown? Exception (r/register-notification "case-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-decision "case-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-decision "case-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-DEC-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-DEC-000001" (get-in hist2 [1 "record_id"])))))
