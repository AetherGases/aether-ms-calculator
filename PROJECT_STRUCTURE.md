# Project structure and practices

This is the contract for how `aether-ms-calculator` is laid out and how new
code must be written. It exists so this service stays aligned with the other
Aether Java microservices (`aether-ms-auth`, `aether-ms-inventory`,
`aether-ms-cloudinary`).

Onboarding (ports, clone, Swagger) belongs in a README. This file is the source
of truth for structure and conventions. If a change disagrees with this
document, update the code and this file in the same change.

Java **21**, Spring Boot, Maven. Default branch is `develop`.

---

## 1. Layout

Root files match the other Java microservices:

```text
.
├── src/
│   ├── main/
│   │   ├── java/com/aether/ms_calculator/
│   │   └── resources/
│   └── test/java/com/aether/ms_calculator/
├── .github/                   CI (tests.yml) and EKS deploy (deploy.yml)
├── .scratch/                  working specs and tickets; not shipped
├── .env.example
├── Dockerfile
├── LICENSE
├── pom.xml
└── PROJECT_STRUCTURE.md
```

Do not add a second build system, a `cmd/` tree, or a Python `tests/` root.
Those belong to Aeko, not to the Java microservices.

### Application package

Base package is `com.aether.ms_calculator`, same pattern as
`com.aether.ms_auth`, `com.aether.ms_inventory`, and
`com.aether.ms_cloudinary`.

```text
com.aether.ms_calculator
├── MsCalculatorApplication.java    process entrypoint
├── calculator/                     this service's feature module
└── shared/                         cross-cutting kernel copied across the MS
```

- One Spring Boot class at the package root: `MsCalculatorApplication`.
- Feature behaviour lives in a module named after the service (`calculator/`),
  the same way auth uses `auth/` + `profile/`, inventory uses `inventory/`,
  and cloudinary uses `cloudinary/`.
- Everything that is not calculator-specific stays in `shared/`.

---

## 2. Feature module

Every Java microservice keeps a feature module in this shape — including
this one. Do not invent a `web/` package.

| Package | Role |
| --- | --- |
| `controllers/` | HTTP adapters (`@RestController`). No business rules. |
| `services/` | Use-cases called by controllers. |
| `dto/input`, `dto/output` | **Required.** Service input and response types for every feature HTTP contract. Optional `dto/request` for JSON bodies bound by the controller, and `dto/query_params` for list filters. |
| `mappers/` | **Required** whenever the feature has DTOs (always). DTO ↔ domain / entity mapping. |
| `domain/` | Pure calculation / parsing that is not an HTTP adapter. Optional. |

`shared/` must not grow new HTTP endpoints. Controllers, feature services, and
feature DTOs stay inside the feature module.

### This service (`calculator/`)

```text
calculator/
├── controllers/   CalculatorRoiController, CalculatorPaybackController
├── services/      CalculatorService
├── domain/        InvestmentValidator, InvestmentCalculator, result records
├── dto/request/   CostReductionForecastRequestDTO, ReductionForecastRequestDTO
├── dto/input/     CalculateInvestmentInputDTO, CostReductionForecastInputDTO, ReductionForecastInputDTO
├── dto/output/    CalculateRoiOutputDTO, CalculatePaybackOutputDTO
└── mappers/       CalculatorMapper
```

| Package | Owns | May import |
| --- | --- | --- |
| `calculator.domain` | parse, validate, calculate, `PythonRepr`, result records | `shared.exceptions` only (no `controllers`, `services`, `dto`, or `shared.security`) |
| `calculator.services` | `CalculatorService` (orchestrates domain for HTTP) | `domain`, `dto`, `mappers` |
| `calculator.controllers` | ROI and payback adapters | `services`, `dto`, `shared.docs` |
| `calculator.dto.request` / `dto.input` / `dto.output` | request / response types for those routes | nothing from `controllers` or `services` |
| `calculator.mappers` | mapping only | `dto`, `domain` |

Controllers implement `shared.docs.CalculatorControllerDocs.Roi` /
`Payback` and stay thin: bind feature DTOs and delegate to
`CalculatorService`. SpringDoc annotations live on that docs type, not
duplicated on the controller methods — same pattern as
`AuthControllerDocs`, `InventoryControllerDocs`, and
`CloudinaryControllerDocs`. Docs schemas point at the output DTOs.

Do not put the internal-key filter, exception types, or
`@RestControllerAdvice` in this module. Those belong in `shared/` (section 3).

### DTOs (required)

Every feature HTTP endpoint uses `dto/input` and `dto/output` types, named
`*InputDTO` / `*OutputDTO` like the other Java MS. Controllers do not take
or return `JsonNode` / `ObjectNode`. Shared error envelopes stay
`shared.handlers.dto.output.ExceptionOutputDTO`; they are not a substitute
for feature DTOs.

ROI and payback share `CalculateInvestmentInputDTO`. Unknown JSON fields
are kept on that input (Jackson `@JsonAnySetter`) so domain validation can
reject them with the hub message. `capex` and `wacc_monthly` stay
`JsonNode` **fields** on the DTOs so the original JSON tokens are echoed
(`150`, not `150.0`). `/roi` and `/payback` are the only allowed use of
`JsonNode` in the HTTP layer.

Every other JSON body binds a typed `dto/request` `*RequestDTO`. The
controller converts it to a typed `dto/input` `*InputDTO` record with
`CalculatorMapper.convert*RequestToInput`, and the service only sees the
input DTO. Request DTOs keep unknown field names (`@JsonAnySetter`) so the
domain validators still reject them with the hub message. Wrong value types
(`"15"` or `15.0` for an integer) fail Jackson binding:
`JacksonConfig.strictScalarCoercion` turns off scalar coercion, and
`GlobalExceptionHandler` reports the field by name.

`CalculatorMapper` is the only place that turns request DTOs into input
DTOs, input DTOs into domain requests, and domain results into output DTOs.
Do not rebuild JSON maps in the controller or service.

### HTTP surface

Kong already mounts this service at `/calculator/`. Controllers must not
repeat that prefix; the paths below are what the process serves.

- `POST /roi`
- `POST /payback`
- `GET /gases`
- `POST /reduction/forecast`
- `POST /cost-reduction/forecast` (camelCase body; JWT + `calculator:roi`)

---

## 3. Shared kernel

`shared/` is the same cross-cutting layout as the other Java microservices:

```text
shared/
├── AetherConstants.java
├── config/                SecurityConfig, OpenAPI, Jackson, Instantiation, properties
├── docs/                  CalculatorControllerDocs (SpringDoc)
├── enums/
├── exceptions/            MsCalculatorException family
├── handlers/              GlobalExceptionHandler + ExceptionOutputDTO
├── helpers/
├── persistence/postgres/  entities/ + repositories/
├── security/              JWT, UserService, AuthenticationKeyFilter
└── services/              MessageService
```

| Area | Rule |
| --- | --- |
| `shared.config` | Process-wide Spring beans. Calculator `@ConfigurationProperties` live under `shared.config.properties`, like Cloudinary. `SecurityConfig` wires JWT **and** the internal-key filter. Feature JSON is bound through standard Jackson + DTOs; do not add a `JsonNode` HTTP converter. |
| `shared.docs` | One `*ControllerDocs` interface per controller. Controllers `implements` it. Do not put OpenAPI annotations only on the controller class. Success schemas are the feature output DTOs, not `ObjectNode`. |
| `shared.exceptions` | **All** API exceptions. Base type is `MsCalculatorException` (rename of leftover `CoreApiException`). Each subclass documents its HTTP status through `getStatus()` (`BadRequestException` → 400, `UnauthorizedException` → 401, …). |
| `shared.handlers` | The only exception mapper: `GlobalExceptionHandler`. It already handles the base exception. Do not add `CalculatorExceptionHandler` or any feature `@RestControllerAdvice`. |
| `shared.persistence.postgres` | JPA entities and repositories. Do not put repositories inside `calculator/`. |
| `shared.security` | JWT stack **and** the `authentication-key` filter. Same place Cloudinary puts `AuthenticationKeyFilter`. |
| `shared.AetherConstants` | Shared length / domain constants. Calculator horizon and field names stay in `calculator.domain`. |

### Exceptions

Domain validation does not declare a `RuntimeException` inside
`calculator.domain`. Throw a type from `shared.exceptions` that extends
`MsCalculatorException` so the HTTP status is part of the type (typically
`BadRequestException` for invalid ROI / payback bodies). The handler maps
that to the response; the feature module does not.

`CoreApiException` is the old core-api name. Auth already renamed it to
`MsAuthException`. This service uses `MsCalculatorException`.

### Internal-key filter

Put the filter in `shared.security` (Cloudinary: `AuthenticationKeyFilter`).
Do not keep a `calculator.security` package. `SecurityConfig` registers it
in the chain; servlet registration of the same bean stays disabled so it
does not run twice.

`shared/` still carries template types from the other MS. Do not invent
calculator business logic there. Do not delete unused shared types in an
opportunistic cleanup unless that cleanup is the requested change.

---

## 4. Resources and environment

```text
src/main/resources/
├── application.yml
├── application-dev.yml
├── application-test.yml
├── messages/messages.properties
└── postgresdb/schema.sql
```

- Copy every setting from `.env.example`. `application.yml` loads
  `optional:file:.env[.properties]`.
- Profiles follow the other MS: `dev`, `staging`, `prod`. Tests use
  `test` (`SPRING_PROFILES_ACTIVE=test`, `MsCalculatorApplicationTests`
  is `@ActiveProfiles("test")`).
- Calculator internal auth is `calculator.authentication.internal-key` ←
  `INTERNAL_AUTHENTICATION_KEY`. Empty key → `503` on protected routes.
- Server port is `API_PORT` (default 8080 in YAML; `.env.example` uses 3001
  for local compose).
- CI applies `src/main/resources/postgresdb/schema.sql` before `mvn test`.

Restart the process after changing environment values.

---

## 5. Tests

Tests live under `src/test/java` and **mirror** the production package they
cover — same rule as auth / inventory / cloudinary.

Sibling MS name the pair `*ControllerTests` and `*ServiceTests` at the feature
root. Mirror that:

| Production | Test |
| --- | --- |
| `MsCalculatorApplication` | `MsCalculatorApplicationTests` (context load, profile `test`) |
| `calculator.domain` | `InvestmentValidatorTest`, `InvestmentCalculatorTest`, `PythonReprTest` |
| `calculator.mappers` | `CalculatorMapperTest` |
| `calculator.controllers` / `calculator.services` | `CalculatorControllerTests`, `CalculatorServiceTests` |
| `shared.security` | filter / chain tests next to the filter |
| (test only) | `calculator.support.CalculatorMockMvcSupport` |

- Write or update the test that fails for the behaviour you are changing
  before (or with) the production change. Do not edit a test to match
  accidental production behaviour without an explicit request.
- Domain tests have no Spring context. HTTP and security tests go through
  `CalculatorMockMvcSupport`.
- Do not hit a live hub, Gemini, or Cloudinary from this suite.
- CI is `.github/workflows/tests.yml`. Local equivalent:

```bash
mvn test
```

After a change, run the test class that covers the behaviour you touched.
Ask which class when that is unclear.

---

## 6. Identity and HTTP

- `/roi` and `/payback` require header `authentication-key` equal to
  `INTERNAL_AUTHENTICATION_KEY`. Same header name and `shared.security`
  placement as `aether-ms-cloudinary`.
- Missing or wrong key → `401` via `UnauthorizedException` and
  `GlobalExceptionHandler`. Do not run validation before that check.
- Unconfigured key is a server fault, not a client validation error.
  It 503s only roi/payback. Forecast and gas listing must not depend on
  `INTERNAL_AUTHENTICATION_KEY`.
- `GET /gases` and `POST /reduction/forecast` require
  `Authorization: Bearer <jwt>` with authority `calculator:gas`.
- `POST /cost-reduction/forecast` requires JWT with authority
  `calculator:roi` (not `calculator:gas`). Internal `authentication-key` does
  not satisfy that path.
- `JwtTokenFilter` skips only roi and payback. Other calculator JWT
  routes (`/gases`, `/reduction/**`) are deny-by-default: they require
  `calculator:gas` unless `CalculatorPaths` marks them as `calculator:roi`
  (`/cost-reduction/**` today).
- Do not bind a user or company from the JSON body. Investment routes are
  service-to-service; forecast identity stays in the JWT.
- `/api/**` stays on the shared JWT chain. `/roi`, `/payback`, `/gases`,
  `/reduction/**`, and `/cost-reduction/**` are `permitAll` in
  `SecurityConfig` **because** the filters authenticate those routes
  (internal key on roi/payback, JWT on gases, gas forecast, and
  cost-reduction forecast). Do not mark those matchers as
  `authenticated()` unless those filters are removed.
- Unexpected failures: log the cause; return a stable client error. Do not
  leak stack traces in the body.

---

## 7. Source style

- Feature classes use Lombok where the other MS do (`@RequiredArgsConstructor`
  on Spring types). Domain records stay plain Java.
- Request field names and validation messages are part of the hub contract
  (`capex`, `wacc_monthly`, `cash_flows`, `monthly_cash_flow`). Do not
  rename them for Java style. HTTP error envelopes go through
  `GlobalExceptionHandler` / `ExceptionOutputDTO`, not a parallel JSON
  shape in the feature module.
- Echo `capex` and `wacc_monthly` with the original JSON tokens
  (`JsonNode` fields on the input/output DTOs, mapped by `CalculatorMapper`).
  Do not re-serialize those two fields as `150.0` when the client sent `150`.
- Horizon is `InvestmentValidator.HORIZON_MONTHS` (60). Do not duplicate that
  number in controllers.
- Prefer small, single-purpose types. Controllers do not call the calculator
  math directly.

---

## 8. Documentation

- **PROJECT_STRUCTURE.md** — this file: how to change the service.
- **`shared.docs.CalculatorControllerDocs`** — OpenAPI for calculator
  routes (`Roi` / `Payback` / `Gases` / `ReductionForecast` /
  `CostReductionForecast` nested interfaces; same role as
  `CloudinaryControllerDocs`). Gas listing uses
  `@ArraySchema`.
- **README.md** — how to run and call it (add or update when onboarding
  text changes; keep structure rules here).
- **`.scratch/`** — specs and tickets; not a substitute for tests or this
  file.
- **`.github/pull_request_template.md`** — PR checklist shared with the
  other Java MS.

Do not commit secrets. Do not put a real `INTERNAL_AUTHENTICATION_KEY` or
`JWT_SECRET` in examples.

---

## 9. Hard “don’ts”

- Do not put controllers or services under `calculator.web`. Use
  `calculator.controllers` and `calculator.services`.
- Do not skip `shared.docs.CalculatorControllerDocs`. Controllers implement
  that interface.
- Do not declare API exceptions in `calculator.domain`. They live in
  `shared.exceptions` and extend `MsCalculatorException` with `getStatus()`.
- Do not keep the base type named `CoreApiException`. Rename it to
  `MsCalculatorException`.
- Do not add a feature `@RestControllerAdvice`. `GlobalExceptionHandler`
  already maps `MsCalculatorException`.
- Do not put the `authentication-key` filter in `calculator.security`. It
  belongs in `shared.security`.
- Do not put `@RestController` types in `shared/` (docs interfaces are not
  controllers).
- Do not put JPA repositories inside `calculator/`.
- Do not authenticate ROI/payback with JWT instead of `authentication-key`.
  Forecast and gas listing use JWT + `calculator:gas`; do not put those
  two routes on the internal key.
- Do not put JPA entities in `calculator.domain` forecast math. Pass
  catalog values (`CatalogGas` id + gwp).
- Do not invent request fields or result fields the hub contract omitted.
- Do not add `JsonNode` / `ObjectNode` fields to calculator DTOs outside
  `/roi` and `/payback`. Bind a typed `calculator.dto.request` DTO and map
  it to `calculator.dto.input` before calling the service.
- Do not skip `dto/input`, `dto/output`, or `mappers/` for a new feature
  endpoint. DTOs are the HTTP contract, not an optional extra.
- Do not skip `mvn test` before claiming the suite is green.
- Do not commit unless the user asked.
