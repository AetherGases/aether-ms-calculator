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
| `dto/input`, `dto/output` | Request and response types. Optional `dto/query_params`. |
| `mappers/` | DTO ↔ domain / entity mapping. |
| `domain/` | Pure calculation / parsing that is not an HTTP adapter. Optional. |

`shared/` must not grow new HTTP endpoints. Controllers, feature services, and
feature DTOs stay inside the feature module.

### This service (`calculator/`)

```text
calculator/
├── controllers/   CalculatorRoiController, CalculatorPaybackController
├── services/      CalculatorService
├── domain/        InvestmentValidator, InvestmentCalculator, results
├── dto/           request / response types when they exist
└── mappers/       DTO ↔ domain mapping when DTOs exist
```

| Package | Owns | May import |
| --- | --- | --- |
| `calculator.domain` | parse, validate, calculate, `PythonRepr`, result records | `shared.exceptions` only (no `controllers`, `services`, or `shared.security`) |
| `calculator.services` | `CalculatorService` (orchestrates domain for HTTP) | `domain`, `dto` |
| `calculator.controllers` | ROI and payback adapters | `services`, `shared.docs` |
| `calculator.dto` | input / output types for those routes | nothing from `controllers` |
| `calculator.mappers` | mapping only | `dto`, `domain` |

Controllers implement `shared.docs.CalculatorControllerDocs` and stay thin:
delegate to `CalculatorService`. SpringDoc annotations live on the docs
interface, not duplicated on the controller methods — same pattern as
`AuthControllerDocs`, `InventoryControllerDocs`, and
`CloudinaryControllerDocs`.

Do not put the internal-key filter, exception types, or
`@RestControllerAdvice` in this module. Those belong in `shared/` (section 3).

Investment math may keep a JSON-node echo of `capex` / `wacc_monthly`. That
does not justify a `web/` package or a feature-local exception handler.

### HTTP surface

- `POST /calculator/roi`
- `POST /calculator/payback`

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
| `shared.config` | Process-wide Spring beans. Calculator `@ConfigurationProperties` live under `shared.config.properties`, like Cloudinary. `SecurityConfig` wires JWT **and** the internal-key filter. |
| `shared.docs` | One `*ControllerDocs` interface per controller. Controllers `implements` it. Do not put OpenAPI annotations only on the controller class. |
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

- `/calculator/roi` and `/calculator/payback` require header
  `authentication-key` equal to `INTERNAL_AUTHENTICATION_KEY`. Same header
  name and `shared.security` placement as `aether-ms-cloudinary`.
- Missing or wrong key → `401` via `UnauthorizedException` and
  `GlobalExceptionHandler`. Do not run validation before that check.
- Unconfigured key is a server fault, not a client validation error.
- Do not bind a user or company from the JSON body. These routes are
  service-to-service; the hub holds identity.
- `/api/**` stays on the shared JWT chain. `/calculator/**` is `permitAll` in
  `SecurityConfig` **because** the internal-key filter authenticates those
  POSTs. Do not mark `/calculator/**` as `authenticated()` unless the
  filter is removed.
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
  (`InvestmentParsed` + `InvestmentResultJson`). Do not re-serialize those
  two fields as `150.0` when the client sent `150`.
- Horizon is `InvestmentValidator.HORIZON_MONTHS` (60). Do not duplicate that
  number in controllers.
- Prefer small, single-purpose types. Controllers do not call the calculator
  math directly.

---

## 8. Documentation

- **PROJECT_STRUCTURE.md** — this file: how to change the service.
- **`shared.docs.CalculatorControllerDocs`** — OpenAPI for calculator
  routes (same role as `CloudinaryControllerDocs`).
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
- Do not authenticate calculator POSTs with JWT instead of
  `authentication-key`.
- Do not invent request fields or result fields the hub contract omitted.
- Do not skip `mvn test` before claiming the suite is green.
- Do not commit unless the user asked.
