# 01: Calculator JSON 415 envelope

**What to build:** A calculator POST with a Content-Type that is not JSON returns 415 and the shared exception envelope, so the BFF can key off HTTP 415 instead of an untyped Spring body. `application/json` with an optional charset still succeeds. No new forecast route in this ticket.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

- [ ] `POST /calculator/roi` (valid key) with `Content-Type: text/plain` is 415 `{ "message": <english>, "status": "UNSUPPORTED_MEDIA_TYPE", "error_code": "unsupported_media_type" }`
- [ ] The same 415 envelope is returned for `POST /calculator/payback` and `POST /calculator/reduction/forecast` (forecast still needs its JWT) when the type is not JSON
- [ ] `application/json` and `application/json;charset=UTF-8` still reach the existing 200/400/401 behaviour
- [ ] Missing/wrong `authentication-key` on roi remains 401 even with a wrong Content-Type (auth still wins on key routes)
- [ ] No feature `@RestControllerAdvice`; no new catch-all 500 mapper
- [ ] HTTP tests cover 415 without databases
