# Cursor Cloud Agent Setup for Frontend Unit Tests

## Investigation Summary

This document outlines how Cursor's cloud agents (background agents running on Cursor's virtual machines) can run frontend unit tests without requiring nix.

## Key Findings

### Cloud Agent Environment

The Cursor cloud agent virtual machine comes pre-configured with:
- **Node.js**: v22.21.1 ✅ (meets project requirement: >=22)
- **pnpm**: 10.22.0 ✅ (meets project requirement: >=10.22.0)
- **No nix**: Not available (nor needed)

### Local Development Environment

Local development uses:
- **nix flake**: Manages development environment with specific versions
- **Node.js**: v22 (via nix)
- **pnpm**: v10.22.0 (via nix)
- Commands must be prefixed with `CURSOR_DEV=true nix develop -c`

## Solution

### For Cloud Agents

Cloud agents can run commands **directly** without nix:

```bash
# Frontend unit tests
pnpm frontend:test

# Backend unit tests  
pnpm backend:verify

# E2E tests (requires services running)
pnpm cypress run --spec e2e_test/features/<feature-file>.feature
```

### For Local Development

Local agents/developers must use the nix wrapper:

```bash
# Frontend unit tests
CURSOR_DEV=true nix develop -c pnpm frontend:test

# Backend unit tests
CURSOR_DEV=true nix develop -c pnpm backend:verify

# E2E tests
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/<feature-file>.feature
```

## Test Results

Frontend unit tests were successfully executed on the cloud agent:
- **354 tests passed**
- **3 tests skipped**
- **Duration**: ~40 seconds
- **Exit code**: 0 (success)

TypeScript/vue-tsc linting errors were reported but did not prevent test execution.

## Configuration Changes

Updated `/workspace/.cursor/rules/general.mdc` to include:
1. Dedicated section for cloud agents with direct command execution
2. Clear distinction between cloud and local development environments
3. Specific test commands for each environment

## Conclusion

✅ **No additional setup required** for cloud agents to run frontend unit tests  
✅ **Environment is pre-configured** with correct Node.js and pnpm versions  
✅ **Tests run successfully** without nix  
✅ **Documentation updated** in cursor rules for future reference
