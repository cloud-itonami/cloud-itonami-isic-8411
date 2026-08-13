# Operator Quickstart — Community Public Administration Service

Shortest path from clone to a verified local dry-run for **ISIC 8411** (`cloud-itonami-isic-8411`).

## Prerequisites

- Clojure 1.12+ (`clojure --version`)
- Java 17+
- Git

No invented metrics; this is a governed OSS blueprint, not a hosted SaaS demo.

## 1. Clone

```bash
git clone https://github.com/cloud-itonami/cloud-itonami-isic-8411.git
cd cloud-itonami-isic-8411
```

## 2. Run tests

```bash
clojure -M:test
```

Expect green if maturity is `implemented`. Fix failures before operating.

## 3. Open the product face

```bash
open docs/index.html   # or: python3 -m http.server -d docs 8080
```

Publish: enable GitHub Pages on `main` `/docs`, or any static host.

## 3b. See the Governor actually refuse things

```bash
clojure -M:dev:render-html   # regenerates docs/samples/operator-console.html
open docs/samples/operator-console.html
```

`adminops.render-html` drives the real actor (`adminops.operation` →
`adminops.governor` → `adminops.store`) over the seeded cases and renders
whatever comes back — every case id, amount, decision number and violation
string on that page is read out of the store after the run. It reaches every
HARD check the Governor implements, and **throws instead of writing the page**
if a run produces no HARD hold, so the sample cannot quietly decay into a page
that shows the Governor approving everything.

Output is deterministic (no timestamps, no randomness, no anchor dates) — render
twice into a scratch directory and diff to confirm.

## 4. Where the Governor sits

- Blueprint governor key: `public-administration-governor`
- Likely source path: `adminops.governor.cljc`
- Pattern: advise → govern → phase-gate → commit | escalate | hold (itonami actor / ADR-2607011000)

## 5. Claim / go-live

- Free claim funnel: https://itonami.cloud/isco-1212/
- Paid path docs: https://itonami.cloud/docs/go-live.md
- Blueprint: `blueprint.edn`

## Constraints

- Do not invent users/revenue numbers for marketing
- No force-push; keep AGPL headers
- Secrets stay out of this repo
