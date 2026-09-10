(ns adminops.store
  "SSoT for the community-public-administration actor, behind a
  `Store` protocol so the backend is a swap, not a rewrite -- the
  same seam every prior `cloud-itonami-isic-*` actor in this fleet
  uses.

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/adminops/store_contract_test.clj), which is the whole point:
  the actor, the Public Administration Governor and the audit ledger
  never know which SSoT they run on.

  Like `employmentops`/7810's own `candidacy`, the primary entity here
  is a `case` -- case-decision and citizen-notification actuation
  events apply SEQUENTIALLY to the SAME case record (decide first,
  notify later), matching the freight/quarry/agronomy/hospitality/
  practice/employment cluster's own sequential entity shape.
  Dedicated double-actuation-guard booleans (`:decided?`/`:notified?`,
  never a `:status` value).

  The ledger stays append-only on every backend: 'which case was
  screened for a decision outside delegated authority or a missing
  appeal-rights notice, which case was decided, which citizen was
  notified, on what jurisdictional basis, approved by whom' is always
  a query over an immutable log -- the audit trail a municipality or
  community program trusting a public-administration operator needs,
  and the evidence an operator needs if a decision or a notification
  is later disputed."
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [adminops.registry :as registry]
            [langchain.db :as d]))

(defprotocol Store
  (case-record [s id])
  (all-cases [s])
  (assessment-of [s case-id] "committed jurisdiction assessment, or nil")
  (ledger [s])
  (decision-history [s] "the append-only case-decision history (adminops.registry drafts)")
  (notification-history [s] "the append-only citizen-notification history (adminops.registry drafts)")
  (next-decision-sequence [s jurisdiction] "next decision-number sequence for a jurisdiction")
  (next-notification-sequence [s jurisdiction] "next notification-number sequence for a jurisdiction")
  (case-already-decided? [s case-id] "has this case already been decided?")
  (case-already-notified? [s case-id] "has this case already been notified?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-cases [s cases] "replace/seed the case directory (map id->case)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained case set covering both actuation lifecycles
  (decide, notify) plus the governor's own new checks, so the actor +
  tests run offline."
  []
  {:cases
   {"case-1" {:id "case-1" :applicant "Kita Taro" :case-type "business-license"
              :base-amount 10000 :fee-rate 1.5 :claimed-fee 15000.0
              :within-delegated-authority? true
              :decision-adverse? false :appeal-rights-disclosed? false
              :decided? false :notified? false
              :jurisdiction "JPN" :status :intake}
    "case-2" {:id "case-2" :applicant "Atlantis Ann" :case-type "business-license"
              :base-amount 8000 :fee-rate 1.5 :claimed-fee 12000.0
              :within-delegated-authority? true
              :decision-adverse? false :appeal-rights-disclosed? false
              :decided? false :notified? false
              :jurisdiction "ATL" :status :intake}
    "case-3" {:id "case-3" :applicant "Minami Hana" :case-type "zoning-variance"
              :base-amount 12000 :fee-rate 1.5 :claimed-fee 20000.0
              :within-delegated-authority? true
              :decision-adverse? false :appeal-rights-disclosed? false
              :decided? false :notified? false
              :jurisdiction "JPN" :status :intake}
    "case-4" {:id "case-4" :applicant "Higashi Ichiro" :case-type "special-permit"
              :base-amount 15000 :fee-rate 1.5 :claimed-fee 22500.0
              :within-delegated-authority? false
              :decision-adverse? false :appeal-rights-disclosed? false
              :decided? false :notified? false
              :jurisdiction "JPN" :status :intake}
    "case-5" {:id "case-5" :applicant "Nishi Kenji" :case-type "benefit-denial"
              :base-amount 9000 :fee-rate 1.5 :claimed-fee 13500.0
              :within-delegated-authority? true
              :decision-adverse? true :appeal-rights-disclosed? false
              :decided? false :notified? false
              :jurisdiction "JPN" :status :intake}
    "case-6" {:id "case-6" :applicant "Chuo Yuki" :case-type "benefit-denial"
              :base-amount 9500 :fee-rate 1.5 :claimed-fee 14250.0
              :within-delegated-authority? true
              :decision-adverse? true :appeal-rights-disclosed? true
              :decided? false :notified? false
              :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- decide-case!
  "Backend-agnostic `:case/mark-decided` -- looks up the case via the
  protocol and drafts the decision record, and returns {:result ..
  :case-patch ..} for the caller to persist."
  [s case-id]
  (let [c (case-record s case-id)
        seq-n (next-decision-sequence s (:jurisdiction c))
        result (registry/register-decision case-id (:jurisdiction c) seq-n)]
    {:result result
     :case-patch {:decided? true
                 :decision-number (get result "decision_number")}}))

(defn- notify-citizen!
  "Backend-agnostic `:case/mark-notified` -- looks up the case via the
  protocol and drafts the notification record, and returns {:result
  .. :case-patch ..} for the caller to persist."
  [s case-id]
  (let [c (case-record s case-id)
        seq-n (next-notification-sequence s (:jurisdiction c))
        result (registry/register-notification case-id (:jurisdiction c) seq-n)]
    {:result result
     :case-patch {:notified? true
                 :notification-number (get result "notification_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (case-record [_ id] (get-in @a [:cases id]))
  (all-cases [_] (sort-by :id (vals (:cases @a))))
  (assessment-of [_ case-id] (get-in @a [:assessments case-id]))
  (ledger [_] (:ledger @a))
  (decision-history [_] (:decision-records @a))
  (notification-history [_] (:notification-records @a))
  (next-decision-sequence [_ jurisdiction] (get-in @a [:decision-sequences jurisdiction] 0))
  (next-notification-sequence [_ jurisdiction] (get-in @a [:notification-sequences jurisdiction] 0))
  (case-already-decided? [_ case-id] (boolean (get-in @a [:cases case-id :decided?])))
  (case-already-notified? [_ case-id] (boolean (get-in @a [:cases case-id :notified?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :case/upsert
      (swap! a update-in [:cases (:id value)] merge value)

      :assessment/set
      (swap! a assoc-in [:assessments (first path)] payload)

      :case/mark-decided
      (let [case-id (first path)
            {:keys [result case-patch]} (decide-case! s case-id)
            jurisdiction (:jurisdiction (case-record s case-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:decision-sequences jurisdiction] (fnil inc 0))
                       (update-in [:cases case-id] merge case-patch)
                       (update :decision-records registry/append result))))
        result)

      :case/mark-notified
      (let [case-id (first path)
            {:keys [result case-patch]} (notify-citizen! s case-id)
            jurisdiction (:jurisdiction (case-record s case-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:notification-sequences jurisdiction] (fnil inc 0))
                       (update-in [:cases case-id] merge case-patch)
                       (update :notification-records registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-cases [s cases] (when (seq cases) (swap! a assoc :cases cases)) s))

(defn seed-db
  "A MemStore seeded with the demo case set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :assessments {}
                           :ledger [] :decision-sequences {} :decision-records []
                           :notification-sequences {} :notification-records []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Map/compound values (assessment payloads, ledger facts,
  decision/notification records) are stored as EDN strings so
  `langchain.db` doesn't expand them into sub-entities -- the same
  convention every sibling actor's store uses."
  {:case/id                        {:db/unique :db.unique/identity}
   :assessment/case-id             {:db/unique :db.unique/identity}
   :ledger/seq                     {:db/unique :db.unique/identity}
   :decision-record/seq            {:db/unique :db.unique/identity}
   :notification-record/seq        {:db/unique :db.unique/identity}
   :decision-sequence/jurisdiction     {:db/unique :db.unique/identity}
   :notification-sequence/jurisdiction {:db/unique :db.unique/identity}})

(defn- enc [v] (pr-str v))
(defn- dec* [s] (when s (edn/read-string s)))

(defn- case->tx [{:keys [id applicant case-type base-amount fee-rate claimed-fee
                         within-delegated-authority?
                         decision-adverse? appeal-rights-disclosed?
                         decided? notified?
                         jurisdiction status decision-number notification-number]}]
  (cond-> {:case/id id}
    applicant                                       (assoc :case/applicant applicant)
    case-type                                           (assoc :case/case-type case-type)
    base-amount                                             (assoc :case/base-amount base-amount)
    fee-rate                                                   (assoc :case/fee-rate fee-rate)
    claimed-fee                                                   (assoc :case/claimed-fee claimed-fee)
    (some? within-delegated-authority?)                              (assoc :case/within-delegated-authority? within-delegated-authority?)
    (some? decision-adverse?)                                           (assoc :case/decision-adverse? decision-adverse?)
    (some? appeal-rights-disclosed?)                                       (assoc :case/appeal-rights-disclosed? appeal-rights-disclosed?)
    (some? decided?)                                                          (assoc :case/decided? decided?)
    (some? notified?)                                                           (assoc :case/notified? notified?)
    jurisdiction                                                                   (assoc :case/jurisdiction jurisdiction)
    status                                                                           (assoc :case/status status)
    decision-number                                                                     (assoc :case/decision-number decision-number)
    notification-number                                                                   (assoc :case/notification-number notification-number)))

(def ^:private case-pull
  [:case/id :case/applicant :case/case-type :case/base-amount :case/fee-rate :case/claimed-fee
   :case/within-delegated-authority? :case/decision-adverse? :case/appeal-rights-disclosed?
   :case/decided? :case/notified?
   :case/jurisdiction :case/status :case/decision-number :case/notification-number])

(defn- pull->case [m]
  (when (:case/id m)
    {:id (:case/id m) :applicant (:case/applicant m) :case-type (:case/case-type m)
     :base-amount (:case/base-amount m) :fee-rate (:case/fee-rate m) :claimed-fee (:case/claimed-fee m)
     :within-delegated-authority? (boolean (:case/within-delegated-authority? m))
     :decision-adverse? (boolean (:case/decision-adverse? m))
     :appeal-rights-disclosed? (boolean (:case/appeal-rights-disclosed? m))
     :decided? (boolean (:case/decided? m)) :notified? (boolean (:case/notified? m))
     :jurisdiction (:case/jurisdiction m) :status (:case/status m)
     :decision-number (:case/decision-number m) :notification-number (:case/notification-number m)}))

(defrecord DatomicStore [conn]
  Store
  (case-record [_ id]
    (pull->case (d/pull (d/db conn) case-pull [:case/id id])))
  (all-cases [_]
    (->> (d/q '[:find [?id ...] :where [?e :case/id ?id]] (d/db conn))
         (map #(pull->case (d/pull (d/db conn) case-pull [:case/id %])))
         (sort-by :id)))
  (assessment-of [_ case-id]
    (dec* (d/q '[:find ?p . :in $ ?cid
                :where [?a :assessment/case-id ?cid] [?a :assessment/payload ?p]]
              (d/db conn) case-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (decision-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :decision-record/seq ?s] [?e :decision-record/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (notification-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :notification-record/seq ?s] [?e :notification-record/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (next-decision-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :decision-sequence/jurisdiction ?j] [?e :decision-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (next-notification-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :notification-sequence/jurisdiction ?j] [?e :notification-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (case-already-decided? [s case-id]
    (boolean (:decided? (case-record s case-id))))
  (case-already-notified? [s case-id]
    (boolean (:notified? (case-record s case-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :case/upsert
      (d/transact! conn [(case->tx value)])

      :assessment/set
      (d/transact! conn [{:assessment/case-id (first path) :assessment/payload (enc payload)}])

      :case/mark-decided
      (let [case-id (first path)
            {:keys [result case-patch]} (decide-case! s case-id)
            jurisdiction (:jurisdiction (case-record s case-id))
            next-n (inc (next-decision-sequence s jurisdiction))]
        (d/transact! conn
                     [(case->tx (assoc case-patch :id case-id))
                      {:decision-sequence/jurisdiction jurisdiction :decision-sequence/next next-n}
                      {:decision-record/seq (count (decision-history s)) :decision-record/record (enc (get result "record"))}])
        result)

      :case/mark-notified
      (let [case-id (first path)
            {:keys [result case-patch]} (notify-citizen! s case-id)
            jurisdiction (:jurisdiction (case-record s case-id))
            next-n (inc (next-notification-sequence s jurisdiction))]
        (d/transact! conn
                     [(case->tx (assoc case-patch :id case-id))
                      {:notification-sequence/jurisdiction jurisdiction :notification-sequence/next next-n}
                      {:notification-record/seq (count (notification-history s)) :notification-record/record (enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (enc fact)}])
    fact)
  (with-cases [s cases]
    (when (seq cases) (d/transact! conn (mapv case->tx (vals cases)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:cases ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [cases]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-cases s cases))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo case set -- the Datomic-backed
  analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
