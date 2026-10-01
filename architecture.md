# Architecture — TangramNoAds

**Status:** Placeholder — authored in **Phase P2**, after requirements are locked.
**Approved by:** <name>  ·  **Approved on:** YYYY-MM-DD

> **Human-authored and human-approved.** Together with the locked REQs, this
> document is **the Contract** — the pipeline never bends it silently; a
> conflict is a hard-stop. Do not fill this in until requirements are locked.
> Authoring guide: `C:\GitHub\AI\SWDev\framework\guardrails\guardrail-authoring.md`.
> Standing directives (inherited): `C:\GitHub\AI\SWDev\framework\guardrails\directives.md`.

---

## 1. Standing directives — acknowledgment

Directives D1–D6 apply in full. Deviations (each needs explicit sign-off):

| Directive | Deviation | Reason | Approved by / on |
|---|---|---|---|
| none | — | — | — |

## 2. Project guardrails

> Few, concrete, checkable — a reviewer must be able to say "violates G-NN".

- **G-01** — <constraint>

## 3. Component map

> Main components/slices, sized to the current locked REQs.

| Component / slice | Owns | Serves (REQ areas) |
|---|---|---|
| <name> | <responsibility> | <req_<area>.md> |

### 3b. Component ownership rules

> Which logic belongs to which component — checkable boundary prose; the
> compensating control for notify-tier interfaces (gates-and-autonomy.md,
> hard-stop 5).

- **O-01** — <rule>

## 4. Governed Interface Registry  *(normative)*

> The main `I*` contracts. Every entry gets a **Tier** at G2
> (gates-and-autonomy.md, hard-stop 5): **locked** = changed only with the
> owner's pre-approval; **notify** = the pipeline may change it as feature
> work — logged (`contract-delta`), summarized in the WO's Contract-deltas
> section, presented at close/G4. Work orders declare which entries they
> touch. Everything not listed is AI-owned (`i*`) and freely refactorable.
> Keep small (~a dozen max). Mirror the tiers into `.swdev/guard.json`
> (`contract_paths.locked` / `.notify`).

| Interface | Tier | Purpose (one line) | Between | Serves REQs | Defined in | Sign-off |
|---|---|---|---|---|---|---|
| — | — | — | — | — | — | — |

*(Define entries as documented code contracts via
`C:\GitHub\AI\SWDev\framework\methodology\interface-definition.md`; record the
project's governed-marker convention here — e.g. a `Contracts` namespace for C#.)*

## 5. Technology & data constraints

- <only what matters; security & data rules are pipeline hard-stops>

## 6. Architecture Decision Records

| ADR | Title | Status | Links |
|---|---|---|---|
| — | — | — | — |

## 7. Change log

| Version | Date | Change | Reason | Approved by |
|---|---|---|---|---|
| 0.1 | YYYY-MM-DD | placeholder | — | — |
