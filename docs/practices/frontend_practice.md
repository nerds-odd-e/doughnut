# Frontend Practices

* The front end code is at `frontend/src/` and test code is at `frontend/tests/`
* The test framework is `vitest` with TypeScript
* 

## Backend API access

* The backend API code for the frontend is generated automatically from the OpenAPI specification of the backend
* The generated code is at `frontend/src/generated/backend`
* There is a helper `managedApi` in `useLoadingApi` to access the backend API

Example of api call:

```typescript
import useLoadingApi from "@/managedApi/useLoadingApi"

const { managedApi } = useLoadingApi()

const notebooks = ref<BazaarNotebook[] | undefined>(undefined)

const fetchData = async () => {
  notebooks.value = await managedApi.restBazaarController.bazaar()
}

```

## Core Unit Test Principles

1. **Focus on Component Behavior**
   - Test component behavior through user interactions
   - Avoid testing implementation details
   - Example from Modal.spec.ts:
   ```typescript
   it("click on note when doing review - close-button", async () => {
     const wrapper = mountWithoutTeleport()
     await wrapper.find(".close-button").trigger("click")
     expect(wrapper.emitted().close_request).toHaveLength(1)
   })
   ```

2. **Minimal Mocking**
   - Mock only external dependencies (primarily API calls)
   - Use real component instances when possible
   - Example from NoteShow.spec.ts:
   ```typescript
   helper.managedApi.restNoteController.show = vitest
     .fn()
     .mockResolvedValue(note)
   ```

3. **Data Builder Pattern**
   - Use makeMe factory for creating test data
   - Builders handle complex object creation
   - Example:
   ```typescript
   const note = makeMe.aNoteRealm
     .topicConstructor("Dummy Title")
     .details("Description")
     .please()
   ```

## Testing Styles

The project supports two main testing styles through the helper:

### 1. Mount Style

   ```typescript
   helper
    .component(Component)
    .withStorageProps({ prop1, prop2 })
    .render()
   ```

**Pros:**
- Full component lifecycle
- Access to component instance
- Good for testing component interactions
- Suitable for complex component behavior

**Cons:**
- Slower than render
- May include unnecessary child components
- Can expose implementation details

**Use When:**
- Testing component lifecycle hooks
- Testing complex user interactions
- Need access to component instance methods
- Testing event handling

### 2. Render Style

  ```typescript
helper
.component(Component)
.withStorageProps({ prop1, prop2 })
.render()
  ```

**Pros:**
- Faster than mount
- More focused on component output
- Better isolation
- Less likely to break due to implementation changes

**Cons:**
- Limited access to component internals
- May miss some integration issues

**Use When:**
- Testing component output/rendering
- Simple component behavior
- Focus on accessibility or DOM structure
- Performance is critical

## Test Organization

1. **Test Structure**
   - Group related tests using describe blocks
   - Use descriptive test names
   - Example from ConversationInner.spec.ts:
   ```typescript
   describe("Form submission", () => {
     it("disables submit button for empty messages", async () => {
       // ...
     })
   })
   ```

2. **Setup and Helpers**
   - Use beforeEach for common setup
   - Create helper functions for repeated operations
   - Example:
   ```typescript
   const submitForm = async (message: string) => {
     const form = wrapper.find("form")
     await form.setValue(message)
     await form.trigger("submit")
   }
   ```

## API Testing

1. **ManagedApi Pattern**
   - Use helper.managedApi for API mocking
   - Supports both regular and silent API calls
   - Example:
   ```typescript
   helper.managedApi.restController.method = vi.fn().mockResolvedValue(result)
   ```

2. **Event Source Testing**
   - Special handling for SSE (Server-Sent Events)
   - Use eventSource for streaming responses
   - Example from ConversationInner.spec.ts:
   ```typescript
   helper.managedApi.eventSource.eventSourceRequest.onMessage(
     "thread.message.created",
     JSON.stringify(newMessage)
   )
   ```

## Best Practices

1. **Async Testing**
   - Always use async/await with component updates
   - Use flushPromises for async operations
   - Example:
   ```typescript
   await wrapper.trigger("click")
   await flushPromises()
   ```

2. **Component Props**
   - Use withStorageProps for reactive props
   - Test prop changes and their effects
   - Example:
   ```typescript
   const wrapper = helper
     .component(Component)
     .withStorageProps({ value: initialValue })
     .mount()
   await wrapper.setProps({ value: newValue })
   ```

3. **Event Testing**
   - Test both event emission and handling
   - Verify event payloads
   - Example:
   ```typescript
   await button.trigger("click")
   expect(wrapper.emitted().event_name).toBeTruthy()
   ```

4. **State Management**
   - Test component state changes
   - Verify state affects rendering
   - Example from Quiz.spec.ts:
   ```typescript
   await wrapper.setProps({ currentIndex: 1 })
   expect(mockedRandomQuestionCall).toHaveBeenCalledWith(3)
   ```

5. **Accessibility Testing**
   - Test ARIA attributes
   - Verify keyboard interactions
   - Example:
   ```typescript
   expect(wrapper.find("button")).toHaveAttribute("aria-label")
   ```
