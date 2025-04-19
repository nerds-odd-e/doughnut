import { describe, expect, it, vi } from "vitest"
import McpTokenDialog from "@/components/users/McpTokenDialog.vue"
import helper from "@tests/helpers"
import { nextTick } from "vue"
import type { User, UserTokenDTO } from "@/generated/backend"

// Helper function to wait for all promises to resolve
const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0))

describe("McpTokenDialog.vue", () => {
  const mockUser: User = {
    id: 123,
    name: "Test User",
    externalIdentifier: "test-id",
  }

  const mockToken: UserTokenDTO = {
    token: "test-token-123",
    createdAt: "2023-01-01T00:00:00Z",
    expiresAt: "2024-01-01T00:00:00Z",
  }

  const mockNewToken: UserTokenDTO = {
    token: "new-generated-token-456",
    createdAt: "2023-02-02T00:00:00Z",
    expiresAt: "2024-02-02T00:00:00Z",
  }

  it("fetches and displays the token on mount", async () => {
    // Setup API mocks
    vi.spyOn(
      helper.managedApi.restUserController,
      "getUserTokens"
    ).mockResolvedValue([mockToken])

    const wrapper = helper
      .component(McpTokenDialog)
      .withCurrentUser(mockUser)
      .mount()

    // Wait for all promises to resolve and DOM to update
    await flushPromises()
    await nextTick()

    const tokenInput = wrapper.find("[data-testid='mcp-token']")
    expect(tokenInput.element.value).toBe(mockToken.token)
    expect(
      helper.managedApi.restUserController.getUserTokens
    ).toHaveBeenCalledWith(mockUser.id)
  })

  it("updates token value when generating new token", async () => {
    // Setup API mocks
    vi.spyOn(
      helper.managedApi.restUserController,
      "getUserTokens"
    ).mockResolvedValue([mockToken])
    vi.spyOn(
      helper.managedApi.restUserController,
      "createUserToken"
    ).mockResolvedValue(mockNewToken)

    const wrapper = helper
      .component(McpTokenDialog)
      .withCurrentUser(mockUser)
      .mount()

    // Wait for initial data to load
    await flushPromises()
    await nextTick()

    // Before clicking generate button, token should be the one from initial fetch
    expect(wrapper.find("[data-testid='mcp-token']").element.value).toBe(
      mockToken.token
    )

    // Click generate button
    await wrapper.find("[data-testid='generate']").trigger("click")

    // Wait for API call to complete and DOM to update
    await flushPromises()
    await nextTick()

    // After generating, token should be updated with new value from API response
    expect(wrapper.find("[data-testid='mcp-token']").element.value).toBe(
      mockNewToken.token
    )

    // Verify API was called with correct parameters
    expect(
      helper.managedApi.restUserController.createUserToken
    ).toHaveBeenCalledWith(mockUser.id)
  })

  it("clears token when deleting", async () => {
    // Setup API mocks
    vi.spyOn(
      helper.managedApi.restUserController,
      "getUserTokens"
    ).mockResolvedValue([mockToken])
    vi.spyOn(
      helper.managedApi.restUserController,
      "deleteUserToken"
    ).mockResolvedValue({})

    const wrapper = helper
      .component(McpTokenDialog)
      .withCurrentUser(mockUser)
      .mount()

    // Wait for initial data to load
    await flushPromises()
    await nextTick()

    // Before clicking delete button, token should be the one from initial fetch
    expect(wrapper.find("[data-testid='mcp-token']").element.value).toBe(
      mockToken.token
    )

    // Click delete button
    await wrapper.find("[data-testid='delete']").trigger("click")

    // Wait for API call to complete and DOM to update
    await flushPromises()
    await nextTick()

    // After deleting, token should be empty
    expect(wrapper.find("[data-testid='mcp-token']").element.value).toBe("")

    // Verify API was called with correct parameters
    expect(
      helper.managedApi.restUserController.deleteUserToken
    ).toHaveBeenCalledWith(mockUser.id)
  })
})
