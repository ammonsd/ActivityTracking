# Dean's Preferences

This file captures Dean's preferred workflows, coding style, and communication preferences for GitHub Copilot.

## Communication Style

**Processing Documentation**: For complex multi-step tasks (especially large documentation updates), create a temporary `Copilot-Processing.md` summary file that includes:

- Original request and context
- Detailed changelog with line numbers and file paths
- Rationale for major decisions made
- Files modified vs. files only referenced
- Statistics (lines added/changed/removed)
- Completeness assessment and next steps/recommendations

This provides an audit trail for review and confirmation before committing changes. The file should be removed after review.

## Code Review

- When requests go against established conventions or best practices, always confirm intent before implementing
- Ask clarifying questions rather than assuming anti-patterns are intentional

## General

- Clear, concise responses preferred
- Direct answers to direct questions
- Thorough explanations for complex topics
