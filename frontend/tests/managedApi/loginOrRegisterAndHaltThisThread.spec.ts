import { HealthCheckController } from "@generated/donut-backend-api/sdk.gen"
import loginOrRegisterAndHaltThisThread from "@/managedApi/window/loginOrRegisterAndHaltThisThread"
import { browserLocation } from "@/managedApi/window/signInRedirect"
import { healthcheckPingBody, mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { afterEach, describe, expect, it, vi } from "vitest"

describe("loginOrRegisterAndHaltThisThread", () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  async function haltAfterPing(profiles: string) {
    mockSdkService(HealthCheckController, "ping", healthcheckPingBody(profiles))
    const assignSpy = vi
      .spyOn(browserLocation, "assign")
      .mockImplementation(() => undefined)
    const pendingSignIn = loginOrRegisterAndHaltThisThread()
    await flushPromises()
    expect(pendingSignIn).toBeInstanceOf(Promise)
    return { assignSpy, from: window.location.href }
  }

  it("sends the browser to continue after a prod healthcheck", async () => {
    const { assignSpy, from } = await haltAfterPing("prod")
    expect(assignSpy).toHaveBeenCalledWith(`/login/continue?from=${from}`)
  })

  it("sends the browser to identify after a non-prod healthcheck", async () => {
    const { assignSpy, from } = await haltAfterPing("e2e")
    expect(assignSpy).toHaveBeenCalledWith(`/users/identify?from=${from}`)
  })
})
