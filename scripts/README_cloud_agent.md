# Cloud Agent Setup Script

## Quick Start

```bash
# Source the setup script (sets up Java 25 and MySQL)
source /workspace/scripts/cloud_agent_setup.sh

# Run backend tests
./backend/gradlew -p backend test -Dspring.profiles.active=test
```

## What This Does

The `cloud_agent_setup.sh` script prepares the Cursor cloud agent environment for running backend unit tests by:

1. Installing Java 25 (required by the project)
2. Installing and configuring MySQL 8.4
3. Setting up test databases
4. Exporting necessary environment variables

## When to Use

Use this script when:
- Running backend tests in Cursor's cloud agent environment
- Nix is not available
- You need Java 25 and MySQL set up quickly

For local development with Nix, use:
```bash
CURSOR_DEV=true nix develop -c pnpm backend:verify
```

## More Information

See `/workspace/docs/cloud_agent_backend_testing.md` for detailed documentation.
