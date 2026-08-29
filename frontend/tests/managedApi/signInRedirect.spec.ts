import { signInRedirectHref } from "@/managedApi/window/signInRedirect"
import { healthcheckPingBody } from "@tests/helpers"
import { describe, expect, it } from "vitest"

describe("signInRedirectHref", () => {
  const current = "https://example.test/n77"

  it.each([
    ["prod", "/login/continue"],
    ["e2e", "/users/identify"],
    ["test", "/users/identify"],
    ["production", "/users/identify"],
    ["e2e, prod", "/login/continue"],
    ["prod, e2e", "/login/continue"],
    ["prod-like", "/users/identify"],
  ])("Active Profile %s → %s", (profiles, path) => {
    expect(signInRedirectHref(current, healthcheckPingBody(profiles))).toBe(
      `${path}?from=${current}`
    )
  })
})
