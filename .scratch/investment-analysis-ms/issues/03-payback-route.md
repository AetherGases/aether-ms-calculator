# 03: Payback route

**What to build:** `POST /calculator/payback` uses the same key, the same investment JSON, and the same present-value rules as ROI, and returns `payback_months` (first accumulated month covering capex, or `-1`) plus `vp_total` and `viable`. A project can pay back and later be not viable. This service still does not implement the arithmetic `calculator` tool.

**Blocked by:** 02: ROI validation and remaining cases

**Status:** ready-for-agent

- [ ] Matching key and the worked example return 200 `{ "capex": 150, "wacc_monthly": 0.25, "months": 60, "vp_total": 200.0, "payback_months": 2, "viable": true }`
- [ ] Same auth contract as ROI: 401 missing/wrong key, 503 when the env is empty and the header is present
- [ ] Capex 250 / worked flows → `payback_months` -1 and `viable` false
- [ ] Capex 100, WACC 0.25, `cash_flows: [125]` → `payback_months` 1
- [ ] Capex 250, WACC 0, `[100, 200, 300]` → `payback_months` 2, `vp_total` 600
- [ ] Capex 290, WACC 0, `[100, 100, 100]` → `payback_months` 3 (accumulated, not the isolated month)
- [ ] Capex 5900, WACC 0, 30×200 + `[-6000]` + 29×200 → `payback_months` 30, `vp_total` 5800, `viable` false
- [ ] A `validation_error` on this route uses the same messages as ROI (spot-check extra fields and missing `capex`)
- [ ] Unrecovered payback is `-1`, never a month past 60
- [ ] HTTP slice tests cover these cases without databases; the arithmetic `calculator` tool is not added here
