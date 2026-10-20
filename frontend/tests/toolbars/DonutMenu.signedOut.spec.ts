import { HealthCheckController } from "@generated/donut-backend-api/sdk.gen"
import { browserLocation } from "@/managedApi/window/browserLocation"
import { fireEvent, screen } from "@testing-library/vue"
import { healthcheckPingBody, mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect, vi } from "vitest"
import { userEvent } from "vitest/browser"
import {
  launcher,
  openDonutMenu,
  panelItemLabels,
  setupDonutMenuTests,
} from "./donutMenuTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceMockExports } = await import("./menuDataMocks")
  return aiReplyEventSourceMockExports()
})

setupDonutMenuTests()

describe("Donut menu for a signed-out visitor", () => {
  it("offers only the sign-in action and the attribution", async () => {
    await openDonutMenu(null)

    expect(panelItemLabels()).toEqual([])
    expect(screen.getByLabelText("Login via Github")).toBeInTheDocument()
    expect(screen.getByText("Donut by")).toBeInTheDocument()
  })

  it("starts the login flow when the sign-in action is pressed", async () => {
    mockSdkService(HealthCheckController, "ping", healthcheckPingBody("e2e"))
    const assignSpy = vi
      .spyOn(browserLocation, "assign")
      .mockImplementation(() => undefined)
    await openDonutMenu(null)

    await fireEvent.click(screen.getByLabelText("Login via Github"))
    await flushPromises()

    expect(assignSpy).toHaveBeenCalledWith(
      `/users/identify?from=${window.location.href}`
    )
  })

  it("reaches the sign-in action by Tab and starts the login flow on Enter", async () => {
    mockSdkService(HealthCheckController, "ping", healthcheckPingBody("e2e"))
    const assignSpy = vi
      .spyOn(browserLocation, "assign")
      .mockImplementation(() => undefined)
    await openDonutMenu(null)
    launcher().focus()

    await userEvent.tab()
    expect(document.activeElement).toBe(
      screen.getByLabelText("Login via Github")
    )
    await userEvent.keyboard("{Enter}")
    await flushPromises()

    expect(assignSpy).toHaveBeenCalledWith(
      `/users/identify?from=${window.location.href}`
    )
  })
})
