import { describe, it, expect, beforeEach, vi } from "vitest"
import { screen } from "@testing-library/vue"
import ManageMCPTokensPage from "@/pages/ManageMCPTokensPage.vue"
import helper, { mockSdkService } from "@tests/helpers"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"

describe("ManageMCPTokensPage", () => {
  let router: ReturnType<typeof createRouter>

  beforeEach(() => {
    router = createRouter({
      history: createWebHistory(),
      routes,
    })
    vi.clearAllMocks()
  })

  it('displays "No Label" when token label is empty', async () => {
    mockSdkService("generateToken", {
      token: "mocked-token",
      label: "",
      id: 1,
    })
    mockSdkService("getTokens", [])

    const { findByText } = helper
      .component(ManageMCPTokensPage)
      .withRouter(router)
      .render()

    const addButton = await screen.findByRole("button", {
      name: "Generate Token",
    })
    addButton.click()

    const submitButton = await screen.findByText("Submit")
    submitButton.click()

    expect(await findByText("No Label")).toBeInTheDocument()
  })
})
