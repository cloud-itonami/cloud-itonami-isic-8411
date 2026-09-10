(ns adminops.phase
  "Phase 0->3 staged rollout for the community-public-administration
  actor.

    Phase 0  read-only        -- no writes, still governor-gated.
    Phase 1  assisted-intake  -- case intake allowed, every write
                                 needs human approval.
    Phase 2  assisted-assess  -- adds jurisdiction assessment writes,
                                 still approval.
    Phase 3  supervised auto  -- governor-clean, high-confidence
                                 `:case/intake` (no decision-facing
                                 risk yet) may auto-commit. `:case/
                                 decide`/`:case/notify` NEVER auto-
                                 commit, at any phase.

  `:case/decide`/`:case/notify` are deliberately ABSENT from every
  phase's `:auto` set, including phase 3 -- a permanent structural
  fact, not a rollout milestone still to come. Deciding a real case
  and notifying a real citizen are the two real-world official acts
  this actor performs; both are always a human case officer/
  authorized official's call. `adminops.governor`'s `:actuation/
  decide-case`/`:actuation/notify-citizen` high-stakes gate enforces
  the same invariant independently -- two layers, not one, agree on
  this. Like every prior sibling's phase 3 `:auto` set, this domain
  has only ONE member (`:case/intake`) -- no separate no-decision-
  facing-risk 'file' lifecycle distinct from the case itself.")

(def read-ops  #{})
(def write-ops #{:case/intake :jurisdiction/assess :case/decide :case/notify})

;; NOTE the invariant: `:case/decide`/`:case/notify` are members of
;; `write-ops` (governor-gated like any write) but are NEVER members
;; of any phase's `:auto` set below. Do not add them there.
(def phases
  "phase -> {:label .. :writes <ops allowed to write> :auto <ops allowed to
  auto-commit when governor-clean>}."
  {0 {:label "read-only"       :writes #{}                                                        :auto #{}}
   1 {:label "assisted-intake" :writes #{:case/intake}                                             :auto #{}}
   2 {:label "assisted-assess" :writes #{:case/intake :jurisdiction/assess}                         :auto #{}}
   3 {:label "supervised-auto" :writes write-ops
      :auto #{:case/intake}}})

(def default-phase 3)

(defn gate
  "Adjust a governor disposition for the rollout phase. Returns
  {:disposition kw :reason kw|nil}.

  - a governor HOLD always stays HOLD (compliance wins).
  - a write op not yet enabled in this phase -> HOLD (:phase-disabled).
  - a write op enabled but not auto-eligible -> ESCALATE (:phase-approval),
    even if the governor was clean.
  - `:case/decide`/`:case/notify` are never auto-eligible at any
    phase, so they always escalate once the governor clears them (or
    hold if the governor doesn't)."
  [phase {:keys [op]} governor-disposition]
  (let [{:keys [writes auto]} (get phases phase (get phases default-phase))]
    (cond
      (= :hold governor-disposition)       {:disposition :hold :reason nil}
      (contains? read-ops op)              {:disposition governor-disposition :reason nil}
      (not (contains? writes op))          {:disposition :hold :reason :phase-disabled}
      (and (= :commit governor-disposition)
           (not (contains? auto op)))      {:disposition :escalate :reason :phase-approval}
      :else                                {:disposition governor-disposition :reason nil})))

(defn verdict->disposition
  "Map a Public Administration Governor verdict to a base disposition
  before the phase gate."
  [verdict]
  (cond (:hard? verdict) :hold
        (:escalate? verdict) :escalate
        :else :commit))
