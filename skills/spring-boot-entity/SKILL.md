---
name: spring-boot-entity
description: "Creates complete Spring Boot entities with JPA annotations, repositories, services, and REST controllers following project conventions with explicit access modifiers"
---

# Spring Boot Entity Creation Skill

This skill creates complete entity implementations following ActivityTracking project conventions.

## When to Use

- Creating a new database entity
- Adding CRUD operations for a new domain object
- Need complete service + repository + controller setup

## What This Skill Generates

1. **JPA Entity** - Domain model with proper annotations
2. **Repository Interface** - Spring Data JPA repository
3. **Service Class** - Business logic layer with transactions
4. **REST Controller** - API endpoints
5. **Unit Tests** - Service layer tests
6. **Integration Tests** - Repository tests with Testcontainers

## Entity Creation Process

### Step 1: Gather Requirements

Ask for:

- Entity name (e.g., "Report", "Category", "Note")
- Fields and types
- Relationships to other entities
- Business rules and validation
- API endpoints needed

### Step 2: Generate Entity

```java
package com.ammons.taskactivity.entity;

/**
 * Entity representing [description of what this represents].
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Entity
@Table(name = "table_name")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "field_name", nullable = false)
    @NotBlank(message = "Field name is required")
    private String fieldName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Getters and setters with explicit access modifiers
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // ... rest of getters/setters
}
```

### Step 3: Generate Repository

```java
package com.ammons.taskactivity.repository;

/**
 * Repository interface for MyEntity data access operations.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Repository
public interface MyEntityRepository extends JpaRepository<MyEntity, Long> {

    List<MyEntity> findByUserId(Long userId);

    Page<MyEntity> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT e FROM MyEntity e WHERE e.user.id = :userId AND e.fieldName LIKE %:keyword%")
    List<MyEntity> searchByUserAndKeyword(
        @Param("userId") Long userId,
        @Param("keyword") String keyword
    );
}
```

### Step 4: Generate Service

```java
package com.ammons.taskactivity.service;

/**
 * Service layer for managing MyEntity business logic.
 * Handles CRUD operations, validation, and business rules.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Service
@Transactional
public class MyEntityService {

    private final MyEntityRepository repository;
    private final UserService userService;

    public MyEntityService(MyEntityRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    /**
     * Retrieves all entities for a specific user.
     *
     * @param userId The user's unique identifier
     * @return List of entities belonging to the user
     */
    @Transactional(readOnly = true)
    public List<MyEntity> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    /**
     * Creates or updates an entity.
     *
     * @param entity The entity to save
     * @return The saved entity with generated ID
     * @throws ValidationException if validation fails
     */
    public MyEntity save(MyEntity entity) {
        validateEntity(entity);
        return repository.save(entity);
    }

    /**
     * Deletes an entity by ID.
     *
     * @param id The entity's unique identifier
     * @throws ResourceNotFoundException if entity not found
     */
    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Entity not found with id: " + id);
        }
        repository.deleteById(id);
    }

    private void validateEntity(MyEntity entity) {
        if (entity.getFieldName() == null || entity.getFieldName().trim().isEmpty()) {
            throw new ValidationException("Field name is required");
        }
    }
}
```

### Step 5: Generate REST Controller

```java
package com.ammons.taskactivity.controller;

/**
 * REST API controller for MyEntity operations.
 * Provides CRUD endpoints with proper HTTP semantics.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@RestController
@RequestMapping("/api/myentities")
@CrossOrigin(origins = "*")
public class MyEntityController {

    private final MyEntityService service;

    public MyEntityController(MyEntityService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<MyEntity>> getAll(Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        List<MyEntity> entities = service.findByUserId(userId);
        return ResponseEntity.ok(entities);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MyEntity> getById(@PathVariable Long id) {
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MyEntity> create(
            @Valid @RequestBody MyEntity entity,
            Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        entity.setUser(new User(userId));
        MyEntity saved = service.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MyEntity> update(
            @PathVariable Long id,
            @Valid @RequestBody MyEntity entity) {
        return service.findById(id)
            .map(existing -> {
                entity.setId(id);
                MyEntity updated = service.save(entity);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            service.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Long getUserIdFromPrincipal(Principal principal) {
        // Extract user ID from JWT principal
        return 1L;  // Implement based on your auth setup
    }
}
```

### Step 6: Generate Tests

```java
/**
 * Unit tests for MyEntityService business logic.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@ExtendWith(MockitoExtension.class)
class MyEntityServiceTest {

    @Mock
    private MyEntityRepository repository;

    @Mock
    private UserService userService;

    @InjectMocks
    private MyEntityService service;

    @Test
    @DisplayName("Should save entity successfully")
    void shouldSaveEntity() {
        // Given
        MyEntity entity = MyEntity.builder()
            .fieldName("Test")
            .build();
        when(repository.save(any())).thenReturn(entity);

        // When
        MyEntity saved = service.save(entity);

        // Then
        assertNotNull(saved);
        verify(repository).save(entity);
    }
}
```

## Project Conventions Enforced

✅ **Explicit Access Modifiers** - Every class, method, field
✅ **JavaDoc** - File-level with @author, @version, @since
✅ **Constructor Injection** - No field injection
✅ **Validation** - Bean Validation annotations
✅ **Transactions** - @Transactional on service methods
✅ **Error Handling** - Proper exception types
✅ **Testing** - Unit + integration tests

## Database Migration

After creating entity, remind to:

1. Add table to `schema.sql` for fresh installs
2. Create migration script for existing databases
3. Update production database

```sql
CREATE TABLE IF NOT EXISTS my_entities (
    id BIGSERIAL PRIMARY KEY,
    field_name VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_my_entities_user_id ON my_entities(user_id);
```

## Angular Service (Optional)

```typescript
/**
 * Service for managing MyEntity operations via REST API.
 *
 * Author: Dean Ammons
 * Date: January 2026
 */
@Injectable({ providedIn: "root" })
export class MyEntityService {
    private apiUrl = "/api/myentities";

    constructor(private http: HttpClient) {}

    getAll(): Observable<MyEntity[]> {
        return this.http.get<MyEntity[]>(this.apiUrl);
    }

    create(entity: MyEntity): Observable<MyEntity> {
        return this.http.post<MyEntity>(this.apiUrl, entity);
    }

    update(id: number, entity: MyEntity): Observable<MyEntity> {
        return this.http.put<MyEntity>(`${this.apiUrl}/${id}`, entity);
    }

    delete(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }
}
```

## Checklist

- [ ] Entity has explicit access modifiers on all members
- [ ] JavaDoc includes @author, @version, @since
- [ ] Repository uses constructor injection
- [ ] Service has @Transactional annotations
- [ ] Controller has proper HTTP status codes
- [ ] Validation annotations on entity fields
- [ ] Unit tests for service logic
- [ ] Integration tests for repository
- [ ] Database migration script created
- [ ] Memory banks updated if architectural change

## Memory Bank References

- Check `ai/java-conventions.md` for coding standards
- Check `ai/common-patterns.md` for templates
- Check `ai/architecture-patterns.md` for entity patterns
