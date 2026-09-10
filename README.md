# cloud-itonami-isic-8411

Open Business Blueprint for **ISIC Rev.5 8411**: public administration
-- case intake, decision, notification and appeal for community
services.

This repository publishes a community-public-administration actor --
case intake, per-jurisdiction administrative-procedure/appeal-rights
regulatory assessment, case decision and citizen notification -- as an
OSS business that any qualified operator can fork, deploy, run,
improve and sell, so a municipality or community program never
surrenders case and decision data to a closed government-services
SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet (94 prior actors) -- here it is
**AdminOps-LLM ⊣ Public Administration Governor**. This blueprint's
own `:itonami.blueprint/governor` keyword,
`:public-administration-governor`, is a UNIQUE keyword fleet-wide
(grep-verified: no other blueprint declares it) -- a fresh,
independent build.

> **Why an actor layer at all?** An LLM is great at drafting a case
> summary, normalizing records, and checking whether a claimed
> assessed fee actually equals a case's own recorded base amount
> times fee rate -- but it has **no notion of which jurisdiction's
> administrative-procedure/appeal-rights law is official, no license
> to decide a real case or notify a real citizen, and no way to know
> on its own whether a proposed decision actually falls within the
> deciding official's own delegated authority or whether an adverse
> decision's own notification has actually disclosed appeal rights**.
> Letting it decide or notify directly invites fabricated regulatory
> citations, a fee mismatch being charged to a citizen, a decision
> issued outside the deciding official's own authority, and an adverse
> decision being notified without appeal-rights disclosure -- exposing
> the operator to real regulatory liability and citizens to real
> due-process harm. This project seals the AdminOps-LLM into a single
> node and wraps it with an independent **Public Administration
> Governor**, a human **approval workflow**, and an immutable **audit
> ledger**.

## Scope: what this actor does and does not do

This actor covers case intake through administrative-procedure/
appeal-rights regulatory assessment, case decision and citizen
notification. It does **not**, by itself, hold any operating
authority required to administer public services in a given
jurisdiction, and it does not claim to. It also does not perform the
actual case investigation/adjudication work itself, or judge case
merits -- `adminops.registry/assessed-fee-matches-claim?` is a pure
ground-truth recompute against the case's own recorded fields, not a
merits judgment. Whoever deploys and operates a live instance (a
qualified case officer/authorized official) supplies any
jurisdiction-specific authority, the real case-management-system
integration and the real citizen-notification integrations, and bears
that jurisdiction's liability -- the software supplies the governed,
spec-cited, audited execution scaffold so that operator does not have
to build the compliance layer from scratch.

### Actuation

**Deciding a real case and notifying a real citizen are never
autonomous, at any phase, by construction.** Two independent layers
enforce this (`adminops.governor`'s `:actuation/decide-case`/
`:actuation/notify-citizen` high-stakes gate and `adminops.phase`'s
phase table, which never puts either op in any phase's `:auto` set)
-- see `adminops.phase`'s docstring and `test/adminops/phase_test.
clj`'s `case-decide-never-auto-at-any-phase`/`case-notify-never-auto-
at-any-phase`. The actor may draft, check and recommend; a human case
officer/authorized official is always the one who actually decides a
case or notifies a citizen. Grounded directly in this blueprint's own
`docs/business-model.md` Trust Controls text ("decisions outside
authority are blocked; notifications are auditable; appeals are
mandatory") -- a genuine DUAL-actuation shape, applied SEQUENTIALLY to
the SAME case record (decide first, notify later), matching
`employmentops`/7810's, `practiceops`/7110's, `hospitalityops`/5510's,
`freightops`/4920's, `quarryops`/0810's and `agronomyops`/0162's own
sequential shape rather than `retailops`/4711's own alternative-kind
shape.

### Statutory deadlines (`kotoba-lang/tetsuzuki`)

The original checks asked whether the deciding official had authority,
whether the fee matched, whether appeal rights were disclosed and whether
the case was already decided. None of them asked **whether the agency is
still inside its own statutory period** — and for an authority-side
deadline, lapsing does not merely mean "late". It means different things
in different jurisdictions, and two of them are already legally decisive:

| | On lapse |
|---|---|
| DEU VwVfG §42a | the application is **deemed granted** |
| CAN ATIA s.10(3) | access is **deemed refused** |
| USA FOIA §552(a)(6)(C) | the requester gains standing to sue |
| JPN 行政手続法 §6 | 標準処理期間 — **no automatic legal effect** |

Where a deeming effect has already taken hold, the agency **cannot now
decide** what the law has already deemed; layering a decision on top
would contradict a legal fiction that is already in force. That is the
`:decision-precluded-by-lapse` HARD check.

Two further disciplines:

- **Unresolved deadlines fail closed.** No anchor day, a months-based
  period that cannot be resolved in epoch-days, a business-day period
  with no holiday calendar, a procedure absent from the catalog — all
  hold. An agency that cannot say whether it is inside its own statutory
  period does not get to decide. This is deliberately inconvenient.
- **Undeclared cases pass through.** Not every community case maps to a
  catalogued statutory procedure, and absence is not a violation. But a
  case that *declares* a `:procedure-id` is always checked — declaring it
  means accepting its deadline.

The deadline is recomputed from the case's own records; the proposal's
claim is never read as an input.

## The core contract

```
case intake + jurisdiction facts (adminops.facts, spec-cited)
        |
        v
   ┌───────────────────────┐   proposal      ┌───────────────────────┐
   │ AdminOps-LLM          │ ─────────────▶ │ Public Administration Governor │  (independent system)
   │ (sealed)              │  + citations    │ spec-basis · evidence-       │
   └───────────────────────┘                 │ incomplete · decision-        │
          │                 commit ◀┼ outside-authority (FLAGSHIP NEW) ·    │
          │                         │ assessed-fee-mismatch (ground-        │
    record + ledger        escalate ┼ truth) · appeal-rights-notice-            │
          │              (ALWAYS for│ missing (conditional, NEW) · already-     │
          │       :actuation/decide-│ decided · already-notified                │
          │       case/             │                                            │
          │       :actuation/notify-│                                            │
          │       citizen}           │                                            │
          ▼                          └───────────────────────┘
      human approval
```

**The AdminOps-LLM never decides a case or notifies a citizen the
Public Administration Governor would reject, and never does so
without a human sign-off.** Hard violations (fabricated regulatory
requirements; unsupported evidence; a decision outside delegated
authority; an assessed-fee mismatch; a missing appeal-rights notice
on an adverse decision; a double decision/notification) force **hold**
and *cannot* be approved past; a clean decision/notification proposal
still always routes to a human.

## Run

```bash
clojure -M:dev:run     # walk two clean decide+notify lifecycles (non-adverse decision, adverse decision with appeal rights disclosed), plus four HARD-hold cases, through the actor
clojure -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
clojure -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a service robot performs
intake, document handling and dispatch at the counter and in the
field, under the actor, gated by the independent **Public
Administration Governor**. The governor never dispatches hardware
itself; `:high`/`:safety-critical` actions (such as handling citizen
data, official decisions and public-works dispatch) require human
sign-off.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Public Administration Governor, decision/notification draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`8411`). This vertical's case/decision records are practice-specific
rather than a shared cross-operator data contract, so `adminops.*`
runs on the generic robotics/identity/forms/dmn/bpmn/audit-ledger
stack only -- no bespoke domain capability lib to reference at all
(unlike `retailops`/4711's own `kotoba-lang/retail` and `freightops`/
4920's own `kotoba-lang/logistics` integrations; `kotoba-lang/cofog`
is the generic COFOG government-function-classification registry --
the third such classification registry in this fleet, alongside
`kotoba.industry` and `kotoba.occupation` -- not a bespoke domain
capability library, matching `quarryops`/0810's, `agronomyops`/0162's,
`hospitalityops`/5510's, `practiceops`/7110's and `employmentops`/
7810's own investigated-and-ruled-out precedent).

## Layout

| File | Role |
|---|---|
| `src/adminops/store.kotoba` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + decision AND notification history (dual history). The double-actuation guard checks dedicated `:decided?`/`:notified?` booleans rather than a `:status` value |
| `src/adminops/registry.kotoba` | Decision/notification draft records, plus `assessed-fee-matches-claim?` -- an honest reapplication of the SAME ground-truth-recompute discipline every sibling actor's own cost/total-matching check establishes |
| `src/adminops/facts.kotoba` | Per-jurisdiction administrative-procedure AND appeal-rights catalog with an official spec-basis citation per entry, honest coverage reporting -- ALL FOUR seeded jurisdictions have an appeal-rights sub-citation here |
| `src/adminops/adminopsllm.kotoba` | **AdminOps-LLM** -- `mock-advisor` ‖ `llm-advisor`; intake/jurisdiction-assessment/decision/notification proposals |
| `src/adminops/governor.kotoba` | **Public Administration Governor** -- 5 HARD checks (spec-basis · evidence-incomplete · decision-outside-authority, FLAGSHIP NEW, the 86th unconditional-evaluation-discipline grounding · assessed-fee-mismatch · appeal-rights-notice-missing, CONDITIONAL, the 87th grounding) + 2 double-actuation guards + 1 soft (confidence/actuation gate) |
| `src/adminops/phase.kotoba` | **Phase 0→3** -- read-only → assisted intake → assisted assess → supervised (decide/notify always human; case intake is the ONLY auto-eligible op, no direct decision-facing risk) |
| `src/adminops/operation.kotoba` | **OperationActor** -- langgraph StateGraph |
| `src/adminops/sim.kotoba` | demo driver |
| `test/adminops/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage |

## Business-process coverage (honest)

This actor covers case intake through administrative-procedure/
appeal-rights regulatory assessment, case decision and citizen
notification -- the core governed lifecycle this blueprint's own
`docs/business-model.md` names in its Offer:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Case intake + per-jurisdiction evidence checklisting, HARD-gated on an official spec-basis citation (`:case/intake`/`:jurisdiction/assess`) | Real case-management-system integration, real case-merits judgment (see `adminops.facts`'s docstring) |
| Case decision, HARD-gated on full evidence and delegated-authority compliance, plus a double-decision guard (`:actuation/decide-case`) | |
| Citizen notification, HARD-gated on full evidence, a matching fee claim and (when applicable) appeal-rights disclosure, plus a double-notification guard (`:actuation/notify-citizen`) | |
| Immutable audit ledger for every intake/assessment/decision/notification decision | |

Extending coverage is additive: add the next gate (e.g. an appeal-
resolution-completion-verification check) as its own governed op with
its own HARD checks and tests, following the SAME "an independent
governor re-verifies against the actor's own records before any
real-world act" pattern this repo's flagship ops already establish.

## Jurisdiction coverage (honest)

`adminops.facts/coverage` reports how many requested jurisdictions
actually have an official spec-basis in `adminops.facts/catalog` --
currently 4 seeded (JPN, USA, GBR, DEU) out of ~194 jurisdictions
worldwide. This is a starting catalog to prove the governor contract
end-to-end, not a claim of global coverage. Adding a jurisdiction is
additive: one map entry in `adminops.facts/catalog`, citing a real
official source -- never fabricate a jurisdiction's requirements to
make coverage look bigger. Note that the appeal-rights sub-citation is
FULL coverage rather than a gap: ALL FOUR seeded jurisdictions (JPN,
USA, GBR, DEU) actually have a real appeal-rights enforcement regime,
reported honestly.

## Maturity

`:implemented` -- `AdminOps-LLM` + `Public Administration Governor`
run as real, tested code (see `Run` above), promoted from the
originally-published `:blueprint`-tier scaffold, following the SAME
governed-actor architecture as the 94 other prior actors across this
fleet, with its own distinct, independently-named governor. See
`docs/adr/0001-architecture.md` for the history and design.

## License

Code and implementation templates are AGPL-3.0-or-later.
