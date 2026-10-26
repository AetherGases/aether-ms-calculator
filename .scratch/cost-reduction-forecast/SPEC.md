# Cost-reduction forecast

Status: ready for implementation after this spec is accepted.

Sources: grilling 2026-10-25 in this repo; web cost calculator (`aether-web-flow` calculator page); sibling HTTP surface `POST /calculator/reduction/forecast`.

Local copy under `.scratch/`. Not the GitHub issue tracker.

## Problem Statement

The Aether web already has a cost-reduction calculator. The employee types a monthly cost, an estimated reduction percentage, and a period, and the page computes monthly savings, the new monthly cost, total savings, and a bar chart of accumulated savings.

Those calculations run in the browser today. The BFF and the calculator microservice do not expose this simulation, so the front owns both the form and the math. The team wants the math and validation on the microservice, with the web sending values and rendering whatever comes back.

The percentage is an estimate the person chose, not a guaranteed saving. This first version must not store the simulation.

## Solution

Expose a stateless `POST /calculator/cost-reduction/forecast`. The BFF forwards the session JWT. The microservice authenticates, checks `calculator:roi`, validates the body without coercing values, rounds monthly savings with HALF_UP to two decimals, derives the rest, and returns the figures plus a month-by-month projection for the chart.

The web keeps the current fields and period buttons. It stops computing locally once the BFF is wired. Identity must grant `calculator:roi`; this service only checks that authority string.

## User Stories

1. As an employee with calculator access, I want to POST a monthly cost, a reduction percentage, and a period, so that I receive a cost-reduction simulation without the browser doing the math.

2. As an employee, I want the simulation to stay in memory for that request only, so that trying a scenario does not write history to the database.

3. As an employee, I want the request to omit employee and enterprise ids, so that identity stays in the session token and cannot be spoofed in the JSON body.

4. As an employee, I want `monthlyCost`, `reductionPercentage`, and `periodMonths` all required, so that a partial body is rejected instead of filled in by the service.

5. As an employee, I want `monthlyCost` of `100000` with `15` percent over `12` months to return monthly savings `15000`, new monthly cost `85000`, and total savings `180000`, so that the documented happy path matches production.

6. As an employee, I want a monthly cost of `0` with any allowed percentage and period to return zeros for savings, new cost, and projection, so that an empty budget still simulates.

7. As an employee, I want a reduction of `0` to keep the original monthly cost and yield zero savings, so that “no reduction” is a valid scenario.

8. As an employee, I want a reduction of `50` over `24` months on `100000` to return monthly savings `50000`, new cost `50000`, and total `1200000`, so that the maximum percentage in the UI still works.

9. As an employee, I want `123.45` at `15` percent over `3` months to return monthly savings `18.52`, new monthly cost `104.93`, and total `55.56`, so that HALF_UP to two decimals is applied before the total and the chart.

10. As an employee, I want the last projection point’s accumulated savings to equal `totalSavings`, so that the chart and the headline number cannot disagree.

11. As an employee, I want one projection item per month, ordered, starting at month `1`, so that the chart is simulation months rather than calendar dates.

12. As an employee, I want `periodMonths: 1` to return a single projection point equal to monthly savings, so that the shortest allowed horizon still graphs.

13. As an employee, I want `periodMonths: 60` to return sixty projection points, so that the upper bound of the investment horizon is usable here too.

14. As a BFF, I want `periodMonths` values other than the web buttons (`5`, `18`, `36`) accepted when they sit in `1..60`, so that the API is not coupled to today’s `3 / 6 / 12 / 24` chips.

15. As a BFF, I want `periodMonths: 0` and `periodMonths: 61` to return 400, so that an empty or oversized chart cannot be requested.

16. As a BFF, I want a negative `periodMonths` to return 400 without being clamped to 1, so that bad input is not silently repaired.

17. As an employee, I want `reductionPercentage` from `0` to `50` inclusive, so that the slider range on the web is the contract.

18. As a BFF, I want `reductionPercentage: 51` to return 400, so that the service does not invent a reduction above the product cap.

19. As a BFF, I want JSON `15.0` and `15.5` for `reductionPercentage` to return 400, so that only an integral JSON number is accepted, matching `years` on the gas forecast.

20. As a BFF, I want JSON `3.0` for `periodMonths` to return 400, so that integral-token rules are consistent across integer fields.

21. As a BFF, I want `monthlyCost` with more than two fractional digits (`100000.001`) to return 400, so that money is not silently rounded on the way in.

22. As a BFF, I want `100.100` and `100000.10` accepted as `monthlyCost`, so that trailing zeros do not fail a two-decimal rule.

23. As a BFF, I want a negative `monthlyCost` to return 400 without being forced to zero, so that the web’s local clamp is not copied into the API.

24. As a BFF, I want a JSON string `"100000"` in any numeric field to return 400, so that types are not coerced.

25. As a BFF, I want a JSON boolean in a numeric field to return 400, so that `true` cannot become `1`.

26. As a BFF, I want `null` or a missing field to return 400 with a `got None` message, so that optional-looking JSON is not treated as zero.

27. As a BFF, I want unknown JSON properties to return 400 listing the extra names and the allowed fields, so that a future accidental `employeeId` cannot slip through.

28. As a BFF, I want extra fields rejected before the three known fields are parsed, so that the fail-fast order matches the gas forecast validator.

29. As a BFF, I want the first remaining error after extras to follow `monthlyCost`, then `reductionPercentage`, then `periodMonths`, so that `{}` is deterministic.

30. As a BFF, I want a non-object body (array, primitive) to return 400, so that only a JSON object is a request.

31. As a BFF, I want malformed JSON to return 400 with the existing `The request body is not valid JSON.` envelope, so that parse failures stay uniform.

32. As a BFF, I want a `Content-Type` other than JSON to return 415 with the shared exception envelope, so that I can key off HTTP 415.

33. As a BFF, I want `application/json` with an optional charset to still succeed, so that ordinary clients are not rejected.

34. As a BFF, I want missing, blank, malformed, or expired JWT to return 401, so that I never treat an unauthenticated call as a zeroed simulation.

35. As a BFF, I want an `authentication-key` without a JWT to return 401 on this path, so that the internal ROI key cannot open the cost calculator.

36. As a BFF, I want a valid JWT that lacks `calculator:roi` to return 403, so that permission failures are distinct from auth failures.

37. As an employee who only has `calculator:gas`, I want this path to return 403, so that gas-forecast access is not cost-calculator access.

38. As an employee whose group still has the seed permission named `calculator`, I want this path to return 403 until identity grants `calculator:roi`, so that the authority string is exact.

39. As an employee with `calculator:roi` and a bad body, I want 400 rather than 401, so that validation runs only after the token is accepted.

40. As an employee with both `calculator:roi` and `calculator:gas`, I want this path to succeed, so that holding extra authorities does not lock me out.

41. As the ROI caller, I want `POST /calculator/roi` and `/payback` to keep using `authentication-key`, so that this feature does not migrate those routes to JWT.

42. As the gas-forecast caller, I want `GET /calculator/gases` and `POST /calculator/reduction/forecast` to keep requiring `calculator:gas`, so that the cost path does not weaken or broaden that catch-all.

43. As a BFF, I want error bodies to stay `{ message, status, error_code }` in English, so that I can branch on the HTTP status and ignore the text.

44. As a BFF, I want 400 to carry `error_code` `validation_error` and `status` `BAD_REQUEST`, so that this endpoint matches the rest of the calculator.

45. As a BFF, I want 401 to carry `unauthorized` and 403 to carry `forbidden` with message `Missing calculator:roi permission.`, so that the JWT filter pattern stays consistent with gas.

46. As a BFF, I want 415 to carry `unsupported_media_type` and `status` `UNSUPPORTED_MEDIA_TYPE`, so that media-type failures use the same envelope.

47. As a BFF, I want this slice not to add a global catch-all 500 handler, so that unexpected bugs keep Spring’s current 500 and scope stays on the new route.

48. As an employee, I want the JSON field names in camelCase (`monthlyCost`, `newMonthlyCost`, `accumulatedSavings`), so that the contract matches the web payload rather than the snake_case gas forecast.

49. As an employee, I want the three input fields echoed with the original JSON tokens (`100000` not `100000.0`), so that the ROI echo rule for `capex` also holds here.

50. As an employee, I want computed money fields at two decimal scale after monthly savings is rounded, without scientific notation, so that the front can format BRL from plain numbers.

51. As an employee, I want `currency` always `"BRL"` and never null, so that the front formats by locale while the money stays reais.

52. As an employee, I want every response field present and non-null, so that the chart code does not guard against missing keys.

53. As an employee, I want monthly savings computed as monthly cost × percentage / 100, then rounded HALF_UP to two decimals, so that `123.45` at 15% becomes `18.52` before anything else.

54. As an employee, I want new monthly cost to be monthly cost minus that rounded monthly savings, so that the two headline figures add back to the original cost.

55. As an employee, I want total savings to be rounded monthly savings × period months, so that the total is a multiple of the monthly figure.

56. As an employee, I want each projection accumulated value to be rounded monthly savings × month number, so that the bars are a linear ramp.

57. As an employee, I want cost and percentage to stay constant across the period, so that this version has no inflation, interest, or step changes.

58. As a frontend, I want to keep sending the current form fields and period options, so that wiring the BFF does not redesign the screen.

59. As a frontend, I want to format numbers for pt-BR myself, so that the API never returns `R$` or thousand separators.

60. As a frontend, I want a timeout or downstream 5xx to surface as an error, so that I never display `0` as if it were a real simulation.

61. As a QA engineer, I want HTTP tests for the five documented numeric examples plus invalid tokens, extra fields, integral-float rejects, bounds, 401, 403, and 415, so that the contract is executable.

62. As a QA engineer, I want the projection array length to equal `periodMonths` and months to run `1..N` with no gaps, so that the chart sequence is asserted, not inspected by eye.

63. As an implementer, I want domain validation to throw `BadRequestException` with literal English messages, so that calculator.domain never owns HTTP types beyond `shared.exceptions`.

64. As an implementer, I want OpenAPI on the shared calculator docs type, so that Swagger lists this route next to ROI, payback, gases, and gas forecast.

65. As an identity operator, I want this microservice not to insert `calculator:roi` into `schema.sql`, so that the permission catalog stays owned by identity/toolkit.

66. As a caller of other calculator routes, I want a wrong Content-Type on those POSTs to start returning the shared 415 envelope as well, so that one handler covers every calculator JSON POST.

## Implementation Decisions

- Add one new calculator HTTP use case beside ROI, payback, gases, and gas reduction forecast. Same feature module split: thin controller, service orchestration, domain parse/validate/calculate, input and output DTOs, mapper-only JSON bridging, OpenAPI nested on the existing calculator docs type.

- Path and method: `POST /calculator/cost-reduction/forecast`. This is not `POST /calculator/reduction/forecast` (that remains the gas CO2e forecast).

- JSON names are camelCase on purpose, unlike the snake_case hub contracts on ROI/payback/gas forecast. Request and response shapes:

```json
{
  "monthlyCost": 100000,
  "reductionPercentage": 15,
  "periodMonths": 3
}
```

```json
{
  "currency": "BRL",
  "monthlyCost": 100000,
  "reductionPercentage": 15,
  "periodMonths": 3,
  "monthlySavings": 15000,
  "newMonthlyCost": 85000,
  "totalSavings": 45000,
  "projection": [
    { "month": 1, "accumulatedSavings": 15000 },
    { "month": 2, "accumulatedSavings": 30000 },
    { "month": 3, "accumulatedSavings": 45000 }
  ]
}
```

- Input DTO fields that must be echoed (`monthlyCost`, `reductionPercentage`, `periodMonths`) stay as JSON nodes plus `@JsonAnySetter` unknown-field capture, the same trick ROI uses for `capex` so `100000` does not come back as `100000.0`. Domain validation reads the request node; it does not trust Jackson coercion of numbers.

- Validation is fail-fast and literal-English, `error_code` `validation_error`:
  1. Body must be a JSON object.
  2. Reject unknown properties (sorted names, message lists allowed fields), including extras on the root object only (the body is flat).
  3. `monthlyCost`: JSON number, not boolean, finite, ≥ 0; after stripping trailing zeros, scale ≤ 2.
  4. `reductionPercentage`: JSON integral number, 0..50 inclusive, fits in an int.
  5. `periodMonths`: JSON integral number, 1..60 inclusive, fits in an int.
  6. Missing and JSON `null` use `got None`, matching the gas forecast phrasing.

- Arithmetic uses `BigDecimal`. Round **only** monthly savings first: `monthlyCost * reductionPercentage / 100` with `RoundingMode.HALF_UP` scale 2. Then `newMonthlyCost = monthlyCost - monthlySavings`, `totalSavings = monthlySavings * periodMonths`, `accumulatedSavings = monthlySavings * month`. Do not reuse investment `HALF_EVEN`. Do not apply inflation or interest. Computed money is scale 2; JSON may omit trailing zeros. No scientific notation.

- `currency` is the constant `BRL`. No request field selects a currency.

- No persistence and no catalog read. Employee/enterprise are not taken from the body. JWT identity is used only for authentication and the `calculator:roi` check.

- Auth for this path: `JwtTokenFilter` runs (not skipped). Required authority is `calculator:roi`, not `calculator:gas` and not the seed name `calculator`. Missing/invalid JWT → 401 `unauthorized` (`Missing or invalid JWT.` / existing JWT invalid-or-expired message). Valid JWT without `calculator:roi` → 403 `forbidden` (`Missing calculator:roi permission.`). Internal `authentication-key` does not satisfy this path.

- Path tables in shared security must stop treating “any `/calculator/**` except roi/payback” as `calculator:gas`. Gas listing and gas forecast stay on `calculator:gas`. This new path is JWT + `calculator:roi`. ROI and payback stay `authentication-key` only. `SecurityConfig` keeps `/calculator/**` as `permitAll` because the filters still authenticate.

- 415: handle unsupported media type in the existing global exception handler with the shared exception DTO (`status` `UNSUPPORTED_MEDIA_TYPE`, `error_code` `unsupported_media_type`). This applies to other calculator JSON POSTs as well. Do not add a feature `@RestControllerAdvice`. Do not add a catch-all `Exception` → 500 mapper in this slice.

- No schema or permission-catalog changes in this service. Granting `calculator:roi` is identity/toolkit work; until that exists, callers with only `calculator` get 403 by design.

- Tests live next to the production packages they cover. Domain tests have no Spring context. HTTP and security tests go through the existing calculator MockMvc support (JWT + key filters + global handler), same prior art as the gas forecast controller tests.

- `PROJECT_STRUCTURE.md` HTTP surface, identity, and calculator-path rules must be updated in the same change as the code so they stay the source of truth.

## Testing Decisions

A good test asserts HTTP status, JSON field values, projection length/order, and `error_code` — not class names, SQL, or private helpers. Prefer the highest seam: one POST through MockMvc with a real JWT fixture, the way gas forecast and gases listing are already tested.

Primary seam (preferred, covers almost everything):

- HTTP `POST /calculator/cost-reduction/forecast` via the existing calculator MockMvc support (JWT filter + authentication-key filter + global exception handler).

Existing supporting seams (do not invent new ones):

- Shared calculator path helpers and JWT filter tests, so the new path requires `calculator:roi` and is not swallowed by the gas catch-all.
- Domain validator/calculator tests without Spring, same style as investment and gas-forecast domain tests, for HALF_UP and the numeric table.

Prior art: gas-forecast controller tests (JWT 401/403/400 matrix, extra fields, integral `1.0` reject), investment controller tests (echo of original numeric tokens, extra fields, validation_error), calculator path tests (which routes are key vs gas).

Must-cover cases:

- Table: `100000/15/12`, `0/15/3`, `100000/0/6`, `100000/50/24`, `123.45/15/3`.
- Projection: length = `periodMonths`, months `1..N` in order, last accumulated = `totalSavings`.
- Bounds: `periodMonths` 1 and 60 succeed; 0 and 61 fail; `reductionPercentage` 0 and 50 succeed; 51 fails.
- Tokens: `15.0` / `3.0` fail; string numbers fail; `100000.001` fails; `100.100` succeeds.
- Extra property, `{}`, malformed JSON → 400 `validation_error`.
- No JWT, invalid JWT, key-only → 401 `unauthorized`.
- JWT with `calculator` or `calculator:gas` only → 403 `forbidden`.
- JWT with `calculator:roi` and empty body → 400, not 401.
- Wrong Content-Type → 415.
- Input echo: `"monthlyCost":100000` in the raw body, not `100000.0`.

## Out of Scope

- Saving simulations, history, or audit rows.
- Binding employee or enterprise from the JSON body.
- Migrating `POST /calculator/roi` and `/payback` from `authentication-key` to JWT.
- Reusing `calculator:gas` or the seed permission `calculator` as sufficient authority for this path.
- Restricting `periodMonths` to the web chips `3, 6, 12, 24`.
- Inflation, interest, variable monthly cost, or non-BRL currencies.
- Portuguese error messages or a second error envelope without `error_code`.
- A global 500 envelope for unexpected exceptions.
- Inserting `calculator:roi` into this service’s Postgres schema or toolkit seed (identity owns that catalog).
- Changing the web UI or the BFF in this repository (this spec is the calculator microservice only).
- Calendar dates on the projection.

## Further Notes

- The web page still computes with JS floats and formats with zero fraction digits. After integration, the source of truth is this API’s HALF_UP figures; the front only formats.
- Toolkit seed today inserts permission name `calculator`. This endpoint will 403 those users until identity adds and grants `calculator:roi`. That is expected, not a bug in this service.
- `periodMonths` max 60 matches the investment horizon as a safety cap, not as a shared constant requirement. Do not overload investment cash-flow length rules onto this calculator beyond the numeric cap.
- Issue tracker publish was skipped: the user asked for `.scratch/`. If the GitHub triage labels are needed, run `/setup-matt-pocock-skills` and re-publish.
