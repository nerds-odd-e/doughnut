import { AssistantConsentController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkError } from "@tests/helpers"
import AssistantConsentPage from "@/pages/AssistantConsentPage.vue"
import { namedLocationHref } from "@/routes/namedLocationHref"

function renderConsent() {
  return helper
    .component(AssistantConsentPage)
    .withRouter()
    .withProps({ clientId: "test-assistant", state: "pending-state" })
    .render()
}

describe("AssistantConsentPage", () => {
  it("informs the account before granting the assistant renewable read access", async () => {
    const context = makeMe.aConsentContext.please()
    mockSdkService(
      AssistantConsentController,
      "getAssistantConsentContext",
      context
    )
    const view = renderConsent()
    await flushPromises()

    expect(
      view.getByText("Allow Test assistant to read your notebooks?")
    ).toBeVisible()
    expect(view.getByText("Old Learner")).toBeVisible()
    expect(view.getByText(/search, read, and explore notebooks/)).toBeVisible()
    expect(
      view.getByText(/automatic renewal until you withdraw permission/)
    ).toBeVisible()
    expect(view.getByText("Settings").getAttribute("href")).toBe(
      namedLocationHref({ name: "settingsGeneral" })
    )
    const form = view.container.querySelector("form")!
    const allow = view.getByText("Allow") as HTMLButtonElement
    expect(form.method).toBe("post")
    expect(form.getAttribute("action")).toBe(context.formAction)
    expect(Object.fromEntries(new FormData(form, allow))).toEqual({
      client_id: "test-assistant",
      state: "pending-state",
      _csrf: "csrf-token",
      scope: "notes:read",
    })
  })

  it("denies through the native protocol form without granting a scope", async () => {
    mockSdkService(
      AssistantConsentController,
      "getAssistantConsentContext",
      makeMe.aConsentContext.please()
    )
    const view = renderConsent()
    await flushPromises()
    const form = view.container.querySelector("form")!
    const deny = view.getByText("Deny") as HTMLButtonElement
    expect(Object.fromEntries(new FormData(form, deny))).toEqual({
      client_id: "test-assistant",
      state: "pending-state",
      _csrf: "csrf-token",
    })
  })

  it("lets the user reconnect when the authorization request cannot be loaded", async () => {
    mockSdkService(
      AssistantConsentController,
      "getAssistantConsentContext",
      makeMe.aConsentContext.please()
    ).mockResolvedValue(wrapSdkError({ message: "Invalid consent request" }))
    const view = renderConsent()
    await flushPromises()
    expect(
      view.getByText(/Return to your assistant and start connecting again/)
    ).toBeVisible()
  })
})
