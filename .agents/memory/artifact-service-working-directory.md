---
name: Artifact service working directory
description: Managed artifact workflows resolve commands from the artifact directory rather than the repository root.
---

Managed artifact service commands in this workspace start from their artifact directory. Repository-root-prefixed paths therefore resolve incorrectly inside service commands. Keep artifact commands relative to that directory, and have nested scripts derive paths from their own location.

**Why:** A backend workflow failed twice because its command and Maven script assumed the repository root; both layers duplicated the artifact prefix when launched from `artifacts/api-server`.

**How to apply:** When editing an artifact's service configuration or scripts, confirm the workflow working directory from logs and make commands/scripts work both there and from the repository root where practical.
