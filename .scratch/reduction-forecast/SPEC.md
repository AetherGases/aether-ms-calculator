# Reduction forecast and calculator JWT

Status: ready for implementation after this spec is accepted.

## Problem Statement

The hub and the Aether apps need to forecast how much fossil CO2e is avoided when a current emission is replaced by a green gas over a number of years, and they need to list which gases in the catalog count as green. That contract does not exist on this service today.

The only calculator HTTP surface is discounted ROI and payback. Those two routes authenticate with a shared `authentication-key` and keep that scheme. The gas catalog already lives in Postgres (`gas.id`, `formula`, `is_biogenic`, `gwp`), but nothing in this service reads it for a caller.

## Solution

Expose two new calculator routes, both authenticated with the employee's JWT and authority `calculator:gas`:

- `POST /calculator/reduction/forecast` — given a current annual emission, a green-gas id, and a year horizon, return the remaining emission over that horizon and the fossil tCO2e avoided.
- `GET /calculator/gases` — return the gas catalog, marking green gases.

`POST /calculator/roi` and `POST /calculator/payback` keep `authentication-key`. Do not switch them to JWT. Do not enforce `calculator:roi` in this service. The internal key stays as identity on those two routes only.

Green means `gas.is_biogenic`. There is no new column. The forecast reads two `gas` rows and does not write `reduction`.

## User Stories

1. As an Aether employee with calculator access, I want to POST a reduction forecast so that I can see remaining emissions and avoided tCO2e after switching to a green gas.

2. As an Aether employee, I want to send `years`, `current_emission`, and `green_gas` in one JSON body so that the hub can call a single endpoint.

3. As an Aether employee, I want `current_emission.volume_co2e` to mean tCO2e per year so that the horizon is explicit in `years`.

4. As an Aether employee, I want `years` to be a whole number of years of at least 1 so that a partial year cannot be smuggled in.

5. As an Aether employee, I want `current_emission.gas` and `green_gas` to be `gas.id` integers so that the payload matches the inventory catalog.

6. As an Aether employee, I want `current_emission.scope` to be 1, 2, or 3 so that the forecast stays aligned with GHG Protocol scopes.

7. As an Aether employee, I want `future_emission.gas` to be the green-gas id I sent so that the response names the substitute, not the fossil gas.

8. As an Aether employee, I want `future_emission.scope` to echo the request scope so that the forecast does not silently reclassify the emission.

9. As an Aether employee, I want `future_emission.volume_co2e` to be the remaining tCO2e over the whole horizon (years already applied) so that I do not have to multiply again.

10. As an Aether employee, I want `reduction_co2e` to be the fossil tCO2e avoided over the horizon so that the headline saving is the current annual volume times years.

11. As an Aether employee substituting fossil CO2 (GWP 1) with biogenic CO2(b) (GWP 1) at 10 tCO2e/year for 5 years, I want `future_emission.volume_co2e` 50 and `reduction_co2e` 50 so that same-GWP biogenic replacement still counts as fossil avoided.

12. As an Aether employee substituting CH4 (GWP 28) with CO2(b) (GWP 1) at 28 tCO2e/year for 5 years, I want `future_emission.volume_co2e` 5 and `reduction_co2e` 140 so that the remaining biogenic CO2e uses the GWP ratio while the reduction stays the full fossil total.

13. As an Aether employee, I want the remaining volume to be `current.volume_co2e * (gwp_green / gwp_current) * years` so that the substitute is converted through the catalog GWP values.

14. As an Aether employee, I want `reduction_co2e` to stay `current.volume_co2e * years` even when the green GWP is larger than the current GWP so that fossil avoided is not reduced by a heavier substitute.

15. As an Aether employee, I want a 400 when `green_gas` is not biogenic so that I cannot forecast a “green” switch to a fossil gas.

16. As an Aether employee, I want a 400 when `current_emission.gas` is biogenic so that I cannot treat an already-green gas as the fossil baseline.

17. As an Aether employee, I want a 400 when either gas id does not exist so that a stale hub catalog cannot invent a forecast.

18. As an Aether employee, I want a 400 when `years` is missing, zero, negative, or not an integer so that the horizon cannot be ambiguous.

19. As an Aether employee, I want a 400 when `volume_co2e` is missing or negative so that a forecast cannot start from an impossible emission.

20. As an Aether employee, I want a 400 when `scope` is not 1, 2, or 3 so that only GHG scopes are accepted.

21. As an Aether employee, I want a 400 when the body has unknown fields so that hub drift is rejected the same way ROI rejects extra keys.

22. As an Aether employee, I want a 400 when required objects or fields are missing so that a partial body does not run the formula.

23. As an Aether employee, I want volume 0 over any valid horizon to return future 0 and reduction 0 so that an empty baseline is a valid forecast.

24. As an Aether employee, I want `years` 1 to apply the formula once so that a one-year horizon does not get special-cased.

25. As an Aether employee, I want the forecast to be stateless so that calling it does not insert a `reduction` row or change an inventory.

26. As an Aether employee, I want the forecast to ignore my enterprise and inventory so that the catalog is global and the body is the only input.

27. As an Aether employee with authority `calculator:gas`, I want `POST /calculator/reduction/forecast` to succeed so that forecast access is that permission.

28. As an Aether employee with authority `calculator:gas`, I want `GET /calculator/gases` to succeed so that listing and forecasting share the same permission.

29. As an Aether employee whose JWT lacks `calculator:gas`, I want forecast and gas listing to return 403 so that another calculator permission (or none) does not open the gas routes.

30. As a caller with a JWT and `calculator:gas` but no `authentication-key`, I want ROI and payback to return 401 so that gas access does not open investment routes.

31. As an unauthenticated caller, I want forecast and gas listing without a JWT to return 401 so that the shared `authentication-key` is not identity on those routes.

32. As a caller with a malformed or expired JWT on forecast or gas listing, I want 401 so that a bad token is not confused with a missing permission.

33. As an authenticated employee missing `calculator:gas` on forecast or gas listing, I want 403 with the shared error envelope so that the hub can tell “not logged in” from “not allowed”.

34. As an Aether employee, I want error bodies to be `message`, `status`, and `error_code` so that forecast errors match the rest of this service.

35. As an Aether employee, I want validation failures on the forecast to be 400, not 401, so that a bad body is not reported as an auth failure.

36. As an Aether employee, I want `GET /calculator/gases` to list every `gas` row so that the hub can build both the current-gas and green-gas pickers from one call.

37. As an Aether employee, I want each listed gas to include `id`, `name`, `formula`, `is_green`, and `gwp` so that the UI can label, convert, and filter without a second lookup.

38. As an Aether employee, I want `is_green` to be `gas.is_biogenic` so that “green” matches the dashboard’s biogenic split without a migration.

39. As an Aether employee, I want today’s catalog to show only `CO2(b)` as green so that the listing does not invent hydrogen or biomethane that are not in `gas`.

40. As an Aether employee, I want an empty catalog to return `[]` so that a missing seed is visible rather than a 500.

41. As a hub client, I want `POST /calculator/roi` to keep returning the same investment JSON as today, still authenticated with `authentication-key`, so that investment clients do not change.

42. As a hub client, I want `POST /calculator/payback` to keep returning the same payback JSON as today, still authenticated with `authentication-key`, so that payback is not redesigned.

43. As a hub that sends `authentication-key` to ROI and payback, I want that header to remain proof of identity on those two routes so that this delivery does not break existing investment callers.

44. As a caller, I want a JWT without `authentication-key` to be insufficient on ROI and payback so that the internal key is still required there.

45. As a service operator, I want unconfigured `INTERNAL_AUTHENTICATION_KEY` to keep producing 503 on ROI and payback so that those routes still fail closed.

46. As an Aether employee, I want forecast authorization to run before gas lookup and the formula so that a forbidden caller does not touch the catalog.

47. As a service operator, I want forecast and gas listing not to depend on `INTERNAL_AUTHENTICATION_KEY`, so that an unconfigured key does not 503 the new routes.

48. As a service operator, I want OpenAPI on the calculator docs type to describe the new forecast and gases operations so that the hub can see the contract next to ROI and payback.

49. As a service operator, I want request and response field names to stay snake_case (`volume_co2e`, `green_gas`, `reduction_co2e`, `is_green`) so that the hub contract is not Java-ified.

50. As a catalog maintainer, I want adding a new biogenic row in `gas` to make it appear as green in the listing and to become a valid `green_gas` without a code change so that the catalog is the source of truth.

51. As a catalog maintainer, I want GWP values to be read from `gas.gwp` at request time so that a GWP update changes the next forecast.

52. As an Aether employee, I want unknown JSON types (boolean gas id, string years, object volume) to be 400 so that the formula never sees coerced garbage.

53. As an Aether employee, I want the forecast not to accept a user, company, or inventory id in the body so that identity stays in the JWT.

54. As an Aether employee calling GET gases, I want no request body so that a listing cannot be filtered into a second undocumented contract.

55. As a developer of this service, I want the feature to live in the calculator module (HTTP adapters, use-case, DTOs, mapper, domain formula) so that `shared/` does not grow new calculator endpoints.

56. As a developer of this service, I want API exceptions to stay in the shared exception family so that the global handler keeps producing the same envelope.

## Implementation Decisions

- The feature stays in the calculator module: HTTP adapters, use-case service, input/output DTOs, mapper, and a domain type that computes the forecast from two catalog gases plus `years` and `volume_co2e`. `shared/` does not gain controllers.
- Forecast and gas listing get their own input/output DTOs. Controllers do not bind `JsonNode` / `ObjectNode` as the HTTP body type. Snake_case field names are part of the hub contract and are not renamed for Java style.
- OpenAPI annotations live on nested interfaces of the existing calculator docs type (same pattern as ROI and payback). Success schemas are the feature output DTOs.
- `current_emission.gas` and `green_gas` are integers (`gas.id`). The use-case loads both rows from the `gas` table. Missing id → 400. The service does not write `reduction`, `emission`, or inventory.
- Green is `gas.is_biogenic`. The listing field is `is_green` as an alias. No migration and no new catalog column.
- Scope in the forecast body is the integer 1, 2, or 3 (matching seeded `scope.id`). It is echoed on `future_emission`. The forecast does not need a `scope` table lookup beyond that membership check.
- Forecast formula, with `gwp` taken from the loaded rows:
  - `future_emission.volume_co2e = current.volume_co2e * (gwp_green / gwp_current) * years`
  - `reduction_co2e = current.volume_co2e * years`
  - `future_emission.gas = green_gas`
  - `future_emission.scope = current_emission.scope`
  - `gas.gwp` is constrained `> 0` in the schema, so the ratio is defined.
- Forecast validation: `years` integer ≥ 1 with no maximum; `volume_co2e` ≥ 0; `scope` ∈ {1,2,3}; unknown JSON fields → 400 (same extra-field policy as ROI); current gas must be non-biogenic; green gas must be biogenic; missing/wrong types → 400.
- Two auth schemes stay on `/calculator/**`:
  - `POST /calculator/roi` and `POST /calculator/payback` keep `authentication-key`. `AuthenticationKeyFilter` still guards only those two paths. Unconfigured `INTERNAL_AUTHENTICATION_KEY` remains 503 there. A JWT is not identity on those routes.
  - `POST /calculator/reduction/forecast` and `GET /calculator/gases` use `Authorization: Bearer <jwt>` and authority `calculator:gas` (permission **name** on `UserDetails`). The JWT filter must stop skipping every `/calculator/**` path; it still skips roi and payback.
- `calculator:roi` is not enforced in this delivery. Seeding or checking that permission is out of scope.
- 401 on forecast/gases: missing, blank, malformed, or invalid JWT. 403: valid JWT without `calculator:gas`. 400: body/catalog validation. Envelope remains `ExceptionOutputDTO` (`message`, `status`, `error_code`). Authorization is decided before forecast validation. ROI/payback keep their existing key 401/503 behaviour.
- No schema change in this spec. `calculator:gas` is a permission **name** the shared database must already expose (or tests must insert). This service does not add a table for it. Replacing or deleting the existing `calculator` permission row is not part of this work.
- Identity in the JWT is an auth gate on forecast and listing only. The forecast does not scope the catalog by employee, enterprise, or inventory.

## Testing Decisions

A good test observes HTTP behaviour only: status, JSON body, and auth headers. It does not assert class names, SQL, or mapper internals. The formula is proven by posting a forecast against a known catalog and reading `future_emission` and `reduction_co2e`.

**Seam (one):** the HTTP contract of `/calculator/**`. That is the same seam already used by the ROI/payback controller tests and the calculator auth-filter tests (MockMvc against the calculator routes, shared error envelope). Extend that seam so the test caller can present a JWT with authorities and a gas catalog on the new routes; do not add a second public surface to test through.

Cover at least:

- Forecast worked examples: CO2 → CO2(b) (10 t/year, 5 years → future 50, reduction 50) and CH4 → CO2(b) (28 t/year, 5 years → future 5, reduction 140).
- Auth matrix on forecast and listing: 401 without JWT; 403 with a valid JWT that lacks `calculator:gas`; 200 with `calculator:gas`. `authentication-key` is not enough on those routes.
- ROI/payback still accept a valid `authentication-key` (200, same JSON). JWT without that key is 401. Unconfigured key is still 503.
- Forecast 400s: extra fields, bad years, negative volume, bad scope, unknown gas id, biogenic current, non-biogenic green.
- GET gases shape: `id`, `name`, `formula`, `is_green`, `gwp`; only biogenic rows have `is_green` true.

Prior art: calculator HTTP tests via the standalone MockMvc support; existing internal-key filter tests stay for roi/payback. Add JWT/authority tests for forecast and listing; do not replace the key tests. Domain investment tests stay as they are; this spec does not add a parallel domain test suite for the forecast formula.

## Out of Scope

- Switching ROI/payback to JWT, removing `authentication-key`, or enforcing `calculator:roi`.
- Inserting `calculator:gas` into the shared toolkit seed (dependency, not this service’s schema).
- Deleting or renaming the existing `calculator` permission.
- Persisting forecasts into `reduction` or any inventory.
- New gases that are not rows in `gas` (hydrogen, biomethane, syngas).
- A remaining-factor table, gradual substitution curves, or extra request fields (mass, energy, substitution %).
- Changing ROI/payback request or response JSON.
- JWT on `/api/**` beyond what already exists.
- Binding a company, unit, or inventory from the forecast body.
- A `is_green` column or other migration on `gas`.

## Further Notes

This is not a breaking auth change for ROI or payback. Those clients keep sending `authentication-key`. Forecast and gas listing are new JWT routes.

Worked numbers assume the seeded catalog (CO2 id 1 GWP 1, CH4 id 2 GWP 28, CO2(b) id 4 GWP 1, biogenic). Tests may use the same ids or an equivalent fixture.

`gas.gwp` is numeric and greater than zero in the schema. Division uses those catalog values at request time.

The grilling session treated later answers as winning except where they contradicted an earlier lock: Q15 (`authentication-key` on roi/payback) stands. JWT applies only to the new forecast and gases routes.
