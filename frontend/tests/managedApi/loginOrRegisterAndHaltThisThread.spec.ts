import { signInRedirectHref } from "@/managedApi/window/loginOrRegisterAndHaltThisThread"
import { describe, expect, it } from "vitest"

describe("signInRedirectHref", () => {
  it("builds /users/identify with from=current href", () => {
    const current = "https://example.test/n77"
    expect(signInRedirectHref(current)).toBe(`/users/identify?from=${current}`)
  })
})
