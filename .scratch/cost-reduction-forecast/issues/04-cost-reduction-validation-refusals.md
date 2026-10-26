# 04: Cost-reduction validation refusals

**What to build:** The same `POST /calculator/cost-reduction/forecast`, once authenticated with `calculator:roi`, refuses invalid JSON without coercing values. One English `validation_error` at a time. Extra properties first (sorted names + allowed list), then `monthlyCost`, `reductionPercentage`, `periodMonths`. Missing/null use `got None`, matching the gas forecast tone.

**Blocked by:** 03: Cost-reduction forecast happy path

**Status:** ready-for-agent

- [ ] Unknown fields → 400 `validation_error` listing sorted extras and `monthlyCost, reductionPercentage, periodMonths`; extras are checked before missing `monthlyCost`
- [ ] `{}`, omitted field, and JSON `null` → 400 `got None` (not treated as zero)
- [ ] String `"100000"`, boolean, or non-number in a numeric field → 400
- [ ] `monthlyCost` < 0 → 400; `100000.001` → 400; `100.100` and `100000.10` are accepted
- [ ] `reductionPercentage` outside 0..50 → 400; JSON `15.0` and `15.5` → 400; `0` and `50` remain valid
- [ ] `periodMonths` `0`, `61`, negative → 400; JSON `3.0` → 400; integers `1`, `5`, `18`, `36`, `60` are accepted (not limited to 3/6/12/24)
- [ ] Non-object body → 400; malformed JSON → 400 `The request body is not valid JSON.`
- [ ] Values are not clamped or rounded on the way in; 400 uses `{ "message", "status": "BAD_REQUEST", "error_code": "validation_error" }`
- [ ] Valid JWT + empty/invalid body is 400, not 401
- [ ] HTTP tests cover the refusal table without databases
