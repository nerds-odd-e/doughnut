import NonproductionOnlyLoginPage from "@/pages/NonproductionOnlyLoginPage.vue"
import routes from "@/routes/routes"
import helper from "@tests/helpers"
import { screen } from "@testing-library/vue"
import { describe, expect, it } from "vitest"
import { createMemoryHistory, createRouter } from "vue-router"

describe("NonproductionOnlyLoginPage", () => {
  it("shows dev-only sign-in copy and credential fields", async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes,
    })
    await router.push({
      path: "/users/identify",
      query: { from: "/d/notebooks/1" },
    })
    await router.isReady()

    helper.component(NonproductionOnlyLoginPage).withRouter(router).render()

    expect(
      screen.getByRole("heading", {
        name: /This login page is for test and development only/i,
      })
    ).toBeInTheDocument()
    expect(
      screen.getByRole("heading", { name: /Please sign in/i })
    ).toBeInTheDocument()
    expect(document.getElementById("username")).not.toBeNull()
    expect(document.getElementById("password")).not.toBeNull()
    expect(document.getElementById("login-button")).not.toBeNull()
  })
})
