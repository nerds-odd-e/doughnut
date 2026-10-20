import { useRecallData } from "@/composables/useRecallData"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import { userEvent } from "vitest/browser"
import {
  donutMenuPanel,
  expectPanelClosed,
  mountDonutMenu,
  pressLauncher,
  router,
  setupDonutMenuTests,
} from "./donutMenuTestSupport"
import { memoryTrackerLitesStub } from "./menuDataMocks"

setupDonutMenuTests()

describe("Collapsed current activity", () => {
  it.each([
    { route: "notebooks", label: "Note" },
    { route: "recall", label: "Recall" },
    { route: "circles", label: "Circles" },
    { route: "bazaar", label: "Bazaar" },
    { route: "messageCenter", label: "Messages" },
  ])("identifies $label and opens navigation", async ({ route, label }) => {
    await router.push({ name: route })
    mountDonutMenu()
    await flushPromises()

    const activity = screen.getByLabelText(label)
    expect(activity.tagName).toBe("BUTTON")
    expect(activity.querySelector("svg")).toBeInTheDocument()
    await userEvent.click(activity)
    await flushPromises()

    expect(donutMenuPanel()).toBeInTheDocument()
    expect(activity).toHaveAttribute("aria-expanded", "true")
    expect(router.currentRoute.value.name).toBe(route)
  })

  it("opens navigation through the fallback on a page with no active item", async () => {
    mountDonutMenu()
    await flushPromises()
    await pressLauncher()
    expect(donutMenuPanel()).toBeInTheDocument()
  })

  it("resumes directly with its count and diligent styling, while the menu opens independently", async () => {
    await router.push({ name: "notebooks" })
    const {
      setToRepeat,
      setCurrentIndex,
      setDiligentMode,
      shouldResumeRecall,
    } = useRecallData()
    setToRepeat(memoryTrackerLitesStub(5))
    setCurrentIndex(1)
    setDiligentMode(true)
    mountDonutMenu()
    await flushPromises()

    const resume = screen.getByLabelText("Resume")
    expect(resume.closest(".nav-item")).toHaveClass("resume-recall-active")
    expect(resume.querySelector(".recall-count")).toHaveTextContent("4")
    expect(resume.querySelector(".recall-count")).toHaveClass("diligent-mode")
    await pressLauncher()
    expect(donutMenuPanel()).toBeInTheDocument()
    await pressLauncher()
    await fireEvent.click(resume)
    await flushPromises()

    expect(shouldResumeRecall.value).toBe(true)
    expect(router.currentRoute.value.name).toBe("recall")
    expectPanelClosed()
  })
})
