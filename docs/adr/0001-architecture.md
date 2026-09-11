# ADR-0001: AdminOps-LLM ⊣ Public Administration Governor architecture

## Status

Accepted. `cloud-itonami-isic-8411` promoted from `:blueprint` to
`:implemented` in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-8411` publishes an OSS business blueprint for
community public administration service (case intake, decision,
notification and appeal for community services). Like every prior
actor in this fleet, the blueprint alone is not an implementation:
this ADR records the governed-actor architecture that promotes it to
real, tested code, following the same langgraph StateGraph +
independent Governor + Phase 0→3 rollout pattern established by
`cloud-itonami-isic-6511` (life insurance) and applied across 93 prior
siblings, most recently `cloud-itonami-isic-7810` (community
employment agency).

A `kotoba-lang` org search for public-administration/government/
civic/municipal-named repos surfaced `cofog`. It was investigated: it
is the COFOG (UN Classification of the Functions of Government)
registry, the THIRD generic classification registry in this fleet
alongside `kotoba.industry` (ISIC-coded businesses) and
`kotoba.occupation` (ISCO-08-coded occupations) -- generic technology-
capability-resolution infrastructure, not a bespoke domain capability
library for public-administration case decision/notification business
records. This build returns to self-contained domain logic, the same
pattern the majority of this fleet's actors use.

This blueprint's own `:itonami.blueprint/governor` keyword,
`:public-administration-governor`, is grep-verified UNIQUE fleet-wide
-- no naming-collision precedent question, a fresh independent build
(clean on the first attempt, unlike `practiceops`/7110's own
collision case).

## Decision

### Decision 1: fresh governor identity, no reuse precedent needed

`:public-administration-governor` is grep-verified unique across
every blueprint.edn in this fleet. This build follows the SAME
governed-actor architecture as every prior actor, but with its own
distinct governor identity.

### Decision 2: dual-actuation shape, SEQUENTIAL on the SAME `case` entity

This blueprint's own operating states ("intake : register : decide :
notify : appeal : audit") name two real-world official acts: deciding
a case and notifying a citizen. These apply SEQUENTIALLY to the SAME
`case` entity -- decide first, notify later -- matching
`employmentops`/7810's, `practiceops`/7110's, `hospitalityops`/5510's,
`freightops`/4920's, `quarryops`/0810's and `agronomyops`/0162's own
sequential shape rather than `retailops`/4711's own alternative-kind
shape. `high-stakes` is `#{:actuation/decide-case :actuation/
notify-citizen}`.

### Decision 3: `assessed-fee-matches-claim?` -- an honest reapplication of the ground-truth-recompute discipline

`adminops.registry/assessed-fee-matches-claim?` (case's own claimed
assessed fee vs. base-amount x fee-rate) applies the SAME discipline
`employmentops.registry`'s own `placement-fee-matches-claim?`,
`practiceops.registry`'s own `fee-total-matches-claim?`,
`hospitalityops.registry`'s own `folio-total-matches-claim?` and
`agronomyops.registry`'s own `dose-matches-claim?` establish -- verify
a claimed monetary total against the entity's own recorded fields,
independent of proposal inspection. No literal code is shared
(different domain), but the discipline is the same, documented as
such rather than claimed as a novel invention.

### Decision 4: entity and op shape

The primary entity is a `case`. Four ops: `:case/intake` (directory
upsert, no decision-facing risk), `:jurisdiction/assess` (per-
jurisdiction administrative-procedure/appeal-rights evidence
checklist, never auto), `:case/decide` (POSITIVE, high-stakes), and
`:case/notify` (POSITIVE, high-stakes).

### Decision 5: `decision-outside-authority?` -- the 86th unconditional-evaluation grounding, the FLAGSHIP genuinely new check

Grep-verified absent fleet-wide (zero hits for `decision-outside-
authority`, `decision-authority-exceeded` as a governor check name).
Grounded in real administrative-procedure/ultra-vires law: Japan's
own 行政手続法/地方自治法 (Administrative Procedure Act / Local Autonomy
Act, enforced by MIC/local governments), the US's Administrative
Procedure Act §706 (agency action "in excess of statutory
jurisdiction, authority, or limitations"), the UK's ultra vires
doctrine under judicial-review principles (Anisminic v Foreign
Compensation Commission), and Germany's Verwaltungsverfahrensgesetz
(VwVfG) §44 (Nichtigkeit eines Verwaltungsaktes) -- directly grounded
in this blueprint's own text ("decisions outside authority are
blocked"). Evaluated UNCONDITIONALLY on every `:case/decide` (every
decision needs to be within the deciding official's own delegated
authority).

### Decision 6: `appeal-rights-notice-missing?` -- the 87th unconditional-evaluation grounding, the THIRTEENTH conditional variant

Before writing this check, every prior sibling's governor namespace
was grepped for any check function named `appeal-rights-notice` or
`appeal-rights-disclosed` -- zero hits, confirming this is a
genuinely new concept. This is the THIRTEENTH conditional variant
(after `socialresearch`/7220's, `bizassoc`/9411's, `training`/8549's,
`furniture`/9524's, `specialtyrepair`/9529's, `leathergoods`/9523's,
`ictrepair`/9511's, `quarryops`/0810's, `agronomyops`/0162's,
`hospitalityops`/5510's, `practiceops`/7110's and `employmentops`/
7810's own, at 63rd, 64th, 66th, 67th, 68th, 69th, 71st, 77th, 79th,
81st, 83rd and 85th) -- CONDITIONAL on the case's own `:decision-
adverse?` ground truth: not every decision is adverse to the citizen,
some are favorable grants that do not carry the same appeal-rights-
notice requirement. Grounded in real appeal-rights/notice-of-remedies
law: Japan's own 行政不服審査法第82条 (Administrative Appeal Act Article
82, 教示), the US's APA §555(e) (prompt notice of denial with a brief
statement of grounds), the UK's Tribunals, Courts and Enforcement Act
2007 (notice of appeal rights), and Germany's VwVfG §37 Abs. 6
(Rechtsbehelfsbelehrung -- literally "notice of legal remedies," a
mandatory element of an administrative act). ALL FOUR seeded
jurisdictions actually have a real regime here, reported honestly -- a
full-coverage sub-citation, matching `quarryops`/0810's own blast-
safety, `agronomyops`/0162's own water-buffer, `practiceops`/7110's
own professional-seal and `employmentops`/7810's own work-
authorization full coverage rather than `hospitalityops`/5510's own
honest single-jurisdiction gap.

### Decision 7: dedicated double-actuation-guard booleans

`:decided?`/`:notified?` are dedicated booleans on the `case` record,
never a single `:status` value -- the same discipline every prior
governor's guards establish, informed by `cloud-itonami-isic-6492`'s
real status-lifecycle bug (ADR-2607071320).

### Decision 8: Store protocol, MemStore + DatomicStore parity

`adminops.store/Store` is implemented by both `MemStore` (atom-
backed, default for dev/tests/demo) and `DatomicStore` (`langchain.
db`-backed), proven to satisfy the same contract in
`test/adminops/store_contract_test.kotoba`.

### Decision 9: no bespoke domain capability lib, and no `blueprint.edn` field-sync fixes needed beyond `:optional-technologies`

Verified explicitly this session: no `kotoba-lang/public-
administration`, `kotoba-lang/government`, `kotoba-lang/civic` or
`kotoba-lang/municipal`-style bespoke capability library exists;
`kotoba-lang/cofog` is the GENERIC COFOG government-function-
classification registry (the third such classification registry
alongside `kotoba.industry` and `kotoba.occupation`), not domain-
specific to public-administration case decision/notification. This
repo's `blueprint.edn` had the correct `:required-technologies`
matching the `kotoba-lang/industry` registry's own entry for `"8411"`
exactly, but was MISSING `:optional-technologies [:optimization]`
entirely -- the same gap pattern `agronomyops`/0162's,
`hospitalityops`/5510's, `practiceops`/7110's and `employmentops`/
7810's own builds found. Fixed cleanly in the same commit as the
`:maturity` flip.

### Decision 10: mock + LLM advisor pair

`adminops.adminopsllm` provides `mock-advisor` (deterministic,
default everywhere -- the actor graph and governor contract run
offline) and `llm-advisor` (backed by `langchain.model/ChatModel`,
with a defensive EDN-proposal parser so a malformed LLM response
degrades to a safe low-confidence noop rather than ever auto-deciding
a case or auto-notifying a citizen).

## Alternatives considered

- **An unconditional appeal-rights-notice check** (applying to every
  notification regardless of whether the decision is actually
  adverse). Rejected: favorable decisions do not carry the same
  appeal-rights-notice requirement -- forcing the check onto every
  notification would fabricate a requirement.
- **Fabricating a jurisdiction gap** to match `hospitalityops`/5510's
  own single-jurisdiction honesty gap. Rejected: the same honesty
  discipline that forbids fabricating coverage also forbids under-
  reporting it -- all four seeded jurisdictions genuinely have a real
  appeal-rights regime here.
- **Treating `kotoba-lang/cofog` as this vertical's capability
  library.** Considered and explicitly ruled out: it is generic
  COFOG government-function-classification infrastructure, not
  domain-specific business logic for case decision/notification.

## Consequences

- 95th actor in this fleet (94 implemented before this build).
- Establishes two genuinely NEW unconditional-evaluation-discipline
  checks: `decision-outside-authority?` (FLAGSHIP, 86th distinct
  application overall) and `appeal-rights-notice-missing?` (87th
  distinct application overall, the THIRTEENTH conditional variant).
- `MemStore` ‖ `DatomicStore` parity is proven by
  `test/adminops/store_contract_test.kotoba`.
- 39 tests / 176 assertions pass; lint is clean; the demo
  (`kbb -M:dev:run`) walks two clean decide+notify lifecycles
  (non-adverse decision, adverse decision with appeal rights
  disclosed), plus four HARD-hold scenarios, end-to-end.
- `blueprint.edn` needed a genuine field-sync fix this time (a
  missing `:optional-technologies [:optimization]` key) in addition
  to the `:maturity` flip.

## References

- `cloud-itonami-isic-6511/docs/adr/0001-architecture.md` (origin of
  the general governed-actor architecture pattern)
- `cloud-itonami-isic-7810/docs/adr/0001-architecture.md` (most recent
  prior sibling, template for this ADR's structure)
- 行政手続法 (Administrative Procedure Act); 地方自治法 (Local Autonomy
  Act); 行政不服審査法 (Administrative Appeal Act) Article 82 (Japan)
- Administrative Procedure Act (APA), 5 U.S.C. §706, §555(e) (US)
- Ultra vires doctrine (Anisminic v Foreign Compensation Commission);
  Tribunals, Courts and Enforcement Act 2007 (UK)
- Verwaltungsverfahrensgesetz (VwVfG) §37, §44 (Germany)
