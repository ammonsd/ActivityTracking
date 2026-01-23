# Visitor Counter Feature

## Overview

A privacy-friendly page visit counter that tracks only the number of visitors to specific pages. **No personal data, cookies, or tracking information is collected.**

## Implementation Details

### Backend Components

**Controller:** [VisitorCounterController.java](../src/main/java/com/ammons/taskactivity/controller/VisitorCounterController.java)

- REST endpoint for tracking page visits
- Public API (no authentication required)
- CORS enabled for cross-origin requests

**Service:** [VisitorCounterService.java](../src/main/java/com/ammons/taskactivity/service/VisitorCounterService.java)

- Thread-safe counter implementation using `AtomicLong`
- In-memory storage (counts reset on application restart)
- Supports multiple page tracking

### Frontend Integration

**Example:** [activitytracking.html](../docs/activitytracking.html)

```html
<p id="visitor-count"><span id="visitor-number">Loading...</span> visitors</p>

<script>
    fetch(
        "https://taskactivitytracker.com/api/public/visit/activitytracking-home",
        {
            method: "POST",
        },
    )
        .then((response) => response.json())
        .then((data) => {
            document.getElementById("visitor-number").textContent =
                data.count.toLocaleString();
        });
</script>
```

## API Endpoints

### Increment Visit Count

**POST** `/api/public/visit/{pageName}`

Increments the counter and returns the new count.

**Response:**

```json
{
    "page": "activitytracking-home",
    "count": 1234,
    "timestamp": 1706034567890
}
```

### Get Visit Count (Read-Only)

**GET** `/api/public/visit/{pageName}`

Returns the current count without incrementing.

**Response:**

```json
{
    "page": "activitytracking-home",
    "count": 1234
}
```

### Get All Statistics

**GET** `/api/public/visit/stats`

Returns all page visit counts.

**Response:**

```json
{
    "activitytracking-home": 1234,
    "documentation-page": 567,
    "user-guide": 890
}
```

## Usage Examples

### Basic Counter

```html
<p id="visitor-count"><span id="visitor-number">Loading...</span> visitors</p>

<script>
    fetch("https://yourapp.com/api/public/visit/homepage", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
    })
        .then((response) => response.json())
        .then((data) => {
            document.getElementById("visitor-number").textContent =
                data.count.toLocaleString();
        })
        .catch((error) => {
            // Hide counter if unavailable
            document.getElementById("visitor-count").style.display = "none";
        });
</script>
```

### Multiple Page Tracking

```javascript
const pages = ["homepage", "about", "contact"];

pages.forEach((pageName) => {
    fetch(`/api/public/visit/${pageName}`, { method: "POST" })
        .then((res) => res.json())
        .then((data) => console.log(`${pageName}: ${data.count} visits`));
});
```

### Dashboard Analytics

```javascript
fetch("/api/public/visit/stats")
    .then((res) => res.json())
    .then((stats) => {
        Object.entries(stats).forEach(([page, count]) => {
            console.log(`${page}: ${count} visits`);
        });
    });
```

## Privacy Considerations

✅ **What We Track:**

- Only page visit counts (numbers)
- Page identifiers (e.g., "homepage", "documentation")

❌ **What We DON'T Track:**

- User IP addresses
- User agents or browser information
- Cookies or local storage (except for fallback)
- Geographic location
- User behavior or session data
- Any personally identifiable information (PII)

## Data Persistence

**Current Implementation:**

- Counts are stored **in-memory** using `ConcurrentHashMap`
- Counts **reset when the application restarts**
- Thread-safe for concurrent access

**Future Enhancement:**
To persist counts across restarts, you could:

1. Add database persistence (e.g., PostgreSQL table)
2. Use Redis for distributed counter storage
3. Implement periodic snapshots to disk

## Testing

Run the test suite:

```bash
mvnw test -Dtest=VisitorCounterServiceTest
```

**Test Coverage:**

- Basic increment and get operations
- Multiple page tracking
- Reset operations
- Page name sanitization
- Thread safety (implicit via AtomicLong)

## Security

- **No authentication required** (public endpoint)
- **Rate limiting recommended** (add via Spring Security if needed)
- **Page name sanitization** to prevent injection attacks
- **CORS enabled** for cross-origin requests

## Deployment Notes

1. **Compile the application:**

    ```bash
    mvnw clean compile
    ```

2. **Run tests:**

    ```bash
    mvnw test
    ```

3. **Package for deployment:**

    ```bash
    mvnw clean package -DskipTests
    ```

4. **Configure CORS for S3-hosted HTML:**

   If your HTML file is hosted on S3 (e.g., `https://taskactivity-docs.s3.us-east-1.amazonaws.com`), you MUST add the S3 origin to the CORS allowed origins list.

   **AWS ECS Environment Variable:**
   ```bash
   CORS_ALLOWED_ORIGINS=https://taskactivitytracker.com,https://taskactivity-docs.s3.us-east-1.amazonaws.com
   ```

   **Update via AWS Console:**
   - Go to ECS → Clusters → TaskActivity → Service → Update
   - Edit Task Definition → Container → Environment Variables
   - Add/Update: `CORS_ALLOWED_ORIGINS=https://taskactivitytracker.com,https://taskactivity-docs.s3.us-east-1.amazonaws.com`
   - Save and redeploy

   **Or via PowerShell script:**
   ```powershell
   # In aws/update-ecs-environment.ps1 or similar
   aws ecs update-service `
     --cluster taskactivity-cluster `
     --service taskactivity-service `
     --force-new-deployment `
     --task-definition <your-task-def>
   ```

5. The counter will start at 0 on each deployment/restart

## Monitoring

Access real-time statistics:

```bash
curl https://taskactivitytracker.com/api/public/visit/stats
```

Example response:

```json
{
    "activitytracking-home": 1523,
    "user-guide": 342,
    "technology-stack": 187
}
```

## Author

Dean Ammons  
January 2026
