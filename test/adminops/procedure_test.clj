(ns adminops.procedure-test
  "**法定手続きの期限**が `:case/decide` を止める契約。

  この actor が元々持っていた検査は、決定の権限・手数料・不服申立教示・
  二重処分を見ていたが、**その決定が法定期限との関係でまだ可能なのか**は
  誰も見ていなかった。行政庁を縛る期限は徒過すると法域ごとに違う効果を
  生み、うち 2 つ（みなし許可・みなし拒否）は**既に効力を生じている**ので、
  そこへ行政庁が別の決定を重ねることはできない。

  中心の不変条件:

  1. 期限は case 自身の記録から**独立に再計算**する（提案の主張を読まない）。
  2. 解けない期限は fail-closed —— 自分が法定期間の内にいるか言えない
     行政庁に決定させない。
  3. `:procedure-id` を宣言していない案件は素通りする（コミュニティ行政の
     案件すべてが一般カタログに載るわけではない）。**宣言したら必ず検査。**"
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [adminops.governor :as governor]
            [adminops.operation :as op]
            [adminops.procedure :as procedure]
            [adminops.store :as store]))

(def base-case
  {:applicant "Kita Taro" :case-type "disclosure"
   :base-amount 10000 :fee-rate 1.5 :claimed-fee 15000.0
   :within-delegated-authority? true
   :decision-adverse? false :appeal-rights-disclosed? true
   :decided? false :notified? false :status :intake})

(def cases
  {;; カナダ ATIA: 30 日を過ぎている -> みなし拒否が既に効力を生じている
   "ca-lapsed" (merge base-case
                      {:id "ca-lapsed" :jurisdiction "CAN"
                       :procedure-id :ca-atip-request
                       :observed-epoch-day 20500
                       :review-items-satisfied
                       ["request in writing" "sufficient detail to locate the record"
                        "application fee"]})
   ;; 同じ手続きで期限内
   "ca-within" (merge base-case
                      {:id "ca-within" :jurisdiction "CAN"
                       :procedure-id :ca-atip-request
                       :observed-epoch-day 20470
                       :review-items-satisfied
                       ["request in writing" "sufficient detail to locate the record"
                        "application fee"]})
   ;; ドイツ §42a: 月単位なので epoch-day では解けない -> fail-closed
   "de-months" (merge base-case
                      {:id "de-months" :jurisdiction "DEU"
                       :procedure-id :de-genehmigungsfiktion
                       :observed-epoch-day 20600
                       :review-items-satisfied ["vollständiger Antrag"
                                                "Zuständigkeit der Behörde"]})
   ;; 形式審査が未了
   "ca-deficient" (merge base-case
                         {:id "ca-deficient" :jurisdiction "CAN"
                          :procedure-id :ca-atip-request
                          :observed-epoch-day 20470
                          :review-items-satisfied ["request in writing"]})
   ;; 表に無い手続きを宣言している
   "atl-nobasis" (merge base-case
                        {:id "atl-nobasis" :jurisdiction "ATL"
                         :procedure-id :atl-made-up
                         :observed-epoch-day 20470
                         :review-items-satisfied []})
   ;; 手続きを宣言していない（この actor 本来のコミュニティ案件）
   "plain" (merge base-case
                  {:id "plain" :jurisdiction "JPN" :status :intake})})

(def operator
  {:actor-id "clerk-1" :actor-role :case-officer :phase 3
   :anchors {:filing-received 20460}})

(defn- fresh []
  (let [db (store/with-cases (store/seed-db) cases)]
    [db (op/build db)]))

(defn- exec-op
  ([actor tid request] (exec-op actor tid request operator))
  ([actor tid request context]
   (g/run* actor {:request request :context context} {:thread-id tid})))

(defn- dispo [res] (get-in res [:state :disposition]))
(defn- basis-set [db] (into #{} (mapcat :basis (store/ledger db))))

;; ---------------------------------------------------------------- 単体

(deftest deadline-is-recomputed-from-the-case-not-the-proposal
  (let [ctx {:anchors {:filing-received 20460}}]
    (is (= :lapsed (procedure/deadline-status (get cases "ca-lapsed") ctx)))
    (is (= :within (procedure/deadline-status (get cases "ca-within") ctx)))
    (is (= :not-declared (procedure/deadline-status (get cases "plain") ctx)))
    (is (= :no-basis (procedure/deadline-status (get cases "atl-nobasis") ctx)))))

(deftest deemed-refusal-precludes-a-decision
  (let [ctx {:anchors {:filing-received 20460}}]
    (is (= :deemed-refused (procedure/lapse-effect (get cases "ca-lapsed") ctx)))
    (is (true? (procedure/decision-precluded? (get cases "ca-lapsed") ctx))
        "既に効力を生じた擬制へ別の決定を重ねられない")
    (is (false? (procedure/decision-precluded? (get cases "ca-within") ctx)))))

(deftest exhausts-remedy-does-not-preclude-a-decision
  (testing "米英 FOIA の徒過は請求者に選択肢を与えるだけで、決定を妨げない"
    (is (not (contains? procedure/precluding-lapse-effects :exhausts-remedy)))
    (is (not (contains? procedure/precluding-lapse-effects :none)))
    (is (contains? procedure/precluding-lapse-effects :deemed-granted))
    (is (contains? procedure/precluding-lapse-effects :deemed-refused))))

(deftest unresolved-deadlines-fail-closed
  (doseq [s [:no-basis :unknown-anchor :needs-business-calendar
             :needs-calendar-date :unknown-observation :authority-published]]
    (is (true? (procedure/deadline-blocking? s)) (str s " が素通りする")))
  (is (false? (procedure/deadline-blocking? :within)))
  (is (false? (procedure/deadline-blocking? :not-declared))
      "手続きを宣言していない案件は素通りしてよい"))

;; ---------------------------------------------------------------- actor 経由

(deftest lapsed-deeming-holds-the-decision
  (let [[db actor] (fresh)
        res (exec-op actor "p1" {:op :case/decide :subject "ca-lapsed"})]
    (is (= :hold (dispo res)))
    (is (contains? (basis-set db) :decision-precluded-by-lapse))))

(deftest months-unit-holds-rather-than-approximating
  (testing "ドイツ §42a は月単位 —— 30 日で近似せず、解けないので止まる"
    (let [[db actor] (fresh)
          res (exec-op actor "p2" {:op :case/decide :subject "de-months"})]
      (is (= :hold (dispo res)))
      (is (contains? (basis-set db) :procedure-deadline-unresolved)))))

(deftest missing-anchor-holds
  (testing "基準日を渡さない運用は全部止まる（煩わしいが静かに決定するより良い）"
    (let [[db actor] (fresh)
          res (exec-op actor "p3" {:op :case/decide :subject "ca-within"}
                       (assoc operator :anchors {}))]
      (is (= :hold (dispo res)))
      (is (contains? (basis-set db) :procedure-deadline-unresolved)))))

(deftest declared-but-uncatalogued-procedure-holds
  (let [[db actor] (fresh)
        res (exec-op actor "p4" {:op :case/decide :subject "atl-nobasis"})]
    (is (= :hold (dispo res)))
    (is (contains? (basis-set db) :procedure-deadline-unresolved))))

(deftest incomplete-formal-review-holds
  (let [[db actor] (fresh)
        res (exec-op actor "p5" {:op :case/decide :subject "ca-deficient"})]
    (is (= :hold (dispo res)))
    (is (contains? (basis-set db) :procedure-formal-review-incomplete))))

(deftest undeclared-case-is-not-blocked-by-this-check
  (testing "コミュニティ案件すべてが一般カタログに載るわけではない"
    (let [db (store/with-cases (store/seed-db) cases)
          v (governor/check {:op :case/decide :subject "plain"} operator
                            {:confidence 0.9} db)
          rules (into #{} (map :rule (:violations v)))]
      (is (not (contains? rules :procedure-deadline-unresolved)))
      (is (not (contains? rules :decision-precluded-by-lapse)))
      (is (not (contains? rules :procedure-formal-review-incomplete))))))

(deftest a-within-deadline-case-passes-this-check
  (let [db (store/with-cases (store/seed-db) cases)
        v (governor/check {:op :case/decide :subject "ca-within"} operator
                          {:confidence 0.9} db)
        rules (into #{} (map :rule (:violations v)))]
    (is (not (contains? rules :procedure-deadline-unresolved)))
    (is (not (contains? rules :decision-precluded-by-lapse)))
    (is (not (contains? rules :procedure-formal-review-incomplete)))))
