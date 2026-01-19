---
description: "Java-specific coding standards and best practices for GitHub Copilot"
applyTo: "**.java"
---

# Java Coding Instructions

## Core Java Standards

When generating or reviewing Java code, follow these standards:

### Access Modifiers

**All classes, methods, and fields MUST explicitly declare their access modifier. Never rely on package-private (default) access.**

#### Required Access Modifiers

**Classes:**

```java
// ✅ GOOD: Explicit modifier
public class MyClass { }
private class InnerHelper { }

// ❌ BAD: Missing modifier (package-private by default)
class MyClass { }
```

**Methods:**

```java
// ✅ GOOD: Explicit modifiers
public void publicMethod() { }
private void helperMethod() { }
protected void inheritableMethod() { }

// ❌ BAD: Missing modifier
void someMethod() { }
```

**Fields:**

```java
// ✅ GOOD: Explicit modifiers
private static final double CONVERSION_RATE = 2.54;
public String name;
protected int count;
private List<String> items;

// ❌ BAD: Missing modifier
static final double RATE = 2.54;
String name;
```

#### Access Modifier Guidelines

- Use `public` for API methods/classes that need external access
- Use `private` for internal implementation details (preferred for encapsulation)
- Use `protected` only when subclass access is specifically needed
- Use `private` for constants unless they need to be accessed externally
- Static utility methods should be `public static` or `private static` based on visibility needs
- Inner classes should have explicit access modifiers based on their intended use

#### Enforcement Checklist

When generating Java code:

- [ ] Every class has an explicit access modifier (`public`, `protected`, or `private`)
- [ ] Every method has an explicit access modifier
- [ ] Every field has an explicit access modifier
- [ ] No reliance on package-private (default) access

### Naming Conventions

**Classes:**

- PascalCase (e.g., `UserService`, `OrderRepository`)
- Nouns or noun phrases
- Descriptive and specific

**Interfaces:**

- PascalCase (e.g., `Runnable`, `Serializable`, `UserRepository`)
- May use adjective form ending in '-able' or '-ible'
- Or noun phrases describing the contract

**Methods:**

- camelCase (e.g., `getUserById`, `calculateTotal`, `isValid`)
- Verbs or verb phrases
- Boolean methods should start with `is`, `has`, `can`, `should`

**Variables:**

- camelCase (e.g., `userName`, `orderTotal`, `itemCount`)
- Descriptive and meaningful
- Avoid single-letter names except for loop counters

**Constants:**

- UPPER_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT`)
- `static final` fields

**Packages:**

- lowercase, dot-separated (e.g., `com.example.project.service`)
- Follow reverse domain name convention

### Code Structure

**Class Organization:**

```java
public class MyClass {
    // 1. Static constants
    private static final int MAX_SIZE = 100;

    // 2. Static variables
    private static int instanceCount = 0;

    // 3. Instance variables
    private String name;
    private int value;

    // 4. Constructors
    public MyClass(String name) {
        this.name = name;
    }

    // 5. Public methods
    public String getName() {
        return name;
    }

    // 6. Private methods
    private void helperMethod() {
        // implementation
    }

    // 7. Inner classes (if needed)
    private static class Helper {
        // implementation
    }
}
```

### Modern Java Features

When generating Java code, prefer modern features:

**Use Modern Switch Expressions (Java 14+):**

```java
// ✅ GOOD: Modern switch expression
String result = switch (value) {
    case 1 -> "one";
    case 2 -> "two";
    default -> "other";
};

// ❌ AVOID: Traditional switch statement when expression works
String result;
switch (value) {
    case 1:
        result = "one";
        break;
    case 2:
        result = "two";
        break;
    default:
        result = "other";
}
```

**Use Text Blocks (Java 15+) for Multi-line Strings:**

```java
// ✅ GOOD: Text block
String query = """
    SELECT id, name, email
    FROM users
    WHERE status = 'active'
    ORDER BY name
    """;

// ❌ AVOID: String concatenation
String query = "SELECT id, name, email\n" +
    "FROM users\n" +
    "WHERE status = 'active'\n" +
    "ORDER BY name";
```

**Use Records (Java 16+) for Data Classes:**

```java
// ✅ GOOD: Record for immutable data
public record User(String name, String email, int age) { }

// ❌ AVOID: Verbose POJO when record suffices
public class User {
    private final String name;
    private final String email;
    private final int age;

    public User(String name, String email, int age) {
        this.name = name;
        this.email = email;
        this.age = age;
    }

    // getters, equals, hashCode, toString...
}
```

**Use Pattern Matching (Java 16+):**

```java
// ✅ GOOD: Pattern matching instanceof
if (obj instanceof String str) {
    System.out.println(str.toUpperCase());
}

// ❌ AVOID: Traditional instanceof with cast
if (obj instanceof String) {
    String str = (String) obj;
    System.out.println(str.toUpperCase());
}
```

**Use var for Local Variables (Java 10+) When Type is Obvious:**

```java
// ✅ GOOD: var when type is clear from right side
var users = new ArrayList<User>();
var connection = database.getConnection();

// ❌ AVOID: var when type is not obvious
var result = process();  // What type is this?

// ✅ GOOD: Explicit type when clarity needed
UserService userService = new UserServiceImpl();
```

### Exception Handling

**Use Specific Exception Types:**

```java
// ✅ GOOD: Specific exceptions
public User findUserById(long id) throws UserNotFoundException {
    // implementation
}

// ❌ BAD: Generic exceptions
public User findUserById(long id) throws Exception {
    // implementation
}
```

**Include Meaningful Error Messages:**

```java
// ✅ GOOD: Descriptive error message
throw new IllegalArgumentException(
    "User ID must be positive, got: " + userId
);

// ❌ BAD: Generic or missing message
throw new IllegalArgumentException();
```

**Use Try-with-Resources:**

```java
// ✅ GOOD: Try-with-resources
try (var reader = new BufferedReader(new FileReader(file))) {
    return reader.readLine();
}

// ❌ BAD: Manual resource management
BufferedReader reader = new BufferedReader(new FileReader(file));
try {
    return reader.readLine();
} finally {
    reader.close();
}
```

### Collections and Streams

**Prefer Immutable Collections When Possible:**

```java
// ✅ GOOD: Immutable list
List<String> names = List.of("Alice", "Bob", "Charlie");

// ✅ GOOD: Unmodifiable view
List<String> names = Collections.unmodifiableList(mutableList);
```

**Use Streams for Data Processing:**

```java
// ✅ GOOD: Stream for filtering and mapping
List<String> activeUserNames = users.stream()
    .filter(User::isActive)
    .map(User::getName)
    .collect(Collectors.toList());

// ❌ AVOID: Imperative loops when streams are clearer
List<String> activeUserNames = new ArrayList<>();
for (User user : users) {
    if (user.isActive()) {
        activeUserNames.add(user.getName());
    }
}
```

### Null Safety

**Use Optional for Return Values That May Be Absent:**

```java
// ✅ GOOD: Optional for potentially absent value
public Optional<User> findUserByEmail(String email) {
    return users.stream()
        .filter(u -> u.getEmail().equals(email))
        .findFirst();
}

// ❌ BAD: Returning null
public User findUserByEmail(String email) {
    // ... might return null
}
```

**Validate Parameters:**

```java
// ✅ GOOD: Parameter validation
public void setName(String name) {
    this.name = Objects.requireNonNull(name, "Name cannot be null");
}

// ❌ BAD: No validation
public void setName(String name) {
    this.name = name;  // Could be null
}
```

### Testing

**Use JUnit 5:**

```java
// ✅ GOOD: JUnit 5 test structure
@Test
@DisplayName("should calculate discount for premium customers")
void shouldCalculateDiscountForPremiumCustomers() {
    // Given
    Customer customer = new Customer("premium");
    Order order = new Order(100.0);

    // When
    double discount = discountService.calculate(customer, order);

    // Then
    assertEquals(15.0, discount);
}
```

**Use AssertJ for Fluent Assertions:**

```java
// ✅ GOOD: Fluent assertions
assertThat(result)
    .isNotNull()
    .hasSize(3)
    .extracting(User::getName)
    .containsExactly("Alice", "Bob", "Charlie");
```

### Documentation

**Document Public APIs with Javadoc:**

```java
/**
 * Calculates the total price including tax and discount.
 *
 * @param basePrice the original price before tax and discount
 * @param taxRate the tax rate as a decimal (e.g., 0.08 for 8%)
 * @param discountRate the discount rate as a decimal
 * @return the final price after applying tax and discount
 * @throws IllegalArgumentException if basePrice is negative
 */
public double calculateTotal(double basePrice, double taxRate, double discountRate) {
    // implementation
}
```

### Performance Considerations

**Use StringBuilder for String Concatenation in Loops:**

```java
// ✅ GOOD: StringBuilder for loops
StringBuilder sb = new StringBuilder();
for (String item : items) {
    sb.append(item).append(", ");
}
return sb.toString();

// ❌ BAD: String concatenation in loop
String result = "";
for (String item : items) {
    result += item + ", ";  // Creates new String each iteration
}
```

**Cache Expensive Computations:**

```java
// ✅ GOOD: Lazy initialization with caching
private String cachedValue;

public String getExpensiveValue() {
    if (cachedValue == null) {
        cachedValue = computeExpensiveValue();
    }
    return cachedValue;
}
```

### Code Duplication and DRY Principle

**CRITICAL RULE: Never allow duplicate code that must be maintained separately.**

**When generating or reviewing code, actively identify and eliminate duplication by extracting reusable helper methods.**

#### Identifying Code Duplication

Look for these patterns:

- Same or very similar code blocks in multiple methods
- Similar logic with minor variations (different variable names, slightly different parameters)
- Copy-paste code patterns
- Repeated validation, transformation, or formatting logic

#### Refactoring Strategy

**Step 1: Identify Common Logic**

```java
// ❌ BAD: Duplicate validation logic in multiple methods
public void processOrder(Order order) {
    if (order == null) {
        throw new IllegalArgumentException("Order cannot be null");
    }
    if (order.getItems() == null || order.getItems().isEmpty()) {
        throw new IllegalArgumentException("Order must have at least one item");
    }
    // process order
}

public void validateOrder(Order order) {
    if (order == null) {
        throw new IllegalArgumentException("Order cannot be null");
    }
    if (order.getItems() == null || order.getItems().isEmpty()) {
        throw new IllegalArgumentException("Order must have at least one item");
    }
    // validate order
}
```

**Step 2: Extract to Helper Method**

```java
// ✅ GOOD: Single validation helper called by both methods
private void validateOrderNotEmpty(Order order) {
    if (order == null) {
        throw new IllegalArgumentException("Order cannot be null");
    }
    if (order.getItems() == null || order.getItems().isEmpty()) {
        throw new IllegalArgumentException("Order must have at least one item");
    }
}

public void processOrder(Order order) {
    validateOrderNotEmpty(order);
    // process order
}

public void validateOrder(Order order) {
    validateOrderNotEmpty(order);
    // validate order
}
```

#### Real-World Example: Email Grouping Logic

**❌ BAD: Duplicate logic in four notification methods**

```java
public void sendBuildSuccessNotification(JenkinsNotificationDto dto) {
    String recipients = jenkinsBuildNotificationEmail;
    String[] emailGroups = recipients.split(",");

    for (String group : emailGroups) {
        String[] emailsInGroup = group.split(";");
        String[] cleanedEmails = Arrays.stream(emailsInGroup)
            .map(String::trim)
            .filter(email -> !email.isEmpty())
            .toArray(String[]::new);

        if (cleanedEmails.length > 0) {
            sendEmailViaAwsSdk(subject, body, cleanedEmails);
        }
    }
}

public void sendBuildFailureNotification(JenkinsNotificationDto dto) {
    String recipients = jenkinsBuildNotificationEmail;
    String[] emailGroups = recipients.split(",");

    for (String group : emailGroups) {
        String[] emailsInGroup = group.split(";");
        String[] cleanedEmails = Arrays.stream(emailsInGroup)
            .map(String::trim)
            .filter(email -> !email.isEmpty())
            .toArray(String[]::new);

        if (cleanedEmails.length > 0) {
            sendEmailViaAwsSdk(subject, body, cleanedEmails);
        }
    }
}

// ... same logic duplicated in sendDeploySuccessNotification()
// ... same logic duplicated in sendDeployFailureNotification()
```

**✅ GOOD: Single helper method called by all four methods**

```java
private void sendEmailsWithGrouping(String recipientsConfig, String subject, String body, String emailType) {
    if (recipientsConfig == null || recipientsConfig.trim().isEmpty()) {
        log.warn("No recipients configured for {}", emailType);
        return;
    }

    String[] emailGroups = recipientsConfig.split(",");

    for (String group : emailGroups) {
        String[] emailsInGroup = group.split(";");
        String[] cleanedEmails = Arrays.stream(emailsInGroup)
            .map(String::trim)
            .filter(email -> !email.isEmpty())
            .toArray(String[]::new);

        if (cleanedEmails.length > 0) {
            if (mailUseAwsSdk) {
                sendEmailViaAwsSdk(subject, body, cleanedEmails);
            } else {
                sendEmailViaSmtp(subject, body, cleanedEmails);
            }
            log.info("Sent {} email to {} recipient(s)", emailType, cleanedEmails.length);
        }
    }
}

public void sendBuildSuccessNotification(JenkinsNotificationDto dto) {
    String subject = "✅ BUILD SUCCESS";
    String body = formatBuildSuccessBody(dto);
    sendEmailsWithGrouping(jenkinsBuildNotificationEmail, subject, body, "build success");
}

public void sendBuildFailureNotification(JenkinsNotificationDto dto) {
    String subject = "❌ BUILD FAILED";
    String body = formatBuildFailureBody(dto);
    sendEmailsWithGrouping(jenkinsBuildNotificationEmail, subject, body, "build failure");
}

public void sendDeploySuccessNotification(JenkinsNotificationDto dto) {
    String subject = "🚀 DEPLOYMENT SUCCESS";
    String body = formatDeploySuccessBody(dto);
    sendEmailsWithGrouping(jenkinsDeployNotificationEmail, subject, body, "deploy success");
}

public void sendDeployFailureNotification(JenkinsNotificationDto dto) {
    String subject = "💥 DEPLOYMENT FAILED";
    String body = formatDeployFailureBody(dto);
    sendEmailsWithGrouping(jenkinsDeployNotificationEmail, subject, body, "deploy failure");
}
```

#### Benefits of Helper Methods

**Maintainability:**

- Fix bugs in one place, not multiple
- Change behavior in one place, affects all callers
- Easier to understand and reason about

**Testability:**

- Test helper method once thoroughly
- Reduces test duplication
- Easier to mock dependencies

**Readability:**

- Intent-revealing method names
- Shorter, more focused methods
- Clearer separation of concerns

#### When to Extract Helper Methods

Extract a helper method when:

- [ ] Same code appears in 2+ places (immediate refactoring)
- [ ] Logic is >5 lines and does a single cohesive task
- [ ] Code needs explanation (method name becomes the explanation)
- [ ] Similar code with minor variations exists
- [ ] Method would improve testability

#### Helper Method Naming

```java
// ✅ GOOD: Intent-revealing names
private void validateOrderNotEmpty(Order order)
private String formatEmailBody(NotificationDto dto)
private void sendEmailsWithGrouping(String recipients, String subject, String body, String type)
private boolean isValidEmail(String email)

// ❌ BAD: Generic or unclear names
private void check(Order order)
private String format(NotificationDto dto)
private void send(String r, String s, String b, String t)
private boolean validate(String input)
```

#### Code Review Checklist for Duplication

When reviewing or generating code, ask:

- [ ] Is this logic already implemented elsewhere?
- [ ] If I need to change this behavior, how many places would I need to update?
- [ ] Could this code block be reused in other methods?
- [ ] Would a helper method with a descriptive name improve clarity?
- [ ] Are there multiple methods doing similar things with slight variations?

**ENFORCEMENT: If duplicate code is found during code generation or review, IMMEDIATELY refactor to use a single helper method before proceeding.**

## Enforcement Rules

When generating or reviewing Java code:

- [ ] All access modifiers are explicitly declared
- [ ] Naming conventions are followed consistently
- [ ] Modern Java features are used appropriately
- [ ] Exceptions are specific and well-documented
- [ ] Collections and streams are used effectively
- [ ] Null safety practices are implemented
- [ ] **No code duplication exists - duplicate logic is extracted into helper methods**
- [ ] **Helper methods have clear, intent-revealing names**
- [ ] **If changing a behavior would require editing multiple methods, refactor to use a single helper**
- [ ] Tests follow JUnit 5 conventions
- [ ] Public APIs have Javadoc documentation
- [ ] Performance considerations are addressed where relevant
