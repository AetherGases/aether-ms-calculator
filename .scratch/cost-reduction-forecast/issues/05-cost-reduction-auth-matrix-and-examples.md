# 05: Cost-reduction auth matrix and remaining examples

**What to build:** The cost-reduction route’s remaining documented 200s (zeros, 0%, 50%/24 months, period 1 and 60) plus the full auth matrix: 403 when the JWT lacks `calculator:roi`, 401 when there is no usable token, 415 when the type is not JSON. Gases/forecast/ROI/payback behaviour from tickets 01–02 stays intact.

**Blocked by:** 01: Calculator JSON 415 envelope; 03: Cost-reduction forecast happy path

**Status:** ready-for-agent

- [ ] `0 / 15 / 3` → monthly savings 0, new cost 0, total 0, three projection zeros
- [ ] `100000 / 0 / 6` → monthly savings 0, new cost 100000, total 0
- [ ] `100000 / 50 / 24` → 50000 / 50000 / 1200000, 24 projection points, last accumulated = total; no scientific notation
- [ ] `periodMonths` 1 → one projection item, `month` 1, accumulated = monthly savings
- [ ] `periodMonths` 60 → 60 items, months 1..60, last accumulated = totalSavings
- [ ] JWT with only `calculator:gas` or only `calculator` → 403 `{ "error_code": "forbidden", "message": "Missing calculator:roi permission." }`
- [ ] JWT with both `calculator:roi` and `calculator:gas` still 200
- [ ] Missing JWT, invalid/expired JWT, and `authentication-key` only → 401 `unauthorized`
- [ ] Wrong Content-Type on this route → 415 `unsupported_media_type` (ticket 01 envelope)
- [ ] `POST /calculator/roi` and `/payback` still `authentication-key`; gases/forecast still `calculator:gas`
- [ ] HTTP tests cover this matrix without databases
