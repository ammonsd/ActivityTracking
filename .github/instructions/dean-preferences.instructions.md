---
description: "Dean's personal preferences for GitHub Copilot interactions and workflows"
applyTo: "**"
---

# Dean's Interaction Preferences

This instruction file defines Dean's preferred workflows, coding style, and communication preferences that should be automatically applied in all GitHub Copilot interactions.

## Communication Style

### General Communication

- Clear, concise responses preferred
- Direct answers to direct questions
- Thorough explanations for complex topics
- Do not create unnecessary markdown documentation files unless specifically requested

### Processing Documentation for Complex Tasks

For complex multi-step tasks (especially large documentation updates, architectural changes, or major refactoring), **always create** a temporary `Copilot-Processing.md` summary file that includes:

- **Original request and context** - What was asked and why
- **Detailed changelog** - All files modified with line numbers and file paths
- **Rationale for major decisions** - Explain key architectural or design choices
- **Files modified vs. files only referenced** - Clear separation of what changed vs. what was consulted
- **Statistics** - Lines added/changed/removed, files affected, methods created
- **Completeness assessment** - What's done, what's pending, what's optional
- **Next steps/recommendations** - Suggested follow-up actions or improvements
- **Testing recommendations** - How to verify the changes work correctly
- **Impact analysis** - How changes affect UX, DX, and system behavior

**Purpose:** This provides an audit trail for review and confirmation before committing changes. The file should be removed after review.

**When to create:**

- Multi-file changes (3+ files)
- Architectural or design pattern changes
- Major refactoring or restructuring
- Complex bug fixes that touch multiple systems
- Documentation updates that span multiple files
- Any change that requires careful review before commit

**When NOT to create:**

- Simple bug fixes in a single file
- Minor text/comment updates
- Obvious typo corrections
- Trivial code changes with no architectural impact

## Code Review & Validation

### Pre-Implementation Validation

- When requests go against established conventions or best practices, **always confirm intent** before implementing
- Ask clarifying questions rather than assuming anti-patterns are intentional
- If a request seems to contradict project conventions, point it out and ask for confirmation
- Suggest better alternatives when appropriate, but defer to user preference

### Code Quality

- Always check for and report compilation errors before considering work complete
- Validate that changes don't break existing functionality
- Ensure changes follow project conventions and instruction files
- Apply appropriate code attribution (author, date) per project standards

## File and Path Management

### Path Handling

- Always use absolute paths when working with files
- Never use relative paths in tool invocations
- Verify file paths exist before attempting operations

### File Creation

- Do not create unnecessary files
- Only create files that are essential to completing the user's request
- Ask before creating new files if intent is unclear

## Tool Usage Efficiency

### Batch Operations

- When performing multiple independent operations, execute them in parallel when possible
- Use `multi_replace_string_in_file` for multiple file edits instead of sequential `replace_string_in_file` calls
- Batch related operations to improve performance and reduce context switching

### Context Management

- Read sufficient context before making changes (don't read line-by-line unnecessarily)
- Use appropriate line ranges when reading files (prefer larger ranges over many small reads)
- Reference instruction files when needed for guidance

## Response Format

### Structured Responses

When providing explanations or summaries:

- Use clear headings and sections
- Use bullet points for lists
- Use code blocks for examples
- Use tables for comparisons when appropriate
- Highlight critical information with bold or emphasis

### Error Communication

- Be specific about what went wrong
- Provide actionable steps to resolve issues
- Include relevant error messages or logs
- Suggest alternative approaches if the primary path fails

## Project Context Awareness

### Respect Project Standards

- Follow all instruction files in `.github/instructions/`
- Respect language-specific conventions (Java, Python, PowerShell, etc.)
- Apply project-specific patterns and architecture
- Maintain consistency with existing codebase style

### Documentation

- Keep documentation synchronized with code changes
- Update README.md when functionality changes
- Create/update documentation as specified in project instructions
- Follow markdown standards defined in `markdown.instructions.md`

## Workflow Preferences

### Iterative Development

- Break complex tasks into logical steps
- Complete each step fully before moving to the next
- Provide progress updates for long-running tasks
- Validate each step before proceeding

### Confirmation Points

- Confirm before making breaking changes
- Confirm before large-scale refactoring
- Confirm when multiple valid approaches exist
- Confirm when requests are ambiguous or unclear

## Special Instructions

### Copilot-Processing.md Format

When creating the processing summary document, use this structure:

1. **Header** - Date, session name
2. **Original Request** - Verbatim user request(s)
3. **Context** - Background and motivation
4. **Detailed Changelog** - File-by-file changes with line numbers
5. **Rationale** - Why decisions were made
6. **Files Modified vs. Referenced** - Clear categorization
7. **Statistics** - Quantified changes
8. **Key Decisions** - Major architectural or design choices
9. **Testing Recommendations** - How to validate
10. **Completeness Assessment** - Done, pending, optional
11. **Impact Analysis** - UX, DX, system behavior
12. **Related Files** - Documentation and instruction files used
13. **Sign-off Checklist** - Quality gates
14. **Conclusion** - Summary and next steps

### Memory Management

- Use `/memories/` for long-term persistent information
- Memory files should be well-organized and clearly named
- Update memory files when learning new preferences or patterns
- Reference memory files when context from previous sessions is needed

---

**Note:** These preferences should be applied consistently across all interactions unless explicitly overridden by the user for a specific request.
