# 02: Per-route calculator JWT authorities

**What to build:** Calculator JWT permissions are per route, so a future cost-reduction path can require `calculator:roi` without inheriting `calculator:gas`. Gases and gas forecast keep `calculator:roi` out of the picture: they still need `calculator:gas`. ROI and payback stay on `authentication-key`. Unknown `/calculator/**` JWT routes stay deny-by-default (still treated as gas), not opened to any authenticated employee.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

- [ ] `GET /calculator/gases` and `POST /calculator/reduction/forecast` still 401 without JWT and 403 with a JWT that only has `calculator` (not `calculator:gas`)
- [ ] Those two routes still 200 with `calculator:gas`; `authentication-key` alone still 401 on them
- [ ] `POST /calculator/roi` and `/payback` still skip JWT and still require `authentication-key`
- [ ] Shared path helpers can mark a calculator route as JWT + `calculator:roi` without that route also demanding `calculator:gas`
- [ ] A calculator path that is not a key route and not a `calculator:roi` route still requires `calculator:gas` (deny-by-default)
- [ ] Existing gases, forecast, roi, payback, and path/filter tests stay green; PROJECT_STRUCTURE identity section records the per-route split
