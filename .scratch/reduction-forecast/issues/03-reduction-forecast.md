# 03: Reduction forecast

**What to build:** `POST /calculator/reduction/forecast` takes a current annual emission, a green-gas id, and a year horizon, and returns remaining tCO2e over that horizon plus fossil tCO2e avoided. It reads two `gas` rows and does not write `reduction`. Auth: JWT + `calculator:gas`.

**Blocked by:** 01 JWT on ROI and payback

**Status:** ready-for-agent

- [ ] Request shape: `{ "years", "current_emission": { "gas", "volume_co2e", "scope" }, "green_gas" }` — `gas` / `green_gas` are `gas.id`; `volume_co2e` is tCO2e per year; `years` is an integer ≥ 1; `scope` is 1, 2, or 3.
- [ ] Response shape: `{ "future_emission": { "gas", "volume_co2e", "scope" }, "reduction_co2e" }` — `future_emission.gas` is the green id; `scope` is echoed; `future_emission.volume_co2e` already includes `years`.
- [ ] Formula: `future_emission.volume_co2e = current.volume_co2e * (gwp_green / gwp_current) * years`; `reduction_co2e = current.volume_co2e * years` (GWP from the catalog at request time).
- [ ] Worked example: fossil CO2 at 10 tCO2e/year for 5 years → green CO2(b) → future 50, reduction 50.
- [ ] Worked example: CH4 at 28 tCO2e/year for 5 years → green CO2(b) → future 5, reduction 140.
- [ ] `years` 1 applies the formula once; `volume_co2e` 0 yields future 0 and reduction 0.
- [ ] Reduction stays `current.volume_co2e * years` even if green GWP is larger than current GWP.
- [ ] 400 when: unknown JSON fields; missing/wrong types; `years` not an integer ≥ 1; `volume_co2e` missing or negative; `scope` not in {1,2,3}; unknown gas id; current gas is biogenic; green gas is not biogenic.
- [ ] 400 is not 401: a valid JWT with `calculator:gas` and a bad body is validation, not auth.
- [ ] Missing/invalid JWT → 401; JWT without `calculator:gas` (including `calculator:roi` only) → 403, before catalog lookup or the formula.
- [ ] No user, company, or inventory id in the body; identity stays in the JWT; the catalog is global.
- [ ] The call does not insert `reduction` or change an inventory.
- [ ] OpenAPI on the calculator docs type describes this operation; success schema is the forecast output DTO.
- [ ] Behaviour is proven through the `/calculator/**` HTTP seam (no separate domain-only suite required).
