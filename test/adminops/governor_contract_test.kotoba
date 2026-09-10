(ns adminops.governor-contract-test
  "The governor contract as executable tests -- this vertical's own
  Trust Controls ('decisions outside authority are blocked;
  notifications are auditable; appeals are mandatory') implemented
  faithfully. The single invariant under test:

    AdminOps-LLM never decides a case or notifies a citizen the
    Public Administration Governor would reject, `:case/decide`/
    `:case/notify` NEVER auto-commit at any phase, `:case/intake` (no
    direct decision-facing risk) MAY auto-commit when clean, and every
    decision (commit OR hold) leaves exactly one ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [adminops.store :as store]
            [adminops.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :case-officer :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- assess!
  "Walks `subject` through assess -> approve, leaving an assessment on
  file. Uses distinct thread-ids per call site by suffixing
  `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-assess") {:op :jurisdiction/assess :subject subject} operator)
  (approve! actor (str tid-prefix "-assess")))

(defn- decide!
  "Walks `subject` through decide -> approve, leaving :decided? true.
  Assumes `assess!` already ran for this subject."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-decide") {:op :case/decide :subject subject} operator)
  (approve! actor (str tid-prefix "-decide")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :case/intake :subject "case-1"
                   :patch {:id "case-1" :applicant "Kita Taro"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Kita Taro" (:applicant (store/case-record db "case-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest jurisdiction-assess-always-needs-approval
  (testing "assess is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :jurisdiction/assess :subject "case-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/assessment-of db "case-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a jurisdiction/assess proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :jurisdiction/assess :subject "case-1" :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/assessment-of db "case-1")) "no assessment written"))))

(deftest decide-without-assessment-is-held
  (testing "case/decide before any jurisdiction assessment -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :case/decide :subject "case-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest decision-outside-authority-is-held-and-unoverridable
  (testing "a decision outside delegated authority -> HOLD, and never reaches request-approval -- the FLAGSHIP genuinely new check this vertical adds, the 86th unconditional-evaluation-discipline grounding overall, grounded in Japan's own 行政手続法/地方自治法, the US's APA §706, the UK's ultra vires doctrine and Germany's VwVfG §44"
    (let [[db actor] (fresh)
          _ (assess! actor "t5pre" "case-4")
          res (exec-op actor "t5" {:op :case/decide :subject "case-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:decision-outside-authority} (-> (store/ledger db) last :basis)))
      (is (empty? (store/decision-history db))))))

(deftest assessed-fee-mismatch-is-held
  (testing "a claimed assessed fee that doesn't equal base-amount x fee-rate -> HOLD (the ground-truth-recompute discipline every sibling's cost/total-matching check establishes)"
    (let [[db actor] (fresh)
          _ (assess! actor "t6pre" "case-3")
          _ (decide! actor "t6pre" "case-3")
          res (exec-op actor "t6" {:op :case/notify :subject "case-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:assessed-fee-mismatch} (-> (store/ledger db) last :basis)))
      (is (empty? (store/notification-history db))))))

(deftest appeal-rights-notice-missing-is-held-and-unoverridable
  (testing "a missing appeal-rights notice on an adverse-decision case -> HOLD, and never reaches request-approval -- a genuinely new check, the 87th unconditional-evaluation-discipline grounding overall, the THIRTEENTH conditional variant (see this actor's governor ns docstring / the full accumulated ADR-0001 chain: parksafety's ADR-2607071922 Decision 5 through leathergoods's, ictrepair's, retailops's, freightops's, quarryops's, agronomyops's, hospitalityops's, practiceops's and employmentops's own)"
    (let [[db actor] (fresh)
          _ (assess! actor "t7pre" "case-5")
          _ (decide! actor "t7pre" "case-5")
          res (exec-op actor "t7" {:op :case/notify :subject "case-5"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:appeal-rights-notice-missing} (-> (store/ledger db) last :basis)))
      (is (empty? (store/notification-history db))))))

(deftest notify-is-a-noop-when-decision-not-adverse
  (testing "the appeal-rights-notice check is CONDITIONAL: a non-adverse decision has no appeal-rights-notice requirement at all"
    (let [[_db actor] (fresh)
          _ (assess! actor "t7bpre" "case-1")
          _ (decide! actor "t7bpre" "case-1")
          res (exec-op actor "t7b" {:op :case/notify :subject "case-1"} operator)]
      (is (= :interrupted (:status res)) "clean notification still escalates for human sign-off, but is NOT a HARD hold"))))

(deftest notify-always-escalates-then-human-decides
  (testing "a clean, fully-assessed, matching-fee, non-adverse notification still ALWAYS interrupts for human approval -- actuation/notify-citizen is never auto"
    (let [[db actor] (fresh)
          _ (assess! actor "t8pre" "case-1")
          _ (decide! actor "t8pre" "case-1")
          r1 (exec-op actor "t8" {:op :case/notify :subject "case-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, notification record drafted"
        (let [r2 (approve! actor "t8")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:notified? (store/case-record db "case-1"))))
          (is (= 1 (count (store/notification-history db))) "one draft notification record"))))))

(deftest decide-always-escalates-then-human-decides
  (testing "a clean, fully-assessed decision still ALWAYS interrupts for human approval -- actuation/decide-case is never auto"
    (let [[db actor] (fresh)
          _ (assess! actor "t9pre" "case-1")
          r1 (exec-op actor "t9" {:op :case/decide :subject "case-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, decision record drafted"
        (let [r2 (approve! actor "t9")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:decided? (store/case-record db "case-1"))))
          (is (= 1 (count (store/decision-history db))) "one draft decision record"))))))

(deftest case-double-decision-is-held
  (testing "deciding the same case record twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t10pre" "case-1")
          _ (decide! actor "t10pre" "case-1")
          res (exec-op actor "t10" {:op :case/decide :subject "case-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-decided} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/decision-history db))) "still only the one earlier decision"))))

(deftest case-double-notification-is-held
  (testing "notifying the same case twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t11pre" "case-1")
          _ (decide! actor "t11pre" "case-1")
          _ (exec-op actor "t11a" {:op :case/notify :subject "case-1"} operator)
          _ (approve! actor "t11a")
          res (exec-op actor "t11" {:op :case/notify :subject "case-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-notified} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/notification-history db))) "still only the one earlier notification"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :case/intake :subject "case-1"
                          :patch {:id "case-1" :applicant "Kita Taro"}} operator)
      (exec-op actor "b" {:op :jurisdiction/assess :subject "case-1" :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
