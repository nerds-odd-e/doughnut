# API Loading State Refactoring

## Long-term Goal

Refactor API loading state management to use explicit `apiCallWithLoading` wrapper instead of automatic interceptor-based loading for all API calls.

## Current State

We have two approaches for managing loading state:

1. **Global Client with Interceptors** (default)
   - Automatically sets loading state for ALL API calls via request/response interceptors
   - Loading state is set asynchronously (after the function call returns a Promise)
   - Shows error toasts automatically
   - Used by most API calls in the application

2. **Silent Client** (`globalClientSilent`)
   - No interceptors, no automatic loading state
   - No error toasts
   - Used for background operations or when custom loading/error handling is needed

3. **apiCallWithLoading** (new, recommended)
   - Explicitly wraps API calls that need loading state
   - Sets loading state synchronously (immediately when called)
   - Uses default `globalClient` (will show error toasts via interceptor)
   - Guarantees cleanup in `finally` block
   - Simple API - just wrap the API call

## Target State

- Remove automatic loading state from global client interceptors
- Every API call that needs loading state explicitly uses `apiCallWithLoading`
- Remove `globalClientSilent` (it becomes the default)
- This makes loading state opt-in rather than opt-out

## Benefits

1. **Explicit over implicit** - Clear which API calls show loading state
2. **Synchronous loading** - Loading state set immediately, not asynchronously
3. **Better control** - Each component decides if it needs loading state
4. **Simpler architecture** - No need for two separate clients
5. **Easier debugging** - No confusion about which client to use

## Challenges

### Analysis Required

Every API call in the codebase needs careful analysis:

1. **User-initiated actions** - Usually need loading state
   - Form submissions
   - Button clicks
   - Quiz/assessment answers
   
2. **Background operations** - Usually don't need loading state
   - Eager fetching/prefetching
   - Auto-save operations
   - Polling/periodic updates
   
3. **Page loads** - May need loading state
   - Initial data fetching
   - Navigation-triggered loads

### Migration Strategy

1. Identify all API call locations (search for imports from `@generated/backend/sdk.gen`)
2. Categorize each call (user action, background, page load)
3. Wrap appropriate calls with `apiCallWithLoading`
4. Remove interceptors from global client setup
5. Remove `globalClientSilent` (rename `globalClient` to use silent mode)
6. Test thoroughly to ensure no regression in UX

### Effort Estimate

- ~100+ API call locations across the frontend
- Each requires understanding the context and UX requirements
- Significant testing effort to verify no loading state regression

## Current Progress

- ✅ `apiCallWithLoading` implemented and tested
- ✅ Simplified `apiCallWithLoading` to remove client parameter (always uses `globalClientSilent`)
- ✅ Applied to `RecallPromptComponent` (quiz answers)
- ✅ Applied to `AssessmentQuestion` (assessment answers)
- ⏳ Need to analyze and migrate remaining ~98+ API calls

## Implementation Details

`apiCallWithLoading` has been simplified - just wrap your API call:

```typescript
// Usage - uses default globalClient
const result = await apiCallWithLoading(() =>
  SomeController.someMethod({
    path: { id: 123 },
  })
)

// For truly silent calls (no loading state, no toasts), use globalClientSilent directly
const result = await SomeController.someMethod({
  path: { id: 123 },
  client: globalClientSilent,
})
```

The wrapper doesn't specify a client, so API calls use the default `globalClient` which has interceptors for loading state and error toasts.

## Next Steps

1. Audit all API call locations
2. Create migration plan with prioritization
3. Migrate in phases with testing
4. Remove interceptor-based loading once migration complete

## Notes

- This is a long-term refactoring, not urgent
- Can be done incrementally without breaking existing functionality
- Consider doing this as part of larger UI/UX improvements
- May want to add a linting rule to enforce `apiCallWithLoading` usage pattern

