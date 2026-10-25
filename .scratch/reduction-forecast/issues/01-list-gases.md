# 01: List gas catalog

**What to build:** `GET /calculator/gases` returns every gas in the catalog so the hub can build current-gas and green-gas pickers. Each item is `id`, `name`, `formula`, `is_green`, `gwp`. `is_green` is `gas.is_biogenic` (no new column). Auth is JWT + `calculator:gas`. ROI and payback stay on `authentication-key`.

**Blocked by:** None (can start immediately).

**Status:** done

- [x] `GET /calculator/gases` with JWT + `calculator:gas` returns 200 and the full catalog as a JSON array.
- [x] Each element has `id`, `name`, `formula`, `is_green`, `gwp` (snake_case).
- [x] `is_green` is true only for biogenic rows (today that is `CO2(b)` in the seed; a new biogenic row appears without a code change).
- [x] An empty catalog returns `[]`, not 500.
- [x] No request body; extra query filters are not part of the contract.
- [x] Missing/invalid JWT → 401; JWT without `calculator:gas` → 403. `authentication-key` is not enough.
- [x] `JwtTokenFilter` does not skip this path; it still skips roi and payback. `AuthenticationKeyFilter` stays on those two routes.
- [x] OpenAPI on the calculator docs type describes this operation; success schema is the listing output DTO.
- [x] Behaviour is proven through the `/calculator/**` HTTP seam.
