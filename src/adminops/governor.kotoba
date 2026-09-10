(ns adminops.governor
  "Public Administration Governor -- the independent compliance layer
  that earns the AdminOps-LLM the right to commit. The LLM has no
  notion of jurisdictional administrative-procedure or appeal-rights
  law, whether a case's own claimed assessed fee actually equals base
  amount times fee rate, whether a proposed decision actually falls
  within the deciding official's own delegated authority, whether an
  adverse decision's own notification has actually disclosed appeal
  rights, or when an act stops being a draft and becomes a real-world
  case decision or citizen notification, so this MUST be a separate
  system able to *reject* a proposal and fall back to HOLD.

  `:itonami.blueprint/governor` is `:public-administration-governor`,
  grep-verified UNIQUE fleet-wide -- no naming-collision precedent
  question, a fresh independent build following the SAME governed-
  actor architecture (langgraph StateGraph + independent Governor +
  Phase 0->3 rollout) established by `cloud-itonami-isic-6511`.

  This blueprint's own text (docs/business-model.md's own Trust
  Controls: 'decisions outside authority are blocked; notifications
  are auditable; appeals are mandatory') and its own docs/operator-
  guide.md ('handling citizen data, official decisions and public-
  works dispatch' requiring human sign-off) name exactly the checks
  below.

  Seven checks, in priority order, ALL HARD violations: a human
  approver CANNOT override them. The confidence/actuation gate is
  SOFT: it asks a human to look (low confidence / actuation), and the
  human may approve -- but see `adminops.phase`: for `:stake
  :actuation/decide-case`/`:actuation/notify-citizen` (a real decision
  or notification) NO phase ever allows auto-commit either. Two
  independent layers agree that actuation is always a human call.

    1. Spec-basis                  -- did the jurisdiction proposal cite
                                       an OFFICIAL source
                                       (`adminops.facts`), or invent
                                       one?
    2. Evidence incomplete         -- for `:case/decide`/`:case/
                                       notify`, has the jurisdiction
                                       actually been assessed with a
                                       full evidence checklist on
                                       file?
    3. Decision outside authority  -- for `:case/decide`,
                                       INDEPENDENTLY verify the case's
                                       own `:within-delegated-
                                       authority?` is true -- the
                                       FLAGSHIP genuinely new check
                                       this vertical adds (grep-
                                       verified absent fleet-wide --
                                       zero hits for 'decision-
                                       outside-authority'/'decision-
                                       authority-exceeded' as a
                                       governor check function name),
                                       the 86th distinct application
                                       of the unconditional-evaluation
                                       discipline overall (most
                                       recently `employmentops.
                                       governor/matching-basis-
                                       discriminatory-violations` at
                                       84th, and `employmentops.
                                       governor/work-authorization-
                                       unverified-violations` at
                                       85th). Grounded in real
                                       administrative-procedure/
                                       ultra-vires law: Japan's own
                                       行政手続法/地方自治法 (Administrative
                                       Procedure Act / Local Autonomy
                                       Act, enforced by MIC/local
                                       governments), the US's
                                       Administrative Procedure Act
                                       §706 (agency action 'in excess
                                       of statutory jurisdiction,
                                       authority, or limitations'),
                                       the UK's ultra vires doctrine
                                       under judicial-review
                                       principles (Anisminic v Foreign
                                       Compensation Commission), and
                                       Germany's Verwaltungsverfahrens-
                                       gesetz (VwVfG) §44 (Nichtigkeit
                                       eines Verwaltungsaktes) --
                                       directly grounded in this
                                       blueprint's own text ('decisions
                                       outside authority are
                                       blocked'). Evaluated
                                       UNCONDITIONALLY (every decision
                                       needs to be within the deciding
                                       official's own delegated
                                       authority).
    4. Assessed fee mismatch       -- for `:case/notify`,
                                       INDEPENDENTLY recompute whether
                                       the case's own `:claimed-fee`
                                       equals `base-amount x fee-rate`
                                       (`adminops.registry/assessed-
                                       fee-matches-claim?`) -- an
                                       HONEST reapplication of the
                                       SAME ground-truth-recompute
                                       DISCIPLINE `employmentops.
                                       registry`'s/`practiceops.
                                       registry`'s/`hospitalityops.
                                       registry`'s own checks
                                       establish, reapplied to a
                                       case's assessed-fee line -- not
                                       claimed as new.
    5. Appeal rights notice
       missing                        -- for `:case/notify`, for a
                                       case whose own record declares
                                       `:decision-adverse? true` (i.e.
                                       this decision is actually
                                       adverse to the citizen -- not
                                       every decision is, some are
                                       favorable grants that do not
                                       carry the same appeal-rights-
                                       notice requirement),
                                       INDEPENDENTLY check whether
                                       `:appeal-rights-disclosed?` is
                                       true. A GENUINELY NEW concept
                                       (grep-verified absent fleet-
                                       wide -- zero hits for 'appeal-
                                       rights-notice'/'appeal-rights-
                                       disclosed' as a governor check
                                       function name), the 87th
                                       distinct application overall,
                                       the THIRTEENTH conditional
                                       variant (after
                                       `socialresearch`/7220's,
                                       `bizassoc`/9411's, `training`/
                                       8549's, `furniture`/9524's,
                                       `specialtyrepair`/9529's,
                                       `leathergoods`/9523's,
                                       `ictrepair`/9511's, `quarryops`/
                                       0810's, `agronomyops`/0162's,
                                       `hospitalityops`/5510's,
                                       `practiceops`/7110's and
                                       `employmentops`/7810's own, at
                                       63rd, 64th, 66th, 67th, 68th,
                                       69th, 71st, 77th, 79th, 81st,
                                       83rd and 85th). CONDITIONAL on
                                       the case's own `:decision-
                                       adverse?` ground truth.
                                       Grounded in real appeal-rights/
                                       notice-of-remedies law: Japan's
                                       own 行政不服審査法第82条 (Administrative
                                       Appeal Act Article 82, 教示 --
                                       notice of remedies, enforced by
                                       行政不服審査会), the US's APA
                                       §555(e) (prompt notice of
                                       denial with a brief statement
                                       of grounds), the UK's
                                       Tribunals, Courts and
                                       Enforcement Act 2007 (notice of
                                       appeal rights), and Germany's
                                       VwVfG §37 Abs. 6
                                       (Rechtsbehelfsbelehrung --
                                       literally 'notice of legal
                                       remedies', a mandatory element
                                       of an administrative act) --
                                       ALL FOUR seeded jurisdictions
                                       actually have a real regime
                                       here, reported honestly (a
                                       full-coverage sub-citation,
                                       matching `quarryops`/0810's own
                                       blast-safety, `agronomyops`/
                                       0162's own water-buffer,
                                       `practiceops`/7110's own
                                       professional-seal and
                                       `employmentops`/7810's own
                                       work-authorization full
                                       coverage rather than
                                       `hospitalityops`/5510's own
                                       honest single-jurisdiction
                                       gap).
    6. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:case/decide`/
                                       `:case/notify` (REAL acts) ->
                                       escalate.

  Two more guards, double-decision/double-notification prevention,
  are enforced but NOT listed as numbered HARD checks above because
  they need no upstream comparison at all -- `already-decided-
  violations`/`already-notified-violations` refuse to decide/notify
  the SAME case twice, off dedicated `:decided?`/`:notified?` facts
  (never a `:status` value) -- the SAME 'check a dedicated boolean,
  not status' discipline every prior governor's guards establish,
  informed by `cloud-itonami-isic-6492`'s status-lifecycle bug
  (ADR-2607071320)."
  (:require [adminops.facts :as facts]
            [adminops.procedure :as procedure]
            [adminops.registry :as registry]
            [adminops.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Deciding a real case and notifying a real citizen are the two
  real-world actuation events this actor performs -- a two-member
  set, matching every sibling's own dual-actuation shape."
  #{:actuation/decide-case :actuation/notify-citizen})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:jurisdiction/assess` (or `:case/decide`/`:case/notify`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent a jurisdiction's administrative-procedure/appeal-rights
  requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:jurisdiction/assess :case/decide :case/notify} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は法域要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:case/decide`/`:case/notify`, the jurisdiction's required
  intake/registration/decision evidence must actually be satisfied --
  do not trust the advisor's self-reported confidence alone."
  [{:keys [op subject]} st]
  (when (contains? #{:case/decide :case/notify} op)
    (let [c (store/case-record st subject)
          assessment (store/assessment-of st subject)]
      (when-not (and assessment
                     (facts/required-evidence-satisfied?
                      (:jurisdiction c) (:checklist assessment)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(受付記録/登録記録/決定記録/審査請求教示記録等)が充足していない状態での提案"}]))))

(defn- decision-outside-authority-violations
  "For `:case/decide`, INDEPENDENTLY verify the case's own `:within-
  delegated-authority?` is true -- the flagship genuinely new check
  this vertical adds. Evaluated UNCONDITIONALLY (every decision needs
  to be within the deciding official's own delegated authority)."
  [{:keys [op subject]} st]
  (when (= op :case/decide)
    (let [c (store/case-record st subject)]
      (when-not (true? (:within-delegated-authority? c))
        [{:rule :decision-outside-authority
          :detail (str subject " の決定が委任された権限の範囲外")}]))))

(defn- assessed-fee-mismatch-violations
  "For `:case/notify`, INDEPENDENTLY recompute whether the case's own
  claimed assessed fee equals base-amount x fee-rate via
  `adminops.registry/assessed-fee-matches-claim?` -- needs no
  proposal inspection or stored-verdict lookup at all, an honest
  reapplication of the same discipline every sibling actor's own
  cost/total-matching check establishes."
  [{:keys [op subject]} st]
  (when (= op :case/notify)
    (let [c (store/case-record st subject)]
      (when-not (registry/assessed-fee-matches-claim? c)
        [{:rule :assessed-fee-mismatch
          :detail (str subject " の申告手数料(" (:claimed-fee c)
                      ")が独立再計算値(" (registry/compute-assessed-fee c) ")と一致しない")}]))))

(defn- appeal-rights-notice-missing-violations
  "For `:case/notify`, for a case whose own record declares
  `:decision-adverse? true`, INDEPENDENTLY check whether `:appeal-
  rights-disclosed?` is true -- a genuinely new concept, CONDITIONAL
  on the case's own `:decision-adverse?` ground truth (not every
  decision is adverse to the citizen)."
  [{:keys [op subject]} st]
  (when (= op :case/notify)
    (let [c (store/case-record st subject)]
      (when (and (true? (:decision-adverse? c))
                 (not (true? (:appeal-rights-disclosed? c))))
        [{:rule :appeal-rights-notice-missing
          :detail (str subject " は不利益決定だが審査請求教示が未実施 -- 通知提案は進められない")}]))))

(defn- already-decided-violations
  "For `:case/decide`, refuses to decide the SAME case record twice,
  off a dedicated `:decided?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :case/decide)
    (when (store/case-already-decided? st subject)
      [{:rule :already-decided
        :detail (str subject " は既に決定済み")}])))

(defn- already-notified-violations
  "For `:case/notify`, refuses to notify the SAME case twice, off a
  dedicated `:notified?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :case/notify)
    (when (store/case-already-notified? st subject)
      [{:rule :already-notified
        :detail (str subject " は既に通知済み")}])))

(defn- statutory-deadline-violations
  "For `:case/decide`: recompute the statutory deadline INDEPENDENTLY from
  `kotoba-lang/tetsuzuki` and the case's own records, and HOLD when the
  agency may not decide.

  Three HARD rules, all specific to AUTHORITY-side deadlines (the kind
  this actor's original checks had no notion of at all):

  - `:procedure-deadline-unresolved` -- the deadline could not be
    resolved (no basis, no anchor day, needs a business calendar, needs
    a real calendar date). Fail closed: an agency that cannot say
    whether it is inside its own statutory period does not get to
    decide. `:not-declared` cases are exempt (not every community case
    maps to a catalogued statutory procedure), but a case that DECLARES
    a `:procedure-id` is always checked.

  - `:decision-precluded-by-lapse` -- a deeming effect has ALREADY taken
    legal effect (DEU VwVfG §42a deemed granted / CAN ATIA s.10(3)
    deemed refused). The agency cannot now 'decide' what the law has
    already deemed; layering a decision on top would contradict a legal
    fiction that is already in force.

  - `:procedure-formal-review-incomplete` -- the declared procedure's own
    formal-review items are not all satisfied."
  [{:keys [op subject]} context st]
  (when (= op :case/decide)
    (let [c (store/case-record st subject)
          ctx {:anchors (:anchors context) :calendar (:calendar context)}
          status (procedure/deadline-status c ctx)
          review (procedure/formal-review-outcome c)]
      (seq
       (cond-> []
         (procedure/deadline-blocking? status)
         (conj {:rule :procedure-deadline-unresolved
                :detail (procedure/explain c ctx)})

         (procedure/decision-precluded? c ctx)
         (conj {:rule :decision-precluded-by-lapse
                :detail (str "みなし処分が既に効力を生じている: "
                             (procedure/explain c ctx)
                             " —— 擬制と矛盾する処分を重ねられない")})

         (contains? #{:deficient :unknown} review)
         (conj {:rule :procedure-formal-review-incomplete
                :detail (str "宣言された手続きの形式審査が " review)}))))))

(defn check
  "Censors an AdminOps-LLM proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (decision-outside-authority-violations request st)
                           (assessed-fee-mismatch-violations request st)
                           (appeal-rights-notice-missing-violations request st)
                           (statutory-deadline-violations request _context st)
                           (already-decided-violations request st)
                           (already-notified-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
