# Calculator service implements ROI and payback

Sources: [aether-ms-calculator#3](https://github.com/AetherGases/aether-ms-calculator/issues/3), parent [ms-aeko-hub#64](https://github.com/AetherGases/ms-aeko-hub/issues/64), hub `calculate_roi` / `calculate_payback` tools, grilling 2026-10-25.

Local copy. Not the GitHub issue tracker.

## Problem Statement

The Aeko hub still computes discounted ROI and payback inside the gateway tools. Those tools must keep the same JSON and the same refusals, but the arithmetic has to live on the calculator service so the hub can POST and map errors without recalculating.

This service is a Spring Boot skeleton copied from core-api: JWT on `/api/**`, no calculator routes, no investment math, and error bodies shaped as `{ message, status }`. The hub already speaks a different internal contract: header `authentication-key` equal to `INTERNAL_AUTHENTICATION_KEY`, snake_case investment JSON, and `{ error_code, message }` for business and auth refusals.

Until this service exposes `POST /calculator/roi` and `POST /calculator/payback` with that contract, the hub cannot cut over, and agents cannot get the current tool answers from this origin.

## Solution

The calculator service accepts the same investment object on two routes, authenticates with the shared internal key, validates exactly as today’s tools do, discounts a 60-month series, and returns the current ROI or payback JSON.

The hub keeps refusing empty or invalid JSON before it calls this service. This service does not implement the arithmetic `calculator` tool. JWT, Postgres, Mongo, and the rest of the skeleton stay; they are unused by these routes.

## User Stories

1. As the hub `calculate_roi` tool, I want to POST snake_case JSON to `/calculator/roi` with `authentication-key`, so that I can return the service body to the agent unchanged.
2. As the hub `calculate_payback` tool, I want to POST the same JSON to `/calculator/payback` with `authentication-key`, so that payback moves off the gateway on the same contract as ROI.
3. As an Aeko agent, I want `calculate_roi` to still answer `capex`, `wacc_monthly`, `months`, `vp_total`, `vpl`, `roi_percent`, and `viable`, so that ranking proposals does not change.
4. As an Aeko agent, I want `calculate_payback` to still answer `capex`, `wacc_monthly`, `months`, `vp_total`, `payback_months`, and `viable`, so that “how long until recovery” stays a different question from “is it viable”.
5. As the hub, I want origin `INVESTMENT_MS_BASE_URL` with no `/v1` prefix, so that the locked path is `{base}/calculator/roi` and `{base}/calculator/payback`.
6. As the hub, I want `Content-Type: application/json` and a JSON object body, so that this service never has to decode a JSON string.
7. As the hub, I want a matching `authentication-key` to authorize the call, so that only internal callers with `INTERNAL_AUTHENTICATION_KEY` can compute.
8. As the hub, I want a missing `authentication-key` to be 401 `{ "error_code": "unauthorized", "message": "Missing authentication key." }`, so that I treat it as an integration failure rather than bad agent JSON.
9. As the hub, I want a wrong `authentication-key` to be 401 `{ "error_code": "unauthorized", "message": "Invalid authentication key." }`, so that a bad secret is an integration failure.
10. As an operator, I want the routes to still read `authentication-key` when `INTERNAL_AUTHENTICATION_KEY` is unset, so that the header stays part of the contract even in a misconfigured deploy.
11. As an operator, I want a present header and an empty env to be 503 `{ "message": "Calculator authentication is not configured." }` with no `error_code`, so that a missing secret is not disguised as `unauthorized`.
12. As the hub, I want that 503 to be an integration failure, so that the agent is not told the investment JSON was invalid.
13. As the calculator service, I want a missing header to be 401 even when the env is empty, so that callers are still required to send the key.
14. As the hub, I want `validation_error` to become `ValueError` with only `message`, so that the agent sees the current English tool text.
15. As an Aeko agent, I want unknown fields refused with sorted names, so that `discount_rate` and `tax` produce `The request takes no discount_rate, tax. It accepts: capex, wacc_monthly, cash_flows, monthly_cash_flow.`
16. As an Aeko agent, I want a non-number, boolean, inf, or nan `capex` refused as `"capex" must be a number, got {value!r}.`, so that `True`, `None`, `'150'`, and `inf` match Python `repr`.
17. As an Aeko agent, I want `capex` at or below zero refused as `"capex" must be the initial investment, above zero, got {capex!r}.`, so that zero and negative investments are rejected.
18. As an Aeko agent, I want a missing `capex` treated as `None`, so that `{}` fails with `"capex" must be a number, got None.`
19. As an Aeko agent, I want a non-number `wacc_monthly` refused as `"wacc_monthly" must be a number, got {value!r}.`, so that strings, booleans, null, and inf are rejected the same way as today.
20. As an Aeko agent, I want `wacc_monthly` outside `[0, 1)` refused as `"wacc_monthly" must be the monthly discount rate as a decimal between 0 and 1 (0.01 is 1% a month), got {wacc!r}. An annual rate must be converted before it is sent.`, so that `1`, `1.5`, `12`, and `-0.01` are rejected.
21. As an Aeko agent, I want `wacc_monthly` of `0` accepted, so that undiscounted cash flows are a valid request.
22. As an Aeko agent, I want exactly one of `cash_flows` or `monthly_cash_flow`, so that neither yields `got neither` and both yield `got cash_flows and monthly_cash_flow` in the current sentence.
23. As an Aeko agent, I want a non-list or empty `cash_flows` refused as `"cash_flows" must be a list of monthly cash flows, oldest first, got {flows!r}.`, so that `[]`, a string, a number, and an object fail.
24. As an Aeko agent, I want more than 60 flows refused as `"cash_flows" must carry at most 60 monthly flows, one per month of the horizon, got {n}.`, so that the horizon cannot be exceeded.
25. As an Aeko agent, I want a bad list item refused as `"cash_flows[{i}]" must be a number, got {value!r}.`, so that the failing month is named.
26. As an Aeko agent, I want a bad `monthly_cash_flow` refused as `"monthly_cash_flow" must be a number, got {value!r}.`, so that a single repeated flow is validated like any other number.
27. As an Aeko agent, I want unknown fields checked before `capex`, so that extra keys are named even if `capex` is also missing.
28. As an Aeko agent, I want a short `cash_flows` list padded with zeros to 60 months, so that omitted months count as zero.
29. As an Aeko agent, I want `monthly_cash_flow` repeated 60 times, so that one net monthly flow covers the whole horizon.
30. As an Aeko agent, I want a negative monthly flow accepted, so that a month that costs money can undo earlier recovery.
31. As an Aeko agent, I want month 1 already discounted as `flow / (1 + wacc_monthly) ** m` for `m` in 1..60, so that there is no undiscounted first month.
32. As an Aeko agent, I want WACC 0 to leave flows undiscounted, so that present value equals the nominal series.
33. As an Aeko agent, I want `vp_total`, `vpl`, and `roi_percent` computed from the raw sum, so that rounding `vp_total` cannot change `vpl` or viability.
34. As an Aeko agent, I want `viable` to be `capex <= vp_total_raw` without rounding, so that a project that matches CAPEX exactly is viable.
35. As an Aeko agent, I want money and percent fields rounded with Python 3 `round` (half-to-even) to 2 places, so that `333.333` at WACC `0.01` still yields `vp_total` 330.03 and `vpl` -669.97.
36. As an Aeko agent, I want `capex` and `wacc_monthly` echoed as sent (150 stays 150, not 150.0), so that the hub can return the 200 body as-is and keep integer CAPEX as an integer.
37. As an Aeko agent, I want `months` always 60, so that the horizon is not the length of the list I sent.
38. As an Aeko agent, I want the worked example `capex` 150, `wacc_monthly` 0.25, `cash_flows` `[125, 156.25]` to yield `vp_total` 200, `vpl` 50, `roi_percent` 33.33, `viable` true, so that 125/1.25 and 156.25/1.25² remain the canonical check.
39. As an Aeko agent, I want that same request on payback to yield `payback_months` 2, so that recovery is the first month whose accumulated present value covers CAPEX.
40. As an Aeko agent, I want capex 250 with those flows to yield `vpl` -50, `roi_percent` -20, `viable` false, and `payback_months` -1, so that an unviable project is reported as never recovered.
41. As an Aeko agent, I want capex 100, WACC 0.25, `cash_flows: [125]` to yield `vp_total` 100, `roi_percent` 0, `viable` true, `payback_months` 1, so that exact recovery in month 1 is viable.
42. As an Aeko agent, I want capex 250, WACC 0, `[100, 200, 300]` to yield `payback_months` 2 and `vp_total` 600, so that undiscounted accumulation is obvious.
43. As an Aeko agent, I want capex 290, WACC 0, `[100, 100, 100]` to yield `payback_months` 3, so that payback uses the accumulated series, not an isolated month.
44. As an Aeko agent, I want capex 5900, WACC 0, 30×200 then `-6000` then 29×200 to yield `payback_months` 30, `vp_total` 5800, `viable` false, so that a project can pay back and later be not viable.
45. As an Aeko agent, I want unrecovered payback to be `-1`, never a month past 60, so that “not recovered in the horizon” is a stable sentinel.
46. As the hub, I want 400 `{ "error_code": "validation_error", "message": "..." }` for business refusals, so that I raise `ValueError(message)` and log the tool as failed.
47. As the hub, I want timeout, `unauthorized`, 5xx, and non-JSON bodies to stay integration errors, so that I never invent a local ROI fallback.
48. As the calculator service, I want validation to run only after a successful key match, so that a bad body plus a bad key is still `unauthorized`.
49. As the calculator service, I want JWT Bearer unused on these routes, so that the hub does not send `Authorization` and a user token cannot stand in for the internal key.
50. As the calculator service, I want existing `/api/**` JWT behaviour left in place, so that this card does not rip out the core-api skeleton.
51. As an operator, I want `INTERNAL_AUTHENTICATION_KEY` documented in the example env, so that deploys know the shared secret name.
52. As CI, I want a test profile key for calculator HTTP tests, so that slice tests can authenticate without production secrets.
53. As CI, I want calculator tests not to need Postgres, Mongo, or Redis, so that the new contract can be proven without the skeleton’s databases.
54. As CI, I want the existing context-load test unchanged, so that the skeleton still boots the way it does today.
55. As a caller hitting the service directly with malformed JSON or a missing Content-Type, I want Spring’s default 400 or 415, so that this service does not reimplement the hub’s “JSON object string” refusal.
56. As an attacker, I want a missing or wrong key compared in constant time, so that I cannot easily probe the secret by timing.
57. As an operator, I want a missing env not to fail process boot, so that CI and unrelated context loads still start.
58. As the hub, I want both routes to share one request shape, so that ROI and payback stay the same investment object.
59. As an Aeko agent, I want boolean `true`/`false` never accepted as numbers, so that JSON booleans cannot slip through as 1 or 0.
60. As an Aeko agent, I want finite numbers only, so that inf and nan cannot produce a 200.
61. As an Aeko agent, I want `roi_percent` as `(vpl_raw / capex) * 100` then rounded, so that ROI stays a percent of CAPEX, not of rounded present value.
62. As an Aeko agent, I want negative `vpl` and negative `roi_percent` when CAPEX exceeds present value, so that unviable projects are numeric, not omitted.
63. As the calculator service, I want snake_case only on these request and response payloads, so that leftover JWT DTOs are not globally renamed.
64. As the calculator service, I want no persistence of investment requests, so that ROI and payback stay pure functions of the body.
65. As the product, I want the arithmetic `calculator` tool to stay in the hub, so that this service is only investment analysis.
66. As the hub ticket 01, I want to script HTTP against this contract without a live service, so that gateway tests can proceed in parallel.

## Implementation Decisions

- Add a calculator HTTP module beside the existing shared skeleton. Do not extract a new microservice and do not delete JWT, JPA, Mongo, Redis, or OpenAPI config in this card.
- Expose only `POST /calculator/roi` and `POST /calculator/payback`. No `/v1`, no `/api` prefix on these routes.
- Keep the Spring Security JWT filter for `/api/**`. Permit `/calculator/**` in the matcher chain so those routes are not forced through Bearer auth. Enforce `authentication-key` in a dedicated filter or interceptor for those two POSTs.
- Compare the header to `INTERNAL_AUTHENTICATION_KEY` in constant time. Do not trim the header. Treat HTTP header names as case-insensitive.
- Auth order: always read `authentication-key`; if absent → 401 `unauthorized` / `Missing authentication key.`; if env empty → 503 `{ "message": "Calculator authentication is not configured." }` (no `error_code`); if present and not equal → 401 `unauthorized` / `Invalid authentication key.`; if equal → validate and compute.
- Do not fail application boot when the env is missing. 503 is a request-time operator failure.
- After a valid key, parse the body as a JSON object. Reject unknown keys first (sorted), then `capex`, then `wacc_monthly`, then cash-flow fields — same order as the current hub tools.
- Do not use Bean Validation as the source of business messages. Hand-validate so refusals match today’s `ValueError` text, including Python `repr` for the offending value (`None`, `True`, `'150'`, `inf`, …).
- Number rules: JSON boolean is not a number; non-finite is not a number; missing field is `None`. `capex` must be `> 0`. `wacc_monthly` must satisfy `0 <= x < 1`. Exactly one of `cash_flows` (non-empty list, length 1..60) or `monthly_cash_flow` (one number, possibly negative).
- Build a 60-element series: pad `cash_flows` with zeros; repeat `monthly_cash_flow` sixty times.
- Discount with IEEE 754 binary floating math, matching current Python `float` tools: `PV_m = flow_m / (1 + wacc_monthly) ** m` for `m` from 1 to 60. WACC 0 leaves the flow unchanged. Negative flows discount the same way.
- Totals from the unrounded sum: `vp_total_raw = sum(PV_m)`; `vpl_raw = vp_total_raw - capex`; `viable = capex <= vp_total_raw`; `roi_percent_raw = vpl_raw / capex * 100`.
- Payback: accumulate present values; first month `m` with accumulated `>= capex`; else `-1`. A project may have a payback month and `viable: false` if later flows undo the recovery.
- Round only `vp_total`, `vpl`, and `roi_percent`, each from the raw value with Python 3 `round` (half-to-even) to 2 decimal places. Do not round `capex` or `wacc_monthly`. `months` is always 60.
- Echo `capex` and `wacc_monthly` as received so JSON integers stay integers (`150` not `150.0`).
- Success 200 bodies:

ROI:

```json
{
  "capex": 150,
  "wacc_monthly": 0.25,
  "months": 60,
  "vp_total": 200.0,
  "vpl": 50.0,
  "roi_percent": 33.33,
  "viable": true
}
```

Payback (same request):

```json
{
  "capex": 150,
  "wacc_monthly": 0.25,
  "months": 60,
  "vp_total": 200.0,
  "payback_months": 2,
  "viable": true
}
```

- Business refusal: 400 `{ "error_code": "validation_error", "message": "<tool text>" }`. Auth refusal: 401 `{ "error_code": "unauthorized", "message": "<Missing|Invalid authentication key.>" }`. Unconfigured secret: 503 `{ "message": "Calculator authentication is not configured." }`.
- Snake_case applies only to calculator request and response payloads. Do not switch the application-wide JSON naming strategy.
- No database schema, entities, or caches for this feature. Computation is stateless.
- Document `INTERNAL_AUTHENTICATION_KEY` in the example env. Give the test profile a key so slice tests can send a matching header.
- OpenAPI may describe the header for these two routes; do not require Bearer on them.
- Malformed JSON and missing `Content-Type` stay Spring defaults (400/415). The hub will not send those cases.
- No new seams beyond the HTTP API the hub already plans to call.

## Testing Decisions

A good test posts HTTP like the hub will, then asserts status and JSON. It does not inspect private helpers, rounding internals, or filter class names. Expected numbers and messages are the current tools’ answers, not a second implementation of ROI in the test.

**Seam (one):** the calculator HTTP API — `POST /calculator/roi` and `POST /calculator/payback`. Same seam for authentication, validation, and arithmetic. Drive it with a Spring MVC slice (MockMvc or equivalent) that does not start Postgres, Mongo, or Redis.

There is no existing HTTP seam in this repository (only a context-load test). The behavioural oracle is the hub finance tool tests: same requests, same 200 fields, same `ValueError` strings. Port those cases to HTTP plus the auth cases decided in grilling.

Cover at least:

- Matching key, worked example, both routes.
- Every numeric row in the issue (unviable ROI, exact month-1 recovery, WACC 0 payback, accumulated payback, payback-then-unviable, 333.333 rounding).
- The full `validation_error` table with exact `message` text and Python `repr`.
- 401 missing key; 401 wrong key; 401 missing key while env is empty.
- 503 with header present and env empty; 503 body has `message` and no `error_code`.
- Extra fields sorted; missing `capex` as `got None.`; boolean and inf rejected; integer `capex` 150 echoed as 150.

Do not replace or extend the existing context-load test to prove this contract.

## Out of Scope

- The arithmetic `calculator` tool (AST expressions) on this service.
- Removing or replacing JWT, employee lookup, Postgres, Mongo, Redis, or mail in the skeleton.
- Hub-side cutover, tool catalog changes, or ChatGPT/Aeko naming (gateway tickets 01 and 02).
- Recalculating ROI/payback in the hub when this service is down.
- Persisting investment analyses.
- Changing the older open issue “Criar rota para realizar cálculo de ROI” unless that work is folded in separately.
- Publishing this spec to the GitHub issue tracker or applying `ready-for-agent` (label vocabulary is not set up in this repo; this file is the spec).

## Further Notes

- Gateway mapping: `validation_error` → `ValueError(message)`; `unauthorized`, timeout, 5xx, non-JSON → integration error; 200 body returned as-is.
- `{value!r}` is Python `repr`. JSON `null` → `None`; JSON `true` → `True`; JSON string `150` → `'150'`; non-finite → `inf` / `nan` style `repr`, not Java `Infinity`.
- Worked example check: `125 / 1.25 = 100`, `156.25 / 1.25² = 100` → VP 200, VPL 50, ROI `50/150*100 = 33.33`, payback month 2.
- This ticket is not blocked by the gateway and may proceed in parallel; hub tests script HTTP and do not need a live process.
- Issue tracker triage labels were not provided here. To publish this spec as a GitHub issue with `ready-for-agent`, run `/setup-matt-pocock-skills` in a repo that has that vocabulary. This copy lives under `.scratch/` by request.
