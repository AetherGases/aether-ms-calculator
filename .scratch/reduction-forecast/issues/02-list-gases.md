# 02: List gas catalog

**What to build:** `GET /calculator/gases` returns every gas in the catalog so the hub can build current-gas and green-gas pickers. Each item is `id`, `name`, `formula`, `is_green`, `gwp`. `is_green` is `gas.is_biogenic` (no new column). Auth is the same JWT scheme as ticket 01; this route requires `calculator:gas`.

**Blocked by:** 01 JWT on ROI and payback

**Status:** ready-for-agent

- [ ] `GET /calculator/gases` with JWT + `calculator:gas` returns 200 and the full catalog as a JSON array.
- [ ] Each element has `id`, `name`, `formula`, `is_green`, `gwp` (snake_case).
- [ ] `is_green` is true only for biogenic rows (today that is `CO2(b)` in the seed; a new biogenic row appears without a code change).
- [ ] An empty catalog returns `[]`, not 500.
- [ ] No request body; extra query filters are not part of the contract.
- [ ] Missing/invalid JWT → 401; JWT without `calculator:gas` (including `calculator:roi` only) → 403.
- [ ] OpenAPI on the calculator docs type describes this operation; success schema is the listing output DTO.
- [ ] Behaviour is proven through the `/calculator/**` HTTP seam.
