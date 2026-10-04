import { useRecallData } from "@/composables/useRecallData"
import { fireEvent } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it, expect, vi } from "vitest"
import { memoryTrackerLitesStub } from "./mainMenuMocks"
import {
  createMatchMediaSpy,
  mountMainMenu,
  renderComponent,
  router,
  setupMainMenuTests,
} from "./mainMenuTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceMockExports } = await import("./mainMenuMocks")
  return aiReplyEventSourceMockExports()
})

setupMainMenuTests()

function resumeLink() {
  return document.querySelector('[aria-label="Resume"]')
}

function menuWrapper() {
  return document.querySelector(".menu-wrapper")
}

describe("MainMenu resume recall", () => {
  beforeEach(() => {
    const { setIsRecallPaused, setToRepeat } = useRecallData()
    setIsRecallPaused(true)
    setToRepeat(memoryTrackerLitesStub(5))
  })

  it("shows highlighted Resume before Note when recall is paused; hides when not", async () => {
    await renderComponent()

    const resumeRecallLink = resumeLink()
    expect(resumeRecallLink).not.toBeNull()
    expect(resumeRecallLink!.closest(".nav-item")).toHaveClass(
      "resume-recall-active"
    )

    const allNavItems = Array.from(document.querySelectorAll(".nav-item"))
    const resumeNavItem = allNavItems.find((el) =>
      el.querySelector('[aria-label="Resume"]')
    )
    const noteNavItem = allNavItems.find((el) =>
      el.querySelector('[aria-label="Note"]')
    )

    expect(allNavItems.indexOf(resumeNavItem!)).toBeLessThan(
      allNavItems.indexOf(noteNavItem!)
    )

    useRecallData().setIsRecallPaused(false)
    await flushPromises()
    expect(resumeLink()).toBeNull()
  })

  it("resumes recall from collapsed horizontal menu without expanding", async () => {
    createMatchMediaSpy(false)
    mountMainMenu()
    expect(menuWrapper()).toHaveClass("is-collapsed")

    await fireEvent.click(resumeLink()!)
    await flushPromises()

    expect(router.currentRoute.value.name).toBe("recall")
    expect(menuWrapper()).toHaveClass("is-collapsed")
  })

  it("does not show zero recall count badge on Resume", async () => {
    useRecallData().setToRepeat([])

    const { queryByText } = mountMainMenu()
    await flushPromises()

    expect(queryByText("0")).not.toBeInTheDocument()
  })

  it.each([
    {
      description: "currentIndex > 0 and not on recall page",
      routeName: "notebooks" as const,
      isRecallPaused: false,
      currentIndex: 1,
      toRepeat: memoryTrackerLitesStub(5),
      shouldShow: true,
    },
    {
      description: "currentIndex > 0 but on recall page",
      routeName: "recall" as const,
      isRecallPaused: false,
      currentIndex: 1,
      toRepeat: undefined,
      shouldShow: false,
    },
    {
      description: "currentIndex is 0 and not on recall page",
      routeName: "notebooks" as const,
      isRecallPaused: false,
      currentIndex: 0,
      toRepeat: undefined,
      shouldShow: false,
    },
    {
      description: "both isRecallPaused and currentIndex > 0",
      routeName: "notebooks" as const,
      isRecallPaused: true,
      currentIndex: 2,
      toRepeat: memoryTrackerLitesStub(5),
      shouldShow: true,
    },
    {
      description: "toRepeatCount is 0 even if recall is paused",
      routeName: "notebooks" as const,
      isRecallPaused: true,
      currentIndex: 0,
      toRepeat: [] as ReturnType<typeof memoryTrackerLitesStub>,
      shouldShow: false,
    },
    {
      description: "toRepeatCount is 0 even if currentIndex > 0",
      routeName: "notebooks" as const,
      isRecallPaused: false,
      currentIndex: 5,
      toRepeat: memoryTrackerLitesStub(5),
      shouldShow: false,
    },
  ])(
    "Resume visibility: $description → $shouldShow",
    async ({
      routeName,
      isRecallPaused,
      currentIndex,
      toRepeat,
      shouldShow,
    }) => {
      await router.push({ name: routeName })
      await flushPromises()

      const { setIsRecallPaused, setCurrentIndex, setToRepeat } =
        useRecallData()
      setIsRecallPaused(isRecallPaused)
      setCurrentIndex(currentIndex)
      setToRepeat(toRepeat)

      await renderComponent()

      expect(!!resumeLink()).toBe(shouldShow)
    }
  )
})
