(ns adminops.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the
  sibling actor."
  (:require [clojure.test :refer [deftest is testing]]
            [adminops.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "JPN" (:jurisdiction (store/case-record s "case-1"))))
      (is (= 15000.0 (:claimed-fee (store/case-record s "case-1"))))
      (is (true? (:within-delegated-authority? (store/case-record s "case-1"))))
      (is (false? (:decision-adverse? (store/case-record s "case-1"))))
      (is (= 20000.0 (:claimed-fee (store/case-record s "case-3"))))
      (is (false? (:within-delegated-authority? (store/case-record s "case-4"))))
      (is (true? (:decision-adverse? (store/case-record s "case-5"))))
      (is (false? (:appeal-rights-disclosed? (store/case-record s "case-5"))))
      (is (true? (:appeal-rights-disclosed? (store/case-record s "case-6"))))
      (is (false? (:decided? (store/case-record s "case-1"))))
      (is (false? (:notified? (store/case-record s "case-1"))))
      (is (= ["case-1" "case-2" "case-3" "case-4" "case-5" "case-6"]
             (mapv :id (store/all-cases s))))
      (is (nil? (store/assessment-of s "case-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/decision-history s)))
      (is (= [] (store/notification-history s)))
      (is (zero? (store/next-decision-sequence s "JPN")))
      (is (zero? (store/next-notification-sequence s "JPN")))
      (is (false? (store/case-already-decided? s "case-1")))
      (is (false? (store/case-already-notified? s "case-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :case/upsert
                                 :value {:id "case-1" :applicant "Kita Taro"}})
        (is (= "Kita Taro" (:applicant (store/case-record s "case-1"))))
        (is (= 15000.0 (:claimed-fee (store/case-record s "case-1"))) "unrelated field preserved"))
      (testing "assessment payloads commit and read back"
        (store/commit-record! s {:effect :assessment/set :path ["case-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/assessment-of s "case-1"))))
      (testing "decision drafts a record and advances the decision sequence"
        (store/commit-record! s {:effect :case/mark-decided :path ["case-1"]})
        (is (= "JPN-DEC-000000" (get (first (store/decision-history s)) "record_id")))
        (is (= "decision-draft" (get (first (store/decision-history s)) "kind")))
        (is (true? (:decided? (store/case-record s "case-1"))))
        (is (= 1 (count (store/decision-history s))))
        (is (= 1 (store/next-decision-sequence s "JPN")))
        (is (true? (store/case-already-decided? s "case-1"))))
      (testing "notification drafts a record and advances the notification sequence"
        (store/commit-record! s {:effect :case/mark-notified :path ["case-1"]})
        (is (= "JPN-NTF-000000" (get (first (store/notification-history s)) "record_id")))
        (is (= "notification-draft" (get (first (store/notification-history s)) "kind")))
        (is (true? (:notified? (store/case-record s "case-1"))))
        (is (= 1 (count (store/notification-history s))))
        (is (= 1 (store/next-notification-sequence s "JPN")))
        (is (true? (store/case-already-notified? s "case-1"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/case-record s "nope")))
    (is (= [] (store/all-cases s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/decision-history s)))
    (is (= [] (store/notification-history s)))
    (is (zero? (store/next-decision-sequence s "JPN")))
    (is (zero? (store/next-notification-sequence s "JPN")))
    (store/with-cases s {"x" {:id "x" :applicant "a" :case-type "t"
                              :base-amount 1 :fee-rate 1.0 :claimed-fee 1.0
                              :within-delegated-authority? true
                              :decision-adverse? false :appeal-rights-disclosed? false
                              :decided? false :notified? false
                              :jurisdiction "JPN" :status :intake}})
    (is (= "a" (:applicant (store/case-record s "x"))))))
