# Graph Report - .  (2026-09-05)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 483 nodes · 1034 edges · 29 communities (19 shown, 10 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 88 edges (avg confidence: 0.8)
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
- `JwtAuthenticationFilter` --references--> `UserDetailsServiceImpl`  [EXTRACTED]
  src/main/java/in/proofofconcept/url/shortner/config/JwtAuthenticationFilter.java → src/main/java/in/proofofconcept/url/shortner/service/UserDetailsServiceImpl.java
- `SecurityConfig` --references--> `JwtAuthenticationFilter`  [EXTRACTED]
  src/main/java/in/proofofconcept/url/shortner/config/SecurityConfig.java → src/main/java/in/proofofconcept/url/shortner/config/JwtAuthenticationFilter.java
- `SecurityConfig` --references--> `UserDetailsServiceImpl`  [EXTRACTED]
  src/main/java/in/proofofconcept/url/shortner/config/SecurityConfig.java → src/main/java/in/proofofconcept/url/shortner/service/UserDetailsServiceImpl.java
- `PasswordGateController` --references--> `UrlService`  [EXTRACTED]
  src/main/java/in/proofofconcept/url/shortner/controller/PasswordGateController.java → src/main/java/in/proofofconcept/url/shortner/service/UrlService.java
- `UrlController` --references--> `AnalyticsService`  [EXTRACTED]
  src/main/java/in/proofofconcept/url/shortner/controller/UrlController.java → src/main/java/in/proofofconcept/url/shortner/service/AnalyticsService.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **URL Creation Flow** — url_controller_createshorturl, url_service_fromrequest, url_service_saveurl, url_service_to_response, readme_rate_limiting, readme_malicious_scanner [EXTRACTED 0.95]
- **URL Retrieval Flow** — url_controller_getoriginalurlbyid, url_service_getoriginalurlbyid, url_repository_findbyid [EXTRACTED 0.95]

## Communities (29 total, 10 thin omitted)

### Community 0 - "URL Request Models"
Cohesion: 0.06
Nodes (39): Autowired, ChatModel, PreUpdate, AllArgsConstructor, Builder, Data, LocalDateTime, Long (+31 more)

### Community 1 - "Password Gate Analytics"
Cohesion: 0.06
Nodes (41): Async, GetMapping, HttpServletRequest, PostMapping, ResponseEntity, RestController, String, PasswordGateController (+33 more)

### Community 2 - "User Authentication Controller"
Cohesion: 0.07
Nodes (32): RequestMapping, AuthController, GetMapping, Map, Object, PostMapping, ResponseEntity, RestController (+24 more)

### Community 3 - "URL Management API"
Cohesion: 0.11
Nodes (26): DeleteMapping, PatchMapping, PutMapping, RuntimeException, GetMapping, HttpServletRequest, List, Long (+18 more)

### Community 4 - "API Integration Tests"
Cohesion: 0.12
Nodes (17): AutoConfigureMockMvc, HttpStatus, MockMvc, ObjectMapper, AllArgsConstructor, Builder, Data, NoArgsConstructor (+9 more)

### Community 5 - "JWT Authentication Filter"
Cohesion: 0.15
Nodes (17): Claims, Date, FilterChain, Function, HttpServletResponse, OncePerRequestFilter, SecretKey, Component (+9 more)

### Community 6 - "User Entity Persistence"
Cohesion: 0.10
Nodes (23): JpaRepository, AllArgsConstructor, Builder, Entity, Getter, List, LocalDateTime, Long (+15 more)

### Community 7 - "API Rate Limiting"
Cohesion: 0.22
Nodes (9): Bucket, ConsumptionProbe, Map, Service, String, RateLimitingService, BeforeEach, Test (+1 more)

### Community 8 - "URL Safety Verification"
Cohesion: 0.20
Nodes (10): RestTemplate, Pattern, Service, Set, Slf4j, String, SafeBrowsingService, BeforeEach (+2 more)

### Community 9 - "Global Exception Handling"
Cohesion: 0.22
Nodes (14): ControllerAdvice, Exception, ExceptionHandler, MethodArgumentNotValidException, ErrorResponse, AllArgsConstructor, Builder, Data (+6 more)

### Community 10 - "URL Repository and Cleanup"
Cohesion: 0.20
Nodes (11): Optional, Scheduled, List, LocalDateTime, Long, Repository, UrlRepository, CleanupService (+3 more)

### Community 11 - "Security Configuration"
Cohesion: 0.24
Nodes (10): AuthenticationConfiguration, AuthenticationManager, AuthenticationProvider, Configuration, EnableWebSecurity, HttpSecurity, SecurityFilterChain, Bean (+2 more)

### Community 12 - "Base62 Encoding Logic"
Cohesion: 0.31
Nodes (4): Base62Encoder, String, Base62EncoderTest, Test

### Community 13 - "Snowflake ID Generation"
Cohesion: 0.26
Nodes (4): Component, SnowflakeIdGenerator, Test, SnowflakeIdGeneratorTest

### Community 14 - "Keyword Validation Tests"
Cohesion: 0.32
Nodes (6): ParameterizedTest, BeforeEach, String, Test, ReservedKeywordsValidatorTest, ValueSource

### Community 15 - "Application Entry Point"
Cohesion: 0.31
Nodes (7): EnableAsync, EnableScheduling, ModelMapper, SpringBootApplication, Bean, String, UrlShortnerApplication

### Community 16 - "Analytics Data Models"
Cohesion: 0.36
Nodes (8): ClickAnalyticsResponse, AllArgsConstructor, Builder, Data, LocalDateTime, Long, NoArgsConstructor, String

### Community 17 - "Context Load Tests"
Cohesion: 0.60
Nodes (3): SpringBootTest, Test, UrlShortnerApplicationTests

## Knowledge Gaps
- **9 isolated node(s):** `in.proofofconcept:url-shortner`, `Base62 URL Shortener Engine`, `Distributed Snowflake ID Generator`, `High-Speed Redirection Engine`, `CleanupService` (+4 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **10 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `CustomException` connect `URL Request Models` to `Password Gate Analytics`, `User Authentication Controller`, `URL Management API`, `URL Safety Verification`, `Global Exception Handling`, `Keyword Validation Tests`?**
  _High betweenness centrality (0.210) - this node is a cross-community bridge._
- **Why does `Url` connect `URL Request Models` to `Password Gate Analytics`, `URL Management API`, `API Integration Tests`, `User Entity Persistence`, `URL Repository and Cleanup`?**
  _High betweenness centrality (0.148) - this node is a cross-community bridge._
- **Why does `UserRepository` connect `User Entity Persistence` to `URL Request Models`, `User Authentication Controller`?**
  _High betweenness centrality (0.114) - this node is a cross-community bridge._
- **What connects `in.proofofconcept:url-shortner`, `Base62 URL Shortener Engine`, `Distributed Snowflake ID Generator` to the rest of the system?**
  _9 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `URL Request Models` be split into smaller, more focused modules?**
  _Cohesion score 0.0625 - nodes in this community are weakly interconnected._
- **Should `Password Gate Analytics` be split into smaller, more focused modules?**
  _Cohesion score 0.05901639344262295 - nodes in this community are weakly interconnected._
- **Should `User Authentication Controller` be split into smaller, more focused modules?**
  _Cohesion score 0.07227891156462585 - nodes in this community are weakly interconnected._