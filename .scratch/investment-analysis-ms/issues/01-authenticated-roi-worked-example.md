# 01: Authenticated ROI worked example

**What to build:** `POST /calculator/roi` accepts the hub’s internal `authentication-key` and, for the canonical investment `{ "capex": 150, "wacc_monthly": 0.25, "cash_flows": [125, 156.25] }`, returns the same 200 JSON the `calculate_roi` tool already answers. A missing or wrong key is `unauthorized`; a missing `INTERNAL_AUTHENTICATION_KEY` is 503 while the header is still required. JWT on `/api/**` stays as it is.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

- [ ] `POST /calculator/roi` with matching `authentication-key` and the worked example returns 200 `{ "capex": 150, "wacc_monthly": 0.25, "months": 60, "vp_total": 200.0, "vpl": 50.0, "roi_percent": 33.33, "viable": true }` (`capex` echoed as the integer 150)
- [ ] Missing header is 401 `{ "error_code": "unauthorized", "message": "Missing authentication key." }` even when the env is empty
- [ ] Wrong header is 401 `{ "error_code": "unauthorized", "message": "Invalid authentication key." }`
- [ ] Header present and env empty is 503 `{ "message": "Calculator authentication is not configured." }` with no `error_code`
- [ ] Comparison of the key is constant-time; boot does not fail when the env is unset; `INTERNAL_AUTHENTICATION_KEY` is named in the example env
- [ ] HTTP slice tests cover the 200 and the auth cases without Postgres, Mongo, or Redis; the existing context-load test is unchanged
- [ ] These routes do not require Bearer JWT; existing `/api/**` JWT behaviour is untouched
