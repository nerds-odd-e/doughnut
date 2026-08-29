import { HealthCheckController } from "@generated/donut-backend-api/sdk.gen"
import * as signInRedirect from "@/managedApi/window/signInRedirect"
import NonproductionOnlyLoginPage from "@/pages/NonproductionOnlyLoginPage.vue"
import helper, { healthcheckPingBody, mockSdkService } from "@tests/helpers"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, describe, expect, it, vi } from "vitest"

async function renderIdentifyPage(query: { from?: string } = {}) {
  helper
    .component(NonproductionOnlyLoginPage)
    .withRouter()
    .currentRoute({ path: "/users/identify", query })
    .render()
  await flushPromises()
}

describe("NonproductionOnlyLoginPage", () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it("shows dev-only sign-in copy and credential fields when backend is not prod", async () => {
    mockSdkService(HealthCheckController, "ping", healthcheckPingBody("e2e"))
    await renderIdentifyPage({ from: "/notebooks/1" })

    expect(
      await screen.findByText(
        /This login page is for test and development only/i
      )
    ).toBeInTheDocument()
    expect(screen.getByText(/Please sign in/i)).toBeInTheDocument()
    expect(document.getElementById("username")).not.toBeNull()
    expect(document.getElementById("password")).not.toBeNull()
    expect(document.getElementById("login-button")).not.toBeNull()
  })

  it("does not show the password form and sends the browser to continue when backend is prod", async () => {
    mockSdkService(HealthCheckController, "ping", healthcheckPingBody("prod"))
    const assignSpy = vi
      .spyOn(signInRedirect.browserLocation, "assign")
      .mockImplementation(() => undefined)
    await renderIdentifyPage({ from: "/notebooks/1" })

    expect(document.getElementById("username")).toBeNull()
    expect(document.getElementById("password")).toBeNull()
    expect(assignSpy).toHaveBeenCalledWith("/login/continue?from=/notebooks/1")
  })

  it("continues to / when the identify screen has no from query", async () => {
    mockSdkService(HealthCheckController, "ping", healthcheckPingBody("prod"))
    const assignSpy = vi
      .spyOn(signInRedirect.browserLocation, "assign")
      .mockImplementation(() => undefined)
    await renderIdentifyPage()

    expect(document.getElementById("username")).toBeNull()
    expect(assignSpy).toHaveBeenCalledWith("/login/continue?from=/")
  })
})
