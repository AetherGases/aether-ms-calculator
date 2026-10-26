# 03: Cost-reduction forecast happy path

**What to build:** `POST /calculator/cost-reduction/forecast` accepts the session JWT with `calculator:roi` and returns a stateless camelCase simulation for the canonical web example. No employee/enterprise in the body, nothing written to the database. This is not the gas route `POST /calculator/reduction/forecast`.

**Blocked by:** 02: Per-route calculator JWT authorities

**Status:** ready-for-agent

- [ ] With Bearer JWT that has `calculator:roi` and body `{ "monthlyCost": 100000, "reductionPercentage": 15, "periodMonths": 12 }`, HTTP 200:

```json
{
  "currency": "BRL",
  "monthlyCost": 100000,
  "reductionPercentage": 15,
  "periodMonths": 12,
  "monthlySavings": 15000,
  "newMonthlyCost": 85000,
  "totalSavings": 180000,
  "projection": [
    { "month": 1, "accumulatedSavings": 15000 },
    { "month": 2, "accumulatedSavings": 30000 }
  ]
}
```

  (`projection` has 12 items, months 1..12 in order; last `accumulatedSavings` is 180000. Snippet above is the contract shape, not the full array.)

- [ ] `{ "monthlyCost": 123.45, "reductionPercentage": 15, "periodMonths": 3 }` returns `monthlySavings` 18.52, `newMonthlyCost` 104.93, `totalSavings` 55.56 (HALF_UP on monthly savings first)
- [ ] Raw response echoes `"monthlyCost":100000` (not `100000.0`) for the integer example
- [ ] No JWT / invalid JWT → 401 `unauthorized`; this path is not satisfied by `authentication-key` alone
- [ ] JWT with only `calculator:gas` does **not** get 200 on this path (403 or filter deny — full matrix is ticket 05; must not succeed)
- [ ] No persistence; OpenAPI lists the route; PROJECT_STRUCTURE HTTP surface includes it
- [ ] HTTP tests through the existing calculator MockMvc support, without databases
