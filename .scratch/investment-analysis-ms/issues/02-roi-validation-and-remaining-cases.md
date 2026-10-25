# 02: ROI validation and remaining cases

**What to build:** The same `POST /calculator/roi` refuses invalid investment JSON with today’s tool `ValueError` text (`validation_error`, Python `repr`) and answers the rest of the ROI numeric cases: padding, repeated monthly flow, exact recovery, unviable project, WACC 0, and half-to-even rounding. Auth still runs first.

**Blocked by:** 01: Authenticated ROI worked example

**Status:** ready-for-agent

- [x] Extra fields are refused with sorted names: `The request takes no discount_rate, tax. It accepts: capex, wacc_monthly, cash_flows, monthly_cash_flow.`
- [x] Unknown fields are checked before `capex`; a missing `capex` is `"capex" must be a number, got None.`
- [x] Non-number / bool / inf / nan `capex` uses `"capex" must be a number, got {value!r}.`; `capex` ≤ 0 uses `"capex" must be the initial investment, above zero, got {capex!r}.`
- [x] Non-number `wacc_monthly` uses `"wacc_monthly" must be a number, got {value!r}.`; values outside `[0, 1)` use the monthly-decimal message including `{wacc!r}`; `0` is accepted
- [x] Neither flow field → `got neither`; both → `got cash_flows and monthly_cash_flow`; empty/non-list `cash_flows`, more than 60 items, bad `cash_flows[{i}]`, and bad `monthly_cash_flow` match the current tool sentences
- [x] Business refusals are 400 `{ "error_code": "validation_error", "message": "<exact text>" }`; a bad body plus a bad key is still 401 `unauthorized`
- [x] Short `cash_flows` pad with zeros to 60 months; `monthly_cash_flow` repeats 60 times; month 1 is already discounted; `months` is always 60
- [x] Capex 250 / same worked flows → `vpl` -50, `roi_percent` -20, `viable` false
- [x] Capex 100, WACC 0.25, `cash_flows: [125]` → `vp_total` 100, `roi_percent` 0, `viable` true
- [x] Capex 1000, WACC 0.01, `cash_flows: [333.333]` → `vp_total` 330.03, `vpl` -669.97 (Python 3 `round`, half-to-even, from raw totals)
- [x] `monthly_cash_flow` 5000 at WACC 0.01 and capex 150000 discounts over 60 months, not the months sent
- [x] HTTP slice tests cover the refusal table and these 200s without databases
