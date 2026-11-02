# Aether MS Calculator

Microsserviço de cálculos financeiros e ambientais do [Aether](https://github.com/AetherGases): **ROI**, **payback**, **catálogo de gases** e **previsões de redução** (de emissões e de custos). Todos os cálculos são sem estado: nada é gravado.

O [Kong](https://github.com/AetherGases/aether-kong-gateway) encaminha `/api/calculator` para este serviço. Quem o consome é o [ms-aeko-hub](https://github.com/AetherGases/ms-aeko-hub) (ferramentas da IA), o BFF do [portal web](https://github.com/AetherGases/aether-web-flow) e o [app mobile](https://github.com/AetherGases/aether-mobile) (calculadora).

## Rotas

Base: `/api/calculator`.

| Método e rota | Credencial | O que calcula |
| --- | --- | --- |
| `POST /roi` | `authentication-key` (interna) | ROI sobre o CAPEX, a partir do VPL dos fluxos de caixa (`capex`, `wacc_monthly`, `cash_flows` ou `monthly_cash_flow`), horizonte de até 60 meses |
| `POST /payback` | `authentication-key` (interna) | Mês em que o investimento é recuperado em valor presente. `-1` quando não é recuperado no horizonte |
| `GET /gases` | JWT com `calculator:gases` | Catálogo de gases com o potencial de aquecimento global (GWP) |
| `POST /reduction/forecast` | JWT com `calculator:gases` | Emissões futuras ao trocar por um gás verde (`years`, `current_emission`, `green_gas`) |
| `POST /cost-reduction/forecast` | JWT com `calculator:roi` | Simulação de economia: custo mensal, percentual de redução (0 a 50) e período (1 a 60 meses). Devolve economia mensal, novo custo, total e a projeção mês a mês |

Detalhes de payload e exemplos estão no Swagger (`/swagger-ui/index.html`).

## Autenticação

- **`/roi` e `/payback`** são rotas internas: exigem o header `authentication-key` com `INTERNAL_AUTHENTICATION_KEY`. Quem chama é o [ms-aeko-hub](https://github.com/AetherGases/ms-aeko-hub), para as ferramentas de IA.
- **Demais rotas** exigem o JWT do `ms-auth` em `Authorization: Bearer ...`, validado de forma stateless (HMAC com `JWT_SECRET`), e a permissão da tabela: `calculator:gases` para o catálogo e a previsão de emissões, `calculator:roi` para a simulação de custos. A falta da permissão devolve `403`.

## Quirks importantes

- **Validação própria, sem coerção.** Os DTOs de entrada recebem `JsonNode` e os validadores (`*Validator`) rejeitam tipos errados (por exemplo, string no lugar de número) em vez de converter. Campos desconhecidos também são rejeitados.
- **Compatibilidade com o cálculo antigo em Python.** As mensagens de erro reproduzem o formato Python (`PythonRepr`) porque o hub já consumia esse contrato. Mude as mensagens com cuidado.
- **Arredondamento.** A economia mensal usa `HALF_UP` com duas casas antes de derivar o total e a projeção, de modo que o último ponto da projeção é igual ao total.
- **Segurança em duas camadas.** O `SecurityConfig` libera as rotas do calculator no nível do Spring e o `JwtTokenFilter` aplica a permissão por rota. Ao criar uma rota nova, acrescente a regra no filtro, senão ela ficará aberta.
- As entidades JPA em `shared/persistence` são cópias do núcleo comum dos microsserviços Java; o serviço lê o catálogo de gases do PostgreSQL B.

## Stack

Java 21, Spring Boot 4.1, Spring Security, Spring Data JPA (PostgreSQL), SpringDoc OpenAPI, Maven, JUnit 5 e Mockito.

```text
src/main/java/com/aether/ms_calculator
├── calculator/
│   ├── controllers/   # um controller por cálculo
│   ├── domain/        # Calculators, Validators e records de resultado (lógica pura)
│   ├── services/ · mappers/ · dto/
└── shared/            # security (paths, filtros JWT e authentication-key), config, exceptions, persistence
```

A estrutura e as regras para código novo estão em [`PROJECT_STRUCTURE.md`](PROJECT_STRUCTURE.md). Especificações de funcionalidades em andamento ficam em `.scratch/` (não vão para produção).

## Executando localmente

Pré-requisitos: Java 21, Maven 3.9+ e os bancos do [aether-toolskit](https://github.com/AetherGases/aether-toolskit).

```bash
git clone https://github.com/AetherGases/aether-ms-calculator.git
cd aether-ms-calculator
cp .env.example .env
mvn spring-boot:run
```

| Variável | Descrição |
| --- | --- |
| `API_PORT`, `SPRING_PROFILES_ACTIVE` | Porta (padrão `8080`) e perfil |
| `POSTGRES_HOST/PORT/USER/PASSWORD/DB` | PostgreSQL B (catálogo de gases) |
| `MONGO_URL`, `REDIS_HOST/PORT/PASSWORD` | Presentes no exemplo, sem uso nas rotas atuais |
| `INTERNAL_AUTHENTICATION_KEY` | Chave de chamadas entre serviços |
| `JWT_SECRET`, `JWT_EXPIRE_LENGTH` | Mesmo segredo do `ms-auth` |

## Testes e deploy

`mvn clean compile test -DskipTests=false` roda no CI. Publicar uma **release** dispara o deploy no EKS pelo workflow compartilhado da organização ([.github](https://github.com/AetherGases/.github)).

> O repositório contém um arquivo `.env` versionado com valores de desenvolvimento. Não coloque credenciais reais nele; use o `.env.example` como base e mantenha o `.env` fora do Git.
