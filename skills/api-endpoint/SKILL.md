---
name: api-endpoint
description: "Creates REST API endpoints following project conventions with proper HTTP methods, validation, error handling, Swagger documentation, and security"
---

# API Endpoint Creation Skill

This skill creates REST API endpoints following ActivityTracking conventions.

## When to Use

- Creating new REST API endpoints
- Need proper HTTP semantics and validation
- Adding API documentation
- Implementing CRUD operations

## API Design Principles

### RESTful Conventions

- **GET** - Retrieve resources (idempotent, safe)
- **POST** - Create resources (non-idempotent)
- **PUT** - Update entire resource (idempotent)
- **PATCH** - Partial update (non-idempotent)
- **DELETE** - Remove resource (idempotent)

### HTTP Status Codes

- **200 OK** - Successful GET, PUT, PATCH
- **201 Created** - Successful POST
- **204 No Content** - Successful DELETE
- **400 Bad Request** - Validation error
- **401 Unauthorized** - Missing/invalid token
- **403 Forbidden** - Insufficient permissions
- **404 Not Found** - Resource not found
- **409 Conflict** - Duplicate resource
- **500 Internal Server Error** - Server error

## Endpoint Creation Process

### Step 1: Define Resource Model

```java
/**
 * DTO for MyResource API requests/responses.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
public class MyResourceDTO {

    @NotNull(message = "ID is required")
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 100, message = "Name must be 3-100 characters")
    private String name;

    @Email(message = "Invalid email format")
    private String email;

    @Min(value = 0, message = "Value must be positive")
    private BigDecimal value;

    // Getters and setters with explicit access modifiers
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    // ... rest of getters/setters
}
```

### Step 2: Create REST Controller

```java
package com.ammons.taskactivity.controller;

/**
 * REST API controller for MyResource operations.
 * Provides CRUD endpoints with proper HTTP semantics, validation, and security.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@RestController
@RequestMapping("/api/myresources")
@CrossOrigin(origins = "*")
@Tag(name = "MyResource", description = "MyResource management APIs")
public class MyResourceController {

    private final MyResourceService service;

    public MyResourceController(MyResourceService service) {
        this.service = service;
    }

    /**
     * Retrieves all resources for the authenticated user.
     *
     * @param principal Authenticated user principal
     * @return List of user's resources
     */
    @GetMapping
    @Operation(summary = "Get all resources", description = "Retrieves all resources for authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved resources"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<MyResourceDTO>> getAll(Principal principal) {
        Long userId = extractUserId(principal);
        List<MyResourceDTO> resources = service.findByUserId(userId);
        return ResponseEntity.ok(resources);
    }

    /**
     * Retrieves a specific resource by ID.
     *
     * @param id Resource ID
     * @param principal Authenticated user principal
     * @return The requested resource
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get resource by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved resource"),
        @ApiResponse(responseCode = "404", description = "Resource not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<MyResourceDTO> getById(
            @PathVariable @Positive Long id,
            Principal principal) {
        Long userId = extractUserId(principal);
        return service.findByIdAndUserId(id, userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new resource.
     *
     * @param dto Resource data
     * @param principal Authenticated user principal
     * @return The created resource with generated ID
     */
    @PostMapping
    @Operation(summary = "Create new resource")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Successfully created resource"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "409", description = "Resource already exists")
    })
    public ResponseEntity<MyResourceDTO> create(
            @Valid @RequestBody MyResourceDTO dto,
            Principal principal) {
        Long userId = extractUserId(principal);
        MyResourceDTO created = service.create(dto, userId);

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();

        return ResponseEntity.created(location).body(created);
    }

    /**
     * Updates an existing resource.
     *
     * @param id Resource ID
     * @param dto Updated resource data
     * @param principal Authenticated user principal
     * @return The updated resource
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update existing resource")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully updated resource"),
        @ApiResponse(responseCode = "404", description = "Resource not found"),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<MyResourceDTO> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody MyResourceDTO dto,
            Principal principal) {
        Long userId = extractUserId(principal);
        return service.update(id, dto, userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deletes a resource.
     *
     * @param id Resource ID
     * @param principal Authenticated user principal
     * @return No content on success
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete resource")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Successfully deleted resource"),
        @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    public ResponseEntity<Void> delete(
            @PathVariable @Positive Long id,
            Principal principal) {
        Long userId = extractUserId(principal);
        boolean deleted = service.delete(id, userId);
        return deleted
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }

    /**
     * Extracts user ID from authenticated principal.
     *
     * @param principal Authenticated user principal
     * @return User ID
     */
    private Long extractUserId(Principal principal) {
        if (principal == null) {
            throw new UnauthorizedException("User not authenticated");
        }
        // Extract from JWT claims
        return JwtUtils.getUserIdFromPrincipal(principal);
    }
}
```

### Step 3: Add Validation

```java
/**
 * Custom validator for MyResource business rules.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Component
public class MyResourceValidator {

    public void validateCreate(MyResourceDTO dto) {
        List<String> errors = new ArrayList<>();

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            errors.add("Name is required");
        }

        if (dto.getValue() != null && dto.getValue().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Value must be positive");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed: " + String.join(", ", errors));
        }
    }
}
```

### Step 4: Add Error Handling

```java
/**
 * Global exception handler for API errors.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Resource Not Found",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            "Access Denied",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid"
            ));

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            "Invalid request parameters",
            LocalDateTime.now(),
            fieldErrors
        );
        return ResponseEntity.badRequest().body(error);
    }
}
```

### Step 5: Add Swagger Documentation

```java
/**
 * OpenAPI configuration for Swagger documentation.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("TaskActivity API")
                .version("1.0.0")
                .description("REST API for Task and Activity tracking")
                .contact(new Contact()
                    .name("Dean Ammons")
                    .email("dean@example.com")))
            .addSecurityItem(new SecurityRequirement().addList("JWT"))
            .components(new Components()
                .addSecuritySchemes("JWT", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
```

## API Testing

### Integration Test

```java
/**
 * Integration tests for MyResource API endpoints.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class MyResourceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;

    @BeforeEach
    void setUp() {
        jwtToken = generateTestJwtToken();
    }

    @Test
    @DisplayName("Should create resource successfully")
    void shouldCreateResource() throws Exception {
        MyResourceDTO dto = new MyResourceDTO();
        dto.setName("Test Resource");
        dto.setValue(BigDecimal.valueOf(100.00));

        mockMvc.perform(post("/api/myresources")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Test Resource"));
    }

    @Test
    @DisplayName("Should return 400 for invalid request")
    void shouldReturn400ForInvalidRequest() throws Exception {
        MyResourceDTO dto = new MyResourceDTO();
        // Missing required name field

        mockMvc.perform(post("/api/myresources")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation Error"));
    }
}
```

## Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/**").authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

## API Conventions Checklist

- [ ] Controller uses @RestController
- [ ] Endpoints follow REST conventions (/api/resources)
- [ ] HTTP methods used correctly (GET, POST, PUT, DELETE)
- [ ] Proper status codes returned
- [ ] Request validation with @Valid
- [ ] Error handling with @RestControllerAdvice
- [ ] Authentication required (JWT)
- [ ] Authorization checks (user can only access their data)
- [ ] Swagger documentation with @Operation
- [ ] Integration tests for all endpoints
- [ ] CORS configured properly
- [ ] Rate limiting on sensitive endpoints

## Common Patterns

### Pagination

```java
@GetMapping
public ResponseEntity<Page<MyResourceDTO>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Principal principal) {
    Long userId = extractUserId(principal);
    Pageable pageable = PageRequest.of(page, size);
    Page<MyResourceDTO> resources = service.findByUserId(userId, pageable);
    return ResponseEntity.ok(resources);
}
```

### Filtering

```java
@GetMapping("/search")
public ResponseEntity<List<MyResourceDTO>> search(
        @RequestParam String keyword,
        @RequestParam(required = false) LocalDate startDate,
        @RequestParam(required = false) LocalDate endDate,
        Principal principal) {
    Long userId = extractUserId(principal);
    List<MyResourceDTO> resources = service.search(userId, keyword, startDate, endDate);
    return ResponseEntity.ok(resources);
}
```

### Bulk Operations

```java
@PostMapping("/bulk")
public ResponseEntity<BulkOperationResult> bulkCreate(
        @Valid @RequestBody List<MyResourceDTO> dtos,
        Principal principal) {
    Long userId = extractUserId(principal);
    BulkOperationResult result = service.bulkCreate(dtos, userId);
    return ResponseEntity.ok(result);
}
```

## Memory Bank References

- Check `ai/architecture-patterns.md` for API design patterns
- Check `ai/common-patterns.md` for controller templates
- Check `docs/Swagger_API_Guide.md` for API documentation standards
