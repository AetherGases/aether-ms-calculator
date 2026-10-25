# 01: JWT on ROI and payback

**What to build:** `POST /calculator/roi` and `POST /calculator/payback` authenticate with the employee's JWT (`Authorization: Bearer`) and require authority `calculator:roi`. The shared internal key is no longer enough — and is no longer required. Investment JSON stays the same; only auth changes. `/calculator/**` is no longer anonymous.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

- [ ] A caller with a valid JWT and `calculator:roi` gets the same ROI and payback JSON as today (200).
- [ ] A caller with no JWT, a blank JWT, or an invalid/expired JWT gets 401 with the shared error envelope (`message`, `status`, `error_code`).
- [ ] A caller with a valid JWT that lacks `calculator:roi` gets 403 with that envelope, even when the body is a valid investment.
- [ ] Sending `authentication-key` without a JWT is 401; the key is not identity.
- [ ] Unconfigured internal key does not produce 503 on these routes.
- [ ] Authorization is decided before investment validation (wrong key/token does not return a body-validation 400).
- [ ] HTTP tests on `/calculator/**` cover this matrix; they do not assert class names or filter internals.
