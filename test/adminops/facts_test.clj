(ns adminops.facts-test
  (:require [clojure.test :refer [deftest is]]
            [adminops.facts :as facts]))

(deftest jpn-has-a-spec-basis
  (is (some? (facts/spec-basis "JPN")))
  (is (string? (:provenance (facts/spec-basis "JPN")))))

(deftest all-four-seeded-jurisdictions-have-an-appeal-spec-basis
  ;; matching employmentops/7810's own full work-authorization,
  ;; practiceops/7110's own full professional-seal, quarryops/0810's
  ;; own full blast-safety and agronomyops/0162's own full water-
  ;; buffer sub-citation coverage, ALL FOUR seeded jurisdictions
  ;; actually have a real appeal-rights regime here -- reported
  ;; honestly, not forced narrower
  (doseq [iso3 ["JPN" "USA" "GBR" "DEU"]]
    (is (some? (facts/appeal-spec-basis iso3)) (str iso3 " appeal-spec-basis"))
    (is (string? (:appeal-provenance (facts/appeal-spec-basis iso3))) (str iso3 " appeal-provenance"))))

(deftest unknown-jurisdiction-has-no-fabricated-spec-basis
  (is (nil? (facts/spec-basis "ATL"))))

(deftest unknown-jurisdiction-has-no-appeal-spec-basis
  (is (nil? (facts/appeal-spec-basis "ATL"))))

(deftest coverage-never-reports-a-missing-jurisdiction-as-covered
  (let [report (facts/coverage ["JPN" "ATL" "GBR"])]
    (is (= 2 (:covered report)))
    (is (= ["ATL"] (:missing-jurisdictions report)))
    (is (= ["GBR" "JPN"] (:covered-jurisdictions report)))))

(deftest required-evidence-satisfied-needs-every-item
  (let [all (facts/evidence-checklist "JPN")]
    (is (facts/required-evidence-satisfied? "JPN" all))
    (is (not (facts/required-evidence-satisfied? "JPN" (rest all))))
    (is (not (facts/required-evidence-satisfied? "ATL" all)) "no spec-basis -> never satisfied")))
