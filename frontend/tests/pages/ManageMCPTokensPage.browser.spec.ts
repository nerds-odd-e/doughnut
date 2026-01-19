import { describe, it, expect, beforeEach, vi } from "vitest"
import ManageMCPTokensPage from "@/pages/ManageMCPTokensPage.vue"
import helper, { mockSdkService } from "@tests/helpers"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import { page } from "vitest/browser"

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

    helper
      .component(ManageMCPTokensPage)
      .withRouter(router)
      .render()

    await page.getByRole("button", { name: "Generate Token" }).click()
    await page.getByRole("button", { name: "Submit" }).click()

    await expect.element(page.getByText("No Label")).toBeVisible()
  })
})
