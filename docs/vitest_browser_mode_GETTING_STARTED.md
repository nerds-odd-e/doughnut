# Getting Started with Vitest Browser Mode

This guide will help you get started with Browser Mode testing alongside your existing jsdom tests.

## Installation

First, install the required dependencies:

```bash
cd frontend
pnpm add -D @vitest/browser-playwright @vitest/ui playwright
```

Then install Playwright browsers (one-time setup):

```bash
pnpm exec playwright install chromium
```

**Note**: `@vitest/ui` is required for the `--ui` flag to work. It's already included in `package.json`.

## Running Browser Mode Tests

### Run all browser tests

```bash
pnpm test:browser
```

### Run browser tests in watch mode

```bash
pnpm test:watch
```

## Test File Naming Convention

- **Browser Mode tests**: `*.spec.ts` (runs with real browser)

Both test suites can run in parallel!

## Migration Examples

### Example 1: Simple Component (Modal)

**Browser Version** (`tests/commons/Modal.spec.ts`):

- Uses real Vue Router
- Runs in real browser
- Better CSS rendering with Tailwind/DaisyUI

### Example 2: Component with IntersectionObserver (AiResponse)

**Browser Version** (`tests/components/conversation/AiResponse.spec.ts`):

- ✅ **No IntersectionObserver mock** - uses real API!
- ✅ **No performance mock** - uses real API!
- Still mocks modules (`AiReplyEventSource`) - module mocks still work

## Key Differences in Browser Mode

### 1. Real Browser APIs

- `IntersectionObserver` - Real API, no mocking needed
- `FormData` - Real API
- `Canvas API` - Real API
- `fetch` - Real API (but you can still mock responses)
- `document`, `window` - Real DOM

### 2. Module Mocks Still Work

- `vi.mock()` for modules still works
- You can mock composables, services, etc.
- Only browser APIs don't need mocking

### 3. Async Rendering

- `mount()` and `render()` may need `await` in some cases
- Browser Mode is more async-friendly

### 4. CSS Rendering

- Real CSS rendering with Tailwind and DaisyUI
- Visual debugging available with `--ui` flag

## What's Already Set Up

✅ Browser Mode configuration (`vitest.config.ts`)
✅ Browser setup file (`tests/setupVitest.ts`)
✅ Package.json scripts
✅ Example migrations:

- `tests/commons/Modal.spec.ts`
- `tests/components/conversation/AiResponse.spec.ts`

## Next Steps

1. **Install dependencies** (see Installation above)
2. **Run the example tests** to verify setup:

   ```bash
   pnpm test
   ```

3. **Migrate more tests** following the examples
4. **Use UI mode** for debugging:

   ```bash
   pnpm test:watch
   ```

## Tips

- Start with simple component tests (no browser APIs)
- Remove IntersectionObserver mocks - use real API
- Keep module mocks (`vi.mock()`) - they still work
- Use `--ui` flag to visually debug tests
- Both test suites can run in parallel

## Troubleshooting

### "Playwright browsers not found"

Run: `pnpm exec playwright install chromium`

### Tests are slow

- Browser Mode is slower than jsdom, but still fast
- Use `headless: true` in CI (already configured)
- Use `--ui` only for debugging

## Resources

- [Vitest Browser Mode Docs](https://vitest.dev/guide/browser/)
