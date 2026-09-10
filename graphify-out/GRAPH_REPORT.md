# Graph Report - url-shortner  (2026-09-10)

## Corpus Check
- 50 files · ~10,372 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 502 nodes · 1049 edges · 29 communities (18 shown, 11 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 87 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `fe6d9aa4`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- [[_COMMUNITY_URL Request Models|URL Request Models]]
- [[_COMMUNITY_Password Gate Analytics|Password Gate Analytics]]
- [[_COMMUNITY_User Authentication Controller|User Authentication Controller]]
- [[_COMMUNITY_URL Management API|URL Management API]]
- [[_COMMUNITY_API Integration Tests|API Integration Tests]]
- [[_COMMUNITY_JWT Authentication Filter|JWT Authentication Filter]]
- [[_COMMUNITY_User Entity Persistence|User Entity Persistence]]
- [[_COMMUNITY_API Rate Limiting|API Rate Limiting]]
- [[_COMMUNITY_URL Safety Verification|URL Safety Verification]]
- [[_COMMUNITY_Global Exception Handling|Global Exception Handling]]
- [[_COMMUNITY_URL Repository and Cleanup|URL Repository and Cleanup]]
- [[_COMMUNITY_Security Configuration|Security Configuration]]
- [[_COMMUNITY_Base62 Encoding Logic|Base62 Encoding Logic]]
- [[_COMMUNITY_Snowflake ID Generation|Snowflake ID Generation]]
- [[_COMMUNITY_Keyword Validation Tests|Keyword Validation Tests]]
- [[_COMMUNITY_Application Entry Point|Application Entry Point]]
- [[_COMMUNITY_Analytics Data Models|Analytics Data Models]]
- [[_COMMUNITY_Context Load Tests|Context Load Tests]]
- [[_COMMUNITY_URL Creation Workflow|URL Creation Workflow]]
- [[_COMMUNITY_URL Retrieval Workflow|URL Retrieval Workflow]]
- [[_COMMUNITY_Shortener Engine Services|Shortener Engine Services]]
- [[_COMMUNITY_Project Metadata|Project Metadata]]
- [[_COMMUNITY_Asynchronous Event Pipeline|Asynchronous Event Pipeline]]
- [[_COMMUNITY_JWT Security Filter|JWT Security Filter]]
- [[_COMMUNITY_Malicious Link Scanner|Malicious Link Scanner]]
- [[_COMMUNITY_Layer 7 Rate Limiting|Layer 7 Rate Limiting]]
- [[_COMMUNITY_High-Speed Redirection Engine|High-Speed Redirection Engine]]
- [[_COMMUNITY_Distributed ID Generation|Distributed ID Generation]]

## God Nodes (most connected - your core abstractions)
1. `Url` - 37 edges
2. `UrlService` - 31 edges
3. `CustomException` - 25 edges
4. `User` - 23 edges
5. `AnalyticsService` - 20 edges
6. `UrlController` - 19 edges
7. `UrlResponse` - 19 edges
8. `UrlRepository` - 19 edges
9. `UrlRequest` - 18 edges
10. `ClickAnalytics` - 18 edges

## Surprising Connections (you probably didn't know these)
- `JwtAuthenticationFilter` --references--> `JwtUtil`  [EXTRACTED]
  src/main/java/in/proofofconcept/url/shortner/config/JwtAuthenticationFilter.java → src/main/java/in/proofofconcept/url/shortner/util/JwtUtil.java
- `PasswordGateController` --references--> `UrlService`  [EXTRACTED]
  src/main/java/in/proofofconcept/url/shortner/controller/PasswordGateController.java → src/main/java/in/proofofconcept/url/shortner/service/UrlService.java
- `UrlController` --references--> `AnalyticsService`  [EXTRACTED]
  src/main/java/in/proofofconcept/url/shortner/controller/UrlController.java → src/main/java/in/proofofconcept/url/shortner/service/AnalyticsService.java
- `UrlController` --references--> `QrCodeService`  [EXTRACTED]
  src/main/java/in/proofofconcept/url/shortner/controller/UrlController.java → src/main/java/in/proofofconcept/url/shortner/service/QrCodeService.java
- `UrlController` --references--> `RateLimitingService`  [EXTRACTED]
  src/main/java/in/proofofconcept/url/shortner/controller/UrlController.java → src/main/java/in/proofofconcept/url/shortner/service/RateLimitingService.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **URL Creation Flow** — url_controller_createshorturl, url_service_fromrequest, url_service_saveurl, url_service_to_response, readme_rate_limiting, readme_malicious_scanner [EXTRACTED 0.95]
- **URL Retrieval Flow** — url_controller_getoriginalurlbyid, url_service_getoriginalurlbyid, url_repository_findbyid [EXTRACTED 0.95]

## Communities (29 total, 11 thin omitted)

### Community 0 - "URL Request Models"
Cohesion: 0.08
Nodes (26): Optional, PreUpdate, Scheduled, AllArgsConstructor, Builder, Entity, Getter, List (+18 more)

### Community 1 - "Password Gate Analytics"
Cohesion: 0.05
Nodes (49): Async, GetMapping, HttpServletRequest, PostMapping, ResponseEntity, RestController, String, PasswordGateController (+41 more)

### Community 2 - "User Authentication Controller"
Cohesion: 0.05
Nodes (44): RequestMapping, AuthController, GetMapping, Map, Object, PostMapping, ResponseEntity, RestController (+36 more)

### Community 3 - "URL Management API"
Cohesion: 0.13
Nodes (22): DeleteMapping, PatchMapping, PutMapping, GetMapping, HttpServletRequest, List, Long, Map (+14 more)

### Community 4 - "API Integration Tests"
Cohesion: 0.09
Nodes (25): AutoConfigureMockMvc, HttpStatus, MockMvc, ObjectMapper, AllArgsConstructor, Builder, Data, LocalDateTime (+17 more)

### Community 5 - "JWT Authentication Filter"
Cohesion: 0.24
Nodes (10): Claims, Date, Function, SecretKey, Component, Map, Object, String (+2 more)

### Community 6 - "User Entity Persistence"
Cohesion: 0.14
Nodes (13): 1. Core Functional Features (MVP), 2. User & Link Customisation, 3. Analytics & Telemetry (Asynchronous Event Processing), 4. System Security & Abuse Prevention, API Documentation, Authentication (`/api/v1/auth`), High-Performance URL Shortener Engine (Spring Boot), Key Features & Architecture (+5 more)

### Community 7 - "API Rate Limiting"
Cohesion: 0.22
Nodes (9): Bucket, ConsumptionProbe, Map, Service, String, RateLimitingService, BeforeEach, Test (+1 more)

### Community 8 - "URL Safety Verification"
Cohesion: 0.08
Nodes (25): ParameterizedTest, RestTemplate, CustomException, String, Pattern, Service, Set, Slf4j (+17 more)

### Community 9 - "Global Exception Handling"
Cohesion: 0.22
Nodes (14): ControllerAdvice, Exception, ExceptionHandler, MethodArgumentNotValidException, ErrorResponse, AllArgsConstructor, Builder, Data (+6 more)

### Community 10 - "URL Repository and Cleanup"
Cohesion: 0.50
Nodes (3): Answer, Q: show getOriginalUrlById knowledge map, Source Nodes

### Community 11 - "Security Configuration"
Cohesion: 0.11
Nodes (23): AuthenticationConfiguration, AuthenticationManager, AuthenticationProvider, Configuration, EnableWebSecurity, FilterChain, HttpSecurity, HttpServletResponse (+15 more)

### Community 12 - "Base62 Encoding Logic"
Cohesion: 0.18
Nodes (5): String, Base62Encoder, String, Base62EncoderTest, Test

### Community 13 - "Snowflake ID Generation"
Cohesion: 0.10
Nodes (19): Autowired, ChatModel, JpaRepository, RuntimeException, Long, Repository, UserRepository, Service (+11 more)

### Community 14 - "Keyword Validation Tests"
Cohesion: 0.50
Nodes (3): Answer, Q: show graphify knowledge map for UrlController.createShortUrl(), Source Nodes

### Community 15 - "Application Entry Point"
Cohesion: 0.31
Nodes (7): EnableAsync, EnableScheduling, ModelMapper, SpringBootApplication, Bean, String, UrlShortnerApplication

### Community 17 - "Context Load Tests"
Cohesion: 0.60
Nodes (3): SpringBootTest, Test, UrlShortnerApplicationTests

## Knowledge Gaps
- **24 isolated node(s):** `in.proofofconcept:url-shortner`, `graphify`, `Workflow: graphify`, `1. Core Functional Features (MVP)`, `2. User & Link Customisation` (+19 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **11 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `CustomException` connect `URL Safety Verification` to `Password Gate Analytics`, `User Authentication Controller`, `URL Management API`, `Global Exception Handling`, `Base62 Encoding Logic`, `Snowflake ID Generation`?**
  _High betweenness centrality (0.195) - this node is a cross-community bridge._
- **Why does `Url` connect `URL Request Models` to `Password Gate Analytics`, `User Authentication Controller`, `URL Management API`, `API Integration Tests`, `URL Safety Verification`, `Snowflake ID Generation`?**
  _High betweenness centrality (0.137) - this node is a cross-community bridge._
- **Why does `UserRepository` connect `Snowflake ID Generation` to `User Authentication Controller`, `Security Configuration`, `Base62 Encoding Logic`?**
  _High betweenness centrality (0.106) - this node is a cross-community bridge._
- **What connects `in.proofofconcept:url-shortner`, `graphify`, `Workflow: graphify` to the rest of the system?**
  _24 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `URL Request Models` be split into smaller, more focused modules?**
  _Cohesion score 0.08097165991902834 - nodes in this community are weakly interconnected._
- **Should `Password Gate Analytics` be split into smaller, more focused modules?**
  _Cohesion score 0.05134575569358178 - nodes in this community are weakly interconnected._
- **Should `User Authentication Controller` be split into smaller, more focused modules?**
  _Cohesion score 0.053185271770894216 - nodes in this community are weakly interconnected._