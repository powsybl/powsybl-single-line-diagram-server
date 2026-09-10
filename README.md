# PowSyBl Single Line Diagram Server

[![Actions Status](https://github.com/powsybl/powsybl-single-line-diagram-server/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/powsybl/powsybl-single-line-diagram-server/actions)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=com.powsybl%3Apowsybl-single-line-diagram-server&metric=coverage)](https://sonarcloud.io/component_measures?id=com.powsybl%3Apowsybl-single-line-diagram-server&metric=coverage)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)
[![Slack](https://img.shields.io/badge/slack-powsybl-blueviolet.svg?logo=slack)](https://join.slack.com/t/powsybl/shared_invite/zt-rzvbuzjk-nxi0boim1RKPS5PjieI0rA)

## Description

The **powsybl-single-line-diagram-server** is a microservice dedicated to **generating power network diagrams** (single-line diagrams and network area diagrams) from a network stored in [powsybl-network-store](https://github.com/powsybl/powsybl-network-store).

It provides the following capabilities:

- **Voltage level diagrams**: generate a single-line diagram (SVG) for a given voltage level, with its metadata (positions, labels) and additional application-level metadata.
- **Substation diagrams**: generate a single-line diagram combining all the voltage levels of a substation.
- **Network area diagrams (NAD)**: generate a geographical/topological overview diagram covering a list of voltage levels, a filter, or a saved configuration, with automatic layout (fixed positions, geographical positions, or force-based layout).
- **NAD configurations**: create, duplicate, update, and delete reusable NAD configurations (selected voltage levels + saved positions + scaling factor) so that a diagram layout can be recomputed identically without asking the client to resend it.
- **NAD configured (reference) positions**: bulk-import via CSV a global set of fixed voltage level positions, used as a base layout for network area diagrams.
- **List available SVG component (icon) libraries**: expose the names of the graphical symbol sets (e.g. `Convergence`, `FlatDesign`, `GridSuiteAndConvergence`) that can be selected via the `componentLibrary` parameter to render single-line diagrams with a different visual style.
- **Highlight current-limit violations** directly on the diagrams (SLD and NAD) via dedicated style providers.
- **Support state-estimation visualisation** on single-line diagrams (dedicated label/style providers).

---

## Technical Stack

- Spring Boot (Web, Data JPA, Actuator)
- PostgreSQL + Liquibase (NAD configurations and configured positions)
- [powsybl-single-line-diagram-core](https://github.com/powsybl/powsybl-diagram) : voltage level / substation single-line diagram generation
- [powsybl-network-area-diagram](https://github.com/powsybl/powsybl-diagram) : network area diagram generation
- [powsybl-network-store-client](https://github.com/powsybl/powsybl-network-store) : network data access
- Spring `RestClient` : outbound HTTP calls to `geo-data-server` (substation coordinates) and `filter-server` (voltage level selection by filter)
- `super-csv` : parsing of CSV files uploaded for NAD configured positions
- Micrometer `context-propagation` : propagates tracing/MDC context into the NAD generation thread pool
- API documentation: OpenAPI / Swagger (`springdoc`)
- Micrometer / Prometheus

---

## Development Scripts

Build Docker image:

```shell
mvn install -DskipTests -Dpowsybl.docker.install
```

Please read [liquibase usage](https://github.com/powsybl/powsybl-parent/#liquibase-usage) for instructions to automatically generate changesets. After you generated a changeset do not forget to add it to git and in `src/main/resources/db/changelog/db.changelog-master.yml`.

---

## Diagram Generation Model

| Diagram type | Endpoint(s) | Output |
|---|---|---|
| Voltage level SLD | `POST /v1/svg/{networkUuid}/{voltageLevelId}`, `GET /v1/metadata/{networkUuid}/{voltageLevelId}`, `POST /v1/svg-and-metadata/{networkUuid}/{voltageLevelId}` | SVG and/or JSON metadata |
| Substation SLD | `POST /v1/substation-svg/{networkUuid}/{substationId}`, `GET /v1/substation-metadata/{networkUuid}/{substationId}`, `POST /v1/substation-svg-and-metadata/{networkUuid}/{substationId}` | SVG and/or JSON metadata |
| Network area diagram | `POST /v1/network-area-diagram/{networkUuid}` (async) | JSON `{svg, metadata, additionalMetadata}` |
| Component libraries | `GET /v1/svg-component-libraries` | List the names of the icon sets (e.g. `Convergence`, `FlatDesign`, `GridSuiteAndConvergence`) usable via the `componentLibrary` parameter |

Network area diagrams can be generated from four sources of voltage levels/positions:

- an explicit list of voltage level ids (with optional depth/expansion),
- a filter evaluated by `filter-server`,
- a saved **NAD configuration** (`/v1/network-area-diagram/config*` endpoints),
- the global **configured positions** table (imported via CSV, see below).

Layout strategies include a fixed/manual layout (from saved or configured positions), a geographical layout (using substation coordinates fetched from `geo-data-server`), and a force-based layout as fallback for voltage levels without known positions.

A guard (`diagram-server.nad.max-voltage-levels`, default `7000`) prevents generating diagrams that would be too large.

---

## SVG Component Libraries

A single-line diagram is drawn using a set of graphical symbols (breaker, disconnector, transformer, generator, ...), each described by an SVG file plus a `components.json` metadata file (anchor points, size, ...). A **component library** is such a symbol set, i.e. a visual "skin" for the diagram.

`powsybl-single-line-diagram-core` provides two built-in libraries (`Convergence`, `FlatDesign`), and this server registers an additional one, `GridSuiteAndConvergence` (the default), which combines both resource sets: it starts from the `Convergence` icons and overrides them with GridSuite-specific ones where available (`GridSuiteAndConvergenceComponentLibrary.java`).

The name of the library to use is passed as the `componentLibrary` parameter when requesting an SVG (`POST /v1/svg/...`); `GET /v1/svg-component-libraries` lets a client discover the list of names it can choose from.

---

## NAD Configuration & Configured Positions

To avoid asking clients to resend a full diagram layout on every request, layouts can be persisted:

- **NAD configuration** (`nadConfig` / `nadConfigVoltageLevel` / `nadVoltageLevelPosition` tables): stores a selected set of voltage levels, their (x, y) and label positions, and a scaling factor. Configurations can be created (single or bulk), duplicated, partially updated, and deleted through the `/v1/network-area-diagram/config*` endpoints.
- **NAD configured positions** (`nadVoltageLevelConfiguredPosition` table): a global reference layout, bulk-replaced from a CSV file uploaded through `POST /v1/supervision/network-area-diagram/config/positions` (parsed with `super-csv`, BOM-safe UTF-8 reading). Used as the base layout when a diagram is requested with the `CONFIGURED` positions generation mode.

---

## Interactions with Other Microservices

```text
┌────────────────────────────────────┐
│ powsybl-single-line-diagram-server │──► network-store-server  (read network data)
│                                    │──► geo-data-server        (substation geographical coordinates)
│                                    │──► filter-server          (resolve voltage levels from a filter)
└────────────────────────────────────┘
```

No message broker is used: the service is purely called synchronously (or asynchronously over plain HTTP, via `CompletableFuture`) by its callers (typically gridsuite's `study-server`).

---

## Concurrency & Micrometer Observability

Network area diagram generation runs on a bounded thread pool (`max-concurrent-nad-generations`, default `3`) to avoid overloading the service with large/concurrent NAD requests. Pool usage is exposed as Micrometer gauges via `DiagramGenerationObserver`:

- `app.diagram.tasks.pool.current` — number of diagrams currently being generated
- `app.diagram.tasks.pool.pending` — number of diagram generation requests queued

---

## Useful Links

- [PowSyBl diagram documentation](https://powsybl.readthedocs.io/projects/powsybl-diagram/latest/index.html)
- [Single-line diagrams documentation](https://powsybl.readthedocs.io/projects/powsybl-diagram/latest/single_line_diagrams/index.html)
- [Network area diagrams documentation](https://powsybl.readthedocs.io/projects/powsybl-diagram/latest/network_area_diagrams/index.html)
