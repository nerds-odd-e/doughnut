import { AssimilationController } from "@generated/donut-backend-api/sdk.gen"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { screen } from "@testing-library/vue"
import { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect, vi } from "vitest"
import { userEvent } from "vitest/browser"
import {
  expectPanelClosed,
  launcher,
  openDonutMenu,
  panelItemLabels,
  router,
  setupDonutMenuTests,
} from "./donutMenuTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceMockExports } = await import("./menuDataMocks")
  return aiReplyEventSourceMockExports()
})

setupDonutMenuTests()

describe("Donut menu keyboard operation", () => {
  it("is reachable by keyboard: the launcher is a focusable button and items follow in order", async () => {
    await openDonutMenu()
    launcher().focus()
    expect(document.activeElement).toBe(launcher())

    const reached: (string | null)[] = []
    for (const _ of panelItemLabels()) {
      await userEvent.tab()
      reached.push(document.activeElement!.getAttribute("aria-label"))
    }
    expect(reached).toEqual(panelItemLabels())
  })

  it("goes to the next note when Space is pressed on Assimilate", async () => {
    mockSdkService(AssimilationController, "next", {
      nextUnit: { noteId: 7 },
    })
    await openDonutMenu()
    screen.getByLabelText("Assimilate").focus()

    await userEvent.keyboard(" ")
    await flushPromises()

    expect(router.currentRoute.value).toMatchObject(noteShowLocation(7))
    expectPanelClosed()
  })

  it("closes when Enter on Assimilate finds no more notes", async () => {
    const next = mockSdkService(AssimilationController, "next", {})
    await openDonutMenu()
    screen.getByLabelText("Assimilate").focus()

    await userEvent.keyboard("{Enter}")
    await flushPromises()

    expect(next).toHaveBeenCalledOnce()
    expect(router.currentRoute.value.name).toBe("root")
    expectPanelClosed()
  })
})
