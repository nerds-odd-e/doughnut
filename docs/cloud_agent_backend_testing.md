# Running Backend Tests in Cursor Cloud Agent

## Overview

This document explains how to run backend unit tests in Cursor's cloud agent environment where Nix is not available. The local development environment uses Nix to manage dependencies (including Java 25 and MySQL), but the cloud agent requires a different approach.

## Problem

The Doughnut project has these requirements:
- **Java 25**: The backend requires Java 25 (specified in `build.gradle`)
- **MySQL**: Backend tests use real database interactions with MySQL
- **Local Environment**: Uses Nix (`flake.nix`) to manage these dependencies
- **Cloud Environment**: Cursor's cloud agent VMs don't have Nix installed

## Solution

A setup script (`scripts/cloud_agent_setup.sh`) that:
1. Downloads and installs Azul Zulu Java 25
2. Downloads and sets up MySQL 8.4 server
3. Initializes test databases
4. Exports necessary environment variables

## Usage

### Quick Start

```bash
# One-time setup (run this first)
source /workspace/scripts/cloud_agent_setup.sh

# Run database migration (if needed)
./backend/gradlew -p backend migrateTestDB -Dspring.profiles.active=test

# Run all backend tests
./backend/gradlew -p backend test -Dspring.profiles.active=test --build-cache --parallel

# Run specific test
./backend/gradlew -p backend test --tests "com.odde.doughnut.services.ai.QuestionEvaluationTest"
```

### What the Setup Script Does

The script performs these steps:

1. **Installs Java 25** (if not already installed)
   - Downloads Azul Zulu JDK 25.0.1 for Linux x64
   - Extracts to `/tmp/java25/zulu25.30.17-ca-jdk25.0.1-linux_x64`
   - Sets `JAVA_HOME` and updates `PATH`

2. **Installs MySQL 8.4** (if not already installed)
   - Downloads MySQL 8.4.3 Community Server
   - Extracts to `/tmp/mysql-8.4.3-linux-glibc2.28-x86_64`
   - Installs required dependencies (`libaio1t64`, `libaio-dev`)
   - Creates compatibility symlink for Ubuntu 24.04

3. **Initializes MySQL**
   - Creates data directory at `/tmp/mysql_data`
   - Initializes with empty root password (for testing only)
   - Starts MySQL server on port 3306 with socket at `/tmp/mysql.sock`

4. **Sets up Doughnut Databases**
   - Creates `doughnut_test`, `doughnut_development`, and `doughnut_e2e_test` databases
   - Creates `doughnut` user with password `doughnut`
   - Grants necessary privileges

5. **Exports Environment Variables**
   ```bash
   JAVA_HOME=/tmp/java25/zulu25.30.17-ca-jdk25.0.1-linux_x64
   MYSQL_HOME=/tmp/mysql-8.4.3-linux-glibc2.28-x86_64
   SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/doughnut_test?allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
   SPRING_DATASOURCE_USERNAME=doughnut
   SPRING_DATASOURCE_PASSWORD=doughnut
   ```

### Idempotent Design

The setup script is designed to be idempotent - you can run it multiple times safely:
- Checks if Java 25 is already installed before downloading
- Checks if MySQL is already downloaded before extracting
- Checks if MySQL data directory exists before initializing
- Checks if MySQL server is running before starting

### Test Types

The backend has two types of tests:

1. **Pure Unit Tests** (no database required)
   - Use `MakeMeWithoutDB` for test data
   - Can run without MySQL
   - Example: `QuestionEvaluationTest`

2. **Integration Tests** (require database)
   - Use `@SpringBootTest` and `@Transactional`
   - Require MySQL to be running
   - Use `MakeMe` (autowired) for test data
   - Example: `TextContentValidatorTest`, `RestNoteControllerTests`

## Comparison: Local vs Cloud Agent

| Aspect | Local Development | Cloud Agent |
|--------|------------------|-------------|
| Environment Manager | Nix (`nix develop`) | Manual setup script |
| Java Installation | `zulu25` from nixpkgs | Downloaded from Azul CDN |
| MySQL Installation | `mysql84` from nixpkgs | Downloaded from MySQL CDN |
| MySQL Port | 3309 | 3306 |
| Command Prefix | `CURSOR_DEV=true nix develop -c` | Direct execution |
| Test Execution | `pnpm backend:verify` | `./backend/gradlew test` |

## Troubleshooting

### Java Version Mismatch

If you see "invalid source release: 25":
```bash
# Check Java version
java -version

# Should show "openjdk version "25.0.1""
# If not, re-source the setup script
source /workspace/scripts/cloud_agent_setup.sh
```

### MySQL Connection Issues

If tests fail with database connection errors:
```bash
# Check if MySQL is running
ps aux | grep mysqld

# Check MySQL logs
tail -50 /tmp/mysql.log

# Restart MySQL by re-sourcing the setup script
pkill mysqld
source /workspace/scripts/cloud_agent_setup.sh
```

### Permission Issues

If you get permission denied errors:
```bash
# Ensure you're not running as root
whoami  # Should show "ubuntu" or similar non-root user
```

## Performance Notes

- Initial setup takes ~2-3 minutes (downloading Java and MySQL)
- Subsequent runs are much faster (skip downloads)
- Test execution time is similar to local development
- Gradle build cache is enabled for faster rebuilds

## Security Considerations

**IMPORTANT**: This setup is for development/testing only:
- MySQL has no root password
- Test databases have weak credentials
- Services bind to localhost only
- All data is stored in /tmp (ephemeral)

## Implementation Details

### Key Technologies

- **Azul Zulu JDK 25**: Chosen for compatibility and reliability
- **MySQL 8.4 Community**: Latest stable version with good performance
- **Gradle**: Uses project's wrapper (ensures consistent Gradle version)
- **Spring Boot Test**: Integration test framework with real database

### File Locations

- Setup script: `/workspace/scripts/cloud_agent_setup.sh`
- Cursor rule: `/workspace/.cursor/rules/backend-development.mdc`
- Init SQL: `/workspace/scripts/sql/init_doughnut_db.sql`
- This documentation: `/workspace/docs/cloud_agent_backend_testing.md`

## Future Improvements

Potential enhancements:
1. Use Docker if available (cleaner isolation)
2. Add Redis setup for tests that need it
3. Create wrapper script for common test commands
4. Add health check verification
5. Support for different MySQL versions

## Related Documentation

- Backend Development Guidelines: `/workspace/.cursor/rules/backend-development.mdc`
- Database Migration: `/workspace/.cursor/rules/db-migration.mdc`
- Local Development Setup: `/workspace/docs/dev_vm.md`
