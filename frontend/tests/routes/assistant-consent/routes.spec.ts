import { describe, expect, it } from "vitest"
import { createMemoryHistory, createRouter } from "vue-router"
import routes from "@/routes/routes"

describe("assistant consent route", () => {
  it("binds an incoming authorization URL to the named consent screen", async () => {
    const router = createRouter({ history: createMemoryHistory(), routes })
    await router.push(
      "/assistant-consent?client_id=registered-assistant&state=pending"
    )
    const route = router.currentRoute.value
    expect(route.name).toBe("assistantConsent")
    const props = route.matched.at(-1)!.props.default as (
      route: unknown
    ) => unknown
    expect(props(route)).toEqual({
      clientId: "registered-assistant",
      state: "pending",
    })
  })
})
