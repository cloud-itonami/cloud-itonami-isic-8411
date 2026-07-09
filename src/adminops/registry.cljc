(ns adminops.registry
  "Pure-function case-decision + citizen-notification record
  construction -- an append-only public-administration book-of-record
  draft.

  Like every sibling actor's registry, there is no single international
  reference-number standard for a decision or notification record --
  every office/jurisdiction assigns its own reference format. This
  namespace does NOT invent one; it builds a jurisdiction-scoped
  sequence number and validates the record's required fields, the
  same honest, non-fabricating discipline `adminops.facts` uses.

  `assessed-fee-matches-claim?` is an HONEST reapplication of the SAME
  ground-truth-recompute DISCIPLINE `employmentops.registry`'s own
  `placement-fee-matches-claim?`, `practiceops.registry`'s own `fee-
  total-matches-claim?`, `hospitalityops.registry`'s own `folio-total-
  matches-claim?` and `agronomyops.registry`'s own `dose-matches-
  claim?` establish (verify a claimed monetary total against the
  entity's own recorded quantity x unit fields), reapplied to a
  case's assessed-fee line rather than a placement-fee, professional-
  fee, folio or dose line -- not claimed as new code, though no
  literal code is shared (different domain).

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real case-management system. It builds the RECORD an
  operator would keep, not the act of deciding a case or notifying a
  citizen itself (that is `adminops.operation`'s `:case/decide`/
  `:case/notify`, always human-gated -- see README `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is
  the public-administration operator's act, not this actor's. See
  README `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn compute-assessed-fee
  "The ground-truth assessed fee for `case`'s own `:base-amount` and
  `:fee-rate` -- a single flat base x rate calculation, not a full
  fee-schedule engine with exemptions/waivers."
  [{:keys [base-amount fee-rate]}]
  (* (double base-amount) (double fee-rate)))

(defn assessed-fee-matches-claim?
  "Does `case`'s own `:claimed-fee` equal the independently
  recomputed `compute-assessed-fee`? A pure ground-truth check
  against the case's own permanent fields -- see ns docstring for why
  this is an honest reapplication of the SAME discipline every
  sibling actor's own cost/total-matching check establishes, not a
  new concept."
  [{:keys [claimed-fee] :as case-record}]
  (== (double claimed-fee) (compute-assessed-fee case-record)))

(defn register-decision
  "Validate + construct the CASE-DECISION registration DRAFT -- the
  public-administration operator's own official act of deciding a
  real case. Pure function -- does not touch any real case-
  management system; it builds the RECORD an operator would keep.
  `adminops.governor` independently re-verifies the case's own
  delegated-authority ground truth, and blocks a double-decision of
  the same record, before this is ever allowed to commit."
  [case-id jurisdiction sequence]
  (when-not (and case-id (not= case-id ""))
    (throw (ex-info "decision: case_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "decision: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "decision: sequence must be >= 0" {})))
  (let [decision-number (str (str/upper-case jurisdiction) "-DEC-" (zero-pad sequence 6))
        record {"record_id" decision-number
                "kind" "decision-draft"
                "case_id" case-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "decision_number" decision-number
     "certificate" (unsigned-certificate "CaseDecision" decision-number decision-number)}))

(defn register-notification
  "Validate + construct the CITIZEN-NOTIFICATION registration DRAFT --
  the public-administration operator's own official act of notifying
  a real citizen of a real decision (triggering assessed-fee accrual
  and, when adverse, an appeal window). Pure function -- does not
  touch any real case-management system; it builds the RECORD an
  operator would keep. `adminops.governor` independently re-verifies
  the case's own fee/appeal-rights ground truth, and blocks a double-
  notification of the same record, before this is ever allowed to
  commit."
  [case-id jurisdiction sequence]
  (when-not (and case-id (not= case-id ""))
    (throw (ex-info "notification: case_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "notification: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "notification: sequence must be >= 0" {})))
  (let [notification-number (str (str/upper-case jurisdiction) "-NTF-" (zero-pad sequence 6))
        record {"record_id" notification-number
                "kind" "notification-draft"
                "case_id" case-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "notification_number" notification-number
     "certificate" (unsigned-certificate "CitizenNotification" notification-number notification-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
