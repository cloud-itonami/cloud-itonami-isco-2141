# cloud-itonami-isco-2141

Open Business Blueprint for **ISCO-08 2141**: Industrial and Production Engineers — an ISCO
**Wave 1 (design & governance)** occupation per ADR-2607121000. This
is the SECOND wave-1 blueprint batch (21xx engineering design
professions): the design/analysis work is cognitive; physical
execution remains robotics-gated and out of the actor's scope.

**Maturity: `:implemented`** — IndustrialProductionEngineersAdvisor ⊣
IndustrialProductionEngineersGovernor as a langgraph StateGraph
(`intake → advise → govern → decide → commit/hold`, human-approval
interrupt), modeled on cloud-itonami-isco-4311's bookkeeping actor.
15 tests / 31 assertions green.

The manufacturing QA HARD invariants — arithmetic, not judgement:

1. **Tolerance containment** — a proposed measurement must satisfy
   LSL ≤ measured ≤ USL against the process's registered tolerance
   band. A part is either in the band or it isn't.
2. **Stack-up limit** — a proposed cumulative tolerance stack must not
   exceed the process's registered allowable stack (arithmetic sum vs
   a registered ceiling).

Also HARD: unregistered/foreign process, unregistered organization,
non-`:propose` effect. Escalations (always human sign-off):
`:approve-deviation` (accepting an out-of-spec part or over-stack
assembly anyway), low confidence (< 0.6).

AGPL-3.0-or-later, forkable by any qualified operator. Part of the
[cloud-itonami](https://itonami.cloud) open business fleet.
