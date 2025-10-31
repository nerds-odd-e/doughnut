import { describe, it, expect, beforeEach, vi } from "vitest"
import { screen } from "@testing-library/vue"
import ManageMCPTokensPage from "@/pages/ManageMCPTokensPage.vue"
import helper from "@tests/helpers"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"

describe("ManageMCPTokensPage", () => {
  let router: ReturnType<typeof createRouter>

  beforeEach(() => {
    router = createRouter({
      history: createWebHistory(),
      routes,
    })
  })

  it('displays "No Label" when token label is empty', async () => {
    helper.managedApi.restUserController.generateToken = vi
      .fn()
      .mockResolvedValue({
        userToken: {
          token: "mocked-token",
          label: "",
        },
      })

    helper.managedApi.restUserController.getTokens = vi
      .fn()
      .mockResolvedValue([])

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
