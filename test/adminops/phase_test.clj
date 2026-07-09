(ns adminops.phase-test
  "The phase table as executable tests. The invariant this repo cannot
  regress on: `:case/decide`/`:case/notify` must NEVER be a member of
  any phase's `:auto` set."
  (:require [clojure.test :refer [deftest is testing]]
            [adminops.phase :as phase]))

(deftest case-decide-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real case decision"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :case/decide))
          (str "phase " n " must not auto-commit :case/decide")))))

(deftest case-notify-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real citizen notification"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :case/notify))
          (str "phase " n " must not auto-commit :case/notify")))))

(deftest phase-0-is-fully-read-only
  (is (empty? (:writes (get phase/phases 0)))))

(deftest phase-3-auto-commits-only-no-decision-facing-risk-ops
  (testing ":case/intake carries no direct decision-facing risk -- auto-eligible; it is the ONLY auto-eligible op in this domain"
    (is (= #{:case/intake} (:auto (get phase/phases 3))))))

(deftest gate-hold-always-wins
  (is (= :hold (:disposition (phase/gate 3 {:op :case/intake} :hold)))))

(deftest gate-escalates-a-clean-non-auto-write
  (is (= :escalate (:disposition (phase/gate 3 {:op :case/decide} :commit))))
  (is (= :escalate (:disposition (phase/gate 3 {:op :case/notify} :commit)))))

(deftest gate-holds-a-write-disabled-in-this-phase
  (is (= :hold (:disposition (phase/gate 0 {:op :case/intake} :commit)))))
