---
description: "Require all new or modified code to include descriptive comments with author attribution and timestamp"
applyTo: "**"
---

# Code Attribution Instructions

## Core Requirement

**All new or modified code MUST include descriptive comments with author attribution and timestamp.**

## Attribution Format

When generating or modifying code, add the following comment block at the appropriate location in each file.

### For Java (Use Standard JavaDoc Annotations):

```java
/**
 * Description: [Brief description of what this class does and why it exists]
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
```

### For Other Languages:

```javascript
/**
 * Description: [Brief description of what this code does and why it exists]
 *
 * Author: Dean Ammons
 * Date: January 2026
 */
```

**IMPORTANT:** Author, Version, and Date annotations should ONLY appear in the main file-level description comment, NOT in method/function comments.

## Language-Specific Formats and Placement

### Java

**Important Placement Rules:**

1. Package declaration comes FIRST
2. Import statements come SECOND
3. File-level attribution comment comes THIRD (AFTER imports, BEFORE class/interface declaration)
4. Author and Date ONLY in file-level comment, NOT in method comments

```java
package com.example.project;

import java.util.List;
import java.util.Scanner;

/**
 * Description: [Brief description of what this class does and why it exists]
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
public class MyClass {

    /**
     * Brief description of what this method does.
     *
     * @param value The value to process
     * @return The processed result
     */
    public int processValue(int value) {
        // implementation
    }
}
```

### Python

**Important:** File-level docstring with Author and Date comes FIRST, then imports.

Python supports two documentation formats depending on the script type:

#### Simple Format (Library Modules and Internal Code)

Use this format for library modules, classes, and internal utilities that are imported by other code:

```python
"""
Description: [Brief description of module purpose]

Author: Dean Ammons
Date: January 2026
"""

import sys
import os

def my_function():
    """Brief description of what this function does."""
    pass
```

#### Extended Format (CLI Scripts and Standalone Tools)

Use this format for command-line scripts that users execute directly. This provides comprehensive usage documentation similar to PowerShell's comment-based help:

```python
"""
============================================================================
Script Title

Author: Dean Ammons
Date: January 2026
============================================================================
Brief description of what this script does and its purpose.

PREREQUISITES:
    Required packages and dependencies
    pip install package1 package2

BASIC USAGE:
    python script.py <required_arg>
    python script.py <required_arg> [optional_arg]

COMMAND LINE OPTIONS:
    arg1                    Description of required argument
    arg2                    Description of optional argument

    --option VALUE         Description of command-line option
                            Example: --option value

    --flag                 Description of boolean flag

USAGE EXAMPLES:
    # Example 1: Basic usage
    python script.py input.txt

    # Example 2: With options
    python script.py input.txt --option value

    # Example 3: Complex usage
    python script.py input.txt output.txt --flag --option value

TIPS/NOTES:
    - Important usage notes
    - Best practices
    - Performance considerations

TROUBLESHOOTING:
    Common Issue 1:
        Solution: Description of how to resolve

    Common Issue 2:
        Solution: Description of how to resolve

============================================================================
"""

import sys
import os

def main():
    """Main entry point for the script."""
    pass

if __name__ == "__main__":
    main()
```

**When to use each format:**

- **Simple Format:** Library modules, class definitions, internal utilities, packages
- **Extended Format:** CLI scripts with argparse, standalone tools, user-facing utilities

### JavaScript/TypeScript

**Important:** File-level comment with Author and Date comes FIRST, then imports.

```javascript
/**
 * Description: [Brief description]
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import { Component } from "react";

/**
 * Brief description of what this function does.
 *
 * @param {string} name - The name parameter
 * @returns {string} The formatted result
 */
function formatName(name) {
    // implementation
}
```

### C#

**Important:** Using statements come FIRST, then file-level comment with Author and Date, then namespace/class.

```csharp
using System;
using System.Collections.Generic;

/// <summary>
/// Description: [Brief description]
///
/// Author: Dean Ammons
/// Date: January 2026
/// </summary>
public class MyClass
{
    /// <summary>
    /// Brief description of what this method does.
    /// </summary>
    /// <param name="value">The value to process</param>
    /// <returns>The processed result</returns>
    public int ProcessValue(int value)
    {
        // implementation
    }
}
```

### PowerShell

**Important:** File-level comment comes FIRST using official PowerShell comment-based help format, then param block or other code.

**CRITICAL:** PowerShell uses `<# ... #>` for multi-line comments, NOT `/** ... */` (which causes parse errors).

**Official Format:** Use PowerShell's comment-based help syntax with .SYNOPSIS, .DESCRIPTION, and .NOTES keywords.

```powershell
<#
.SYNOPSIS
    Brief one-line description of what this script does.

.DESCRIPTION
    Detailed description of what this script does and why it exists.
    Can span multiple lines to provide comprehensive context.

.PARAMETER InputPath
    Description of the InputPath parameter.

.EXAMPLE
    .\MyScript.ps1 -InputPath "C:\data\file.txt"
    Example of how to use the script.

.NOTES
    Author: Dean Ammons
    Date: January 2026
#>

param(
    [Parameter(Mandatory=$true)]
    [string]$InputPath
)

# Function comments use the same official format
<#
.SYNOPSIS
    Brief description of what this function does.

.DESCRIPTION
    Detailed description of the function's purpose and behavior.

.PARAMETER Name
    Description of the parameter

.EXAMPLE
    Example-Function -Name "value"
#>
function Example-Function {
    param([string]$Name)
    # implementation
}
```

## Method/Function Attribution

For significant methods or functions, include ONLY descriptive comments WITHOUT Author and Date:

```java
/**
 * Converts imperial units to metric units using standard conversion factors.
 *
 * @param value The imperial value to convert
 * @param conversionType The type of conversion to perform
 * @return The converted metric value
 * @throws IllegalArgumentException if conversionType is invalid
 */
public double convertToMetric(double value, int conversionType) {
    // implementation
}
```

**Note:** Do NOT include "Author: Dean Ammons" or "Date: January 2026" in method/function comments.

## Modification Attribution

When making significant changes to existing code, document the modification with clear attribution.

### Java - Significant Code Changes

For substantial modifications to Java classes, methods, or logic, use JavaDoc-style comments:

```java
/**
 * Modified by: Dean Ammons - February 2026
 * Change: Removed trust in X-Forwarded-For and similar proxy headers
 * Reason: Prevent spoofed source IP in login audit and lockout flows
 */
```

Place this comment:
- **Above the modified method** - For method-level changes
- **Within the class JavaDoc** - For class-level architectural changes
- **As inline block comment** - For changes to specific code blocks

**Example - Method-level modification:**

```java
/**
 * Modified by: Dean Ammons - February 2026
 * Change: Added input validation and sanitization
 * Reason: Prevent SQL injection vulnerabilities
 */
public User findUserByEmail(String email) {
    // Validate and sanitize input
    if (email == null || !isValidEmail(email)) {
        throw new IllegalArgumentException("Invalid email format");
    }
    // implementation
}
```

**Example - Class-level modification (add to existing class JavaDoc):**

```java
/**
 * Description: User authentication and authorization service
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 *
 * Modified by: Dean Ammons - February 2026
 * Change: Implemented rate limiting and account lockout
 * Reason: Prevent brute force attacks on user accounts
 */
public class AuthenticationService {
    // implementation
}
```

### Other Languages - Significant Code Changes

For significant modifications in other languages, use similar multi-line format:

```javascript
/**
 * Modified by: Dean Ammons - February 2026
 * Change: Migrated from callbacks to async/await
 * Reason: Improve code readability and error handling
 */
async function processData(data) {
    // implementation
}
```

```python
"""
Modified by: Dean Ammons - February 2026
Change: Replaced deprecated pandas.append with concat
Reason: pandas.append removed in pandas 2.0
"""
def process_dataframe(df):
    # implementation
```

### Minor Code Changes

For small, incremental changes or bug fixes, use inline comments:

```java
// Modified by: Dean Ammons - February 2026: Fixed off-by-one error
for (int i = 0; i <= array.length - 1; i++) {
    // implementation
}
```

### When to Use Each Format

**Use JavaDoc/multi-line format when:**
- Changing security-related logic
- Modifying authentication or authorization flows
- Refactoring algorithms or business logic
- Fixing critical bugs with production impact
- Making architectural or design pattern changes
- Changes requiring detailed explanation for future maintainers

**Use inline comment format when:**
- Fixing minor bugs (typos, off-by-one errors)
- Updating dependencies or library calls
- Adjusting formatting or styling
- Making small performance optimizations
- Changes that are self-explanatory from the code itself

## Placement Rules by Language

### Java

1. Package declaration comes FIRST
2. Import statements come SECOND
3. **File-level attribution comment comes THIRD (AFTER imports, BEFORE class declaration)**
4. **@author, @version, and @since ONLY in file-level comment**
5. Method-level comments go before method declarations (WITHOUT @author, @version, @since)

### C#

1. Using statements come FIRST
2. File-level attribution comment comes SECOND (before namespace/class declaration)
3. **Author and Date ONLY in file-level comment**
4. Method-level comments go before method declarations (WITHOUT Author and Date)

### Python

1. **File-level docstring with Author and Date comes FIRST (at the very top of file)**
2. Import statements come SECOND
3. Function/class-level docstrings go immediately after definition line (WITHOUT Author and Date)

### JavaScript/TypeScript

1. **File-level comment with Author and Date comes FIRST (at the very top of file)**
2. Import statements come SECOND
3. Function/class-level comments go before declarations (WITHOUT Author and Date)

### PowerShell

1. **File-level comment with Author and Date comes FIRST (at the very top of file)**
2. **MUST use `<# ... #>` syntax, NOT `/** ... \*/`\*\* (which causes parse errors)
3. Param block comes SECOND
4. Function-level comments use `<# .SYNOPSIS ... #>` format (WITHOUT Author and Date)

## Attribution Summary Rules

**DO:**

- ✅ Include @author, @version, and @since in the ONE main file-level comment (Java)
- ✅ Include Author and Date in the ONE main file-level comment (other languages)
- ✅ Place file-level comment AFTER package/imports for Java
- ✅ Place file-level comment AFTER using statements for C#
- ✅ Place file-level comment BEFORE imports for Python/JavaScript/TypeScript
- ✅ Place file-level comment BEFORE param block for PowerShell
- ✅ Use `<# ... #>` syntax for PowerShell, NOT `/** ... */`
- ✅ Include only descriptions in method/function comments

**DON'T:**

- ❌ Include @author, @version, @since in method/function comments (Java)
- ❌ Include Author and Date in method/function comments (other languages)
- ❌ Place file-level comments before package declarations in Java
- ❌ Place file-level comments before using statements in C#
- ❌ Use `/** ... */` syntax in PowerShell (causes parse errors)
- ❌ Repeat attribution annotations multiple times in the same file

## Requirements Checklist

- [ ] File-level attribution comment is placed correctly (after package/imports for Java, after using for C#)
- [ ] **Java: @author, @version, and @since appear ONLY ONCE in the file-level comment**
- [ ] **Other languages: Author and Date appear ONLY ONCE in the file-level comment**
- [ ] Description explains WHAT the code does and WHY it exists
- [ ] Author name is "Dean Ammons"
- [ ] Version starts at "1.0" for new files
- [ ] Date/Since includes current month and year (e.g., "February 2026")
- [ ] Method/function comments do NOT include attribution annotations
- [ ] Significant functions/methods have descriptive comments
- [ ] **Significant code modifications have JavaDoc-style modification comments with Modified by, Change, and Reason**
- [ ] Minor modifications use inline comment format
- [ ] Modification comments clearly explain what was changed and why

## Notes

- This requirement supplements (does not replace) the self-explanatory code guidelines
- Keep descriptions concise but meaningful
- **Java**: Use standard JavaDoc annotations (@author, @version, @since) for better IDE and documentation tool support
- **Other languages**: Use freeform Author and Date fields
- @version typically stays at 1.0 unless the entire class undergoes major revision
- Update @since or Date when making significant modifications to the file
- **For significant code changes, use the JavaDoc-style modification format with Modified by, Change, and Reason**
- For minor changes, inline modification comments are sufficient
- Never place comments before package or using declarations in Java/C#
- **Attribution annotations should appear exactly once per file, in the main description comment only**
