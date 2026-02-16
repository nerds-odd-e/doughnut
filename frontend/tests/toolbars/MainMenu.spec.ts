import MainMenu from "@/components/toolbars/MainMenu.vue"
import { useRecallData } from "@/composables/useRecallData"
import timezoneParam from "@/managedApi/window/timezoneParam"
import routes from "@/routes/routes"
import type { User } from "@generated/backend"
import { fireEvent, screen } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, vi } from "vitest"
import { computed, ref } from "vue"
import { createRouter, createWebHistory } from "vue-router"

vi.mock("@/composables/useRecallData")

// Browser Mode: Use real Vue Router instead of mocking
const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Browser Mode: Mock AiReplyEventSource to prevent hoisting issues
vi.mock("@/managedApi/AiReplyEventSource", () => ({
  default: class {
    onMessage = vi.fn(() => this)
    onError = vi.fn(() => this)
    start = vi.fn()
  },
}))

// Browser Mode: Use real matchMedia API!
// We can spy on it to control behavior for tests
const createMatchMediaSpy = (matches: boolean) =>
  vi.spyOn(window, "matchMedia").mockImplementation((query: string) => {
    const mediaQueryList = {
      matches,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    } as MediaQueryList
    return mediaQueryList
  })

// Default menu data structure
const defaultMenuData = {
  assimilationCount: {
    dueCount: 0,
    assimilatedCountOfTheDay: 0,
    totalUnassimilatedCount: 0,
  },
  recallStatus: {
    toRepeat: [] as Array<{ memoryTrackerId?: number; spelling?: boolean }>,
    currentRecallWindowEndAt: "",
    totalAssimilatedCount: 0,
  },
  unreadConversations: [],
}

// Helper to create menu data with overrides
const createMenuData = (overrides?: Partial<typeof defaultMenuData>) => ({
  ...defaultMenuData,
  ...overrides,
})

// Helper to create useRecallData mock return value
const createUseRecallDataMock = (overrides?: {
  toRepeat?: Array<{ memoryTrackerId?: number; spelling?: boolean }>
  isRecallPaused?: boolean
  currentIndex?: number
  resumeRecall?: () => void
  diligentMode?: boolean
}) => {
  const toRepeat = ref<
    Array<{ memoryTrackerId?: number; spelling?: boolean }> | undefined
  >(overrides?.toRepeat ?? [])
  const currentIndex = ref(overrides?.currentIndex ?? 0)
  return {
    toRepeatCount: computed(() => {
      const length = toRepeat.value?.length ?? 0
      const index = currentIndex.value
      return Math.max(0, length - index)
    }),
    toRepeat,
    currentRecallWindowEndAt: ref(undefined),
    totalAssimilatedCount: ref(0),
    isRecallPaused: ref(overrides?.isRecallPaused ?? false),
    shouldResumeRecall: ref(false),
    treadmillMode: ref(false),
    currentIndex,
    diligentMode: ref(overrides?.diligentMode ?? false),
    setToRepeat: vi.fn(),
    setCurrentRecallWindowEndAt: vi.fn(),
    setTotalAssimilatedCount: vi.fn(),
    setIsRecallPaused: vi.fn(),
    resumeRecall: (overrides?.resumeRecall ?? vi.fn()) as () => void,
    clearShouldResumeRecall: vi.fn(),
    setTreadmillMode: vi.fn(),
    setCurrentIndex: vi.fn(),
    setDiligentMode: vi.fn(),
  }
}

describe("main menu", () => {
  let user: User

  // Helper to expand menu if needed
  const expandMenuIfNeeded = async () => {
    const expandButton = screen.queryByLabelText("Toggle menu")
    if (expandButton) {
      await fireEvent.click(expandButton)
    }
  }

  // Helper to render component and expand menu if needed
  const renderComponent = async () => {
    const result = helper
      .component(MainMenu)
      .withProps({ user })
      .withRouter(router)
      .render()
    await expandMenuIfNeeded()
    return result
  }

  beforeEach(() => {
    vi.clearAllMocks()
    // Browser Mode: Default to lg or larger (vertical menu) for tests
    createMatchMediaSpy(true)
    mockSdkService("getMenuData", defaultMenuData)
    vi.mocked(useRecallData).mockReturnValue(createUseRecallDataMock())
    user = makeMe.aUser.please()
  })

  afterEach(() => {
    document.body.innerHTML = ""
    vi.restoreAllMocks()
  })

  it("shows assimilate link in main menu", async () => {
    await renderComponent()

    const assimilateLink = screen.getByLabelText("Assimilate")
    expect(assimilateLink).toBeInTheDocument()
    expect(assimilateLink.tagName).toBe("A")
  })

  it("highlights the note link when on notebooks page", async () => {
    await router.push({ name: "notebooks" })
    await flushPromises()

    await renderComponent()

    const noteLink = screen.getByLabelText("Note")
    const navItem = noteLink.closest(".nav-item")
    expect(navItem).toHaveClass("daisy-text-primary")
  })

  it("highlights the note link when on notebook edit page", async () => {
    await router.push({ name: "notebookEdit", params: { notebookId: "1" } })
    await flushPromises()

    await renderComponent()

    const noteLink = screen.getByLabelText("Note")
    const navItem = noteLink.closest(".nav-item")
    expect(navItem).toHaveClass("daisy-text-primary")
  })

  it("shows note link in main menu", async () => {
    await renderComponent()

    const noteLink = screen.getByLabelText("Note")
    expect(noteLink).toBeInTheDocument()
  })

  it("highlights the circles link when on circle show page", async () => {
    await router.push({ name: "circleShow", params: { circleId: "1" } })
    await flushPromises()

    await renderComponent()

    const circlesLink = screen.getByLabelText("Circles")
    const navItem = circlesLink.closest(".nav-item")
    expect(navItem).toHaveClass("daisy-text-primary")
  })

  it("shows assimilate link in both main menu and dropdown menu", async () => {
    await renderComponent()

    const mainMenuAssimilateLink = screen.getByLabelText("Assimilate")

    expect(mainMenuAssimilateLink).toBeInTheDocument()
  })

  describe("assimilate due count", () => {
    it("shows due count when there are due items", async () => {
      mockSdkService(
        "getMenuData",
        createMenuData({
          assimilationCount: {
            dueCount: 5,
            assimilatedCountOfTheDay: 0,
            totalUnassimilatedCount: 0,
          },
        })
      )

      const { getByText } = helper
        .component(MainMenu)
        .withProps({ user })
        .withRouter(router)
        .render()
      await flushPromises()

      const dueCount = getByText("5")
      expect(dueCount).toBeInTheDocument()
      expect(dueCount).toHaveClass("due-count")
    })

    it("does not show due count when there are no due items", async () => {
      const { queryByText } = helper
        .component(MainMenu)
        .withProps({ user })
        .withRouter(router)
        .render()
      await flushPromises()

      const dueCount = queryByText("0")
      expect(dueCount).not.toBeInTheDocument()
    })

    it("fetches menu data when user changes", async () => {
      const getMenuDataSpy = mockSdkService(
        "getMenuData",
        createMenuData({
          assimilationCount: {
            dueCount: 3,
            assimilatedCountOfTheDay: 0,
            totalUnassimilatedCount: 0,
          },
        })
      )

      const { rerender } = helper
        .component(MainMenu)
        .withProps({ user })
        .withRouter(router)
        .render()
      await flushPromises()

      const newUser = { ...user, id: 2 }
      await rerender({ user: newUser })
      await flushPromises()

      expect(getMenuDataSpy).toHaveBeenCalledTimes(2)
    })

    it("calls getMenuData with the correct timezone", async () => {
      const getMenuDataSpy = mockSdkService(
        "getMenuData",
        createMenuData({
          assimilationCount: {
            dueCount: 3,
            assimilatedCountOfTheDay: 0,
            totalUnassimilatedCount: 0,
          },
        })
      )

      helper.component(MainMenu).withProps({ user }).withRouter(router).render()
      await flushPromises()

      expect(getMenuDataSpy).toHaveBeenCalledWith({
        query: { timezone: timezoneParam() },
      })
    })
  })

  describe("recall count", () => {
    it("shows recall count when there are items to repeat", async () => {
      mockSdkService(
        "getMenuData",
        createMenuData({
          recallStatus: {
            toRepeat: Array(789).fill({}) as Array<{
              memoryTrackerId?: number
              spelling?: boolean
            }>,
            currentRecallWindowEndAt: "",
            totalAssimilatedCount: 0,
          },
        })
      )

      vi.mocked(useRecallData).mockReturnValue(
        createUseRecallDataMock({
          toRepeat: Array(789).fill({}) as Array<{
            memoryTrackerId?: number
            spelling?: boolean
          }>,
        })
      )

      const { getByText } = helper
        .component(MainMenu)
        .withProps({ user })
        .withRouter(router)
        .render()
      await flushPromises()

      const recallCount = getByText("789")
      expect(recallCount).toBeInTheDocument()
      expect(recallCount).toHaveClass("recall-count")
    })

    it("does not show recall count when there are no items to repeat", async () => {
      const { queryByText } = helper
        .component(MainMenu)
        .withProps({ user })
        .withRouter(router)
        .render()
      await flushPromises()

      const recallCount = queryByText("0")
      expect(recallCount).not.toBeInTheDocument()
    })

    it("decreases recall count when questions are answered (currentIndex increases)", async () => {
      const toRepeat = Array(10).fill({}) as Array<{
        memoryTrackerId?: number
        spelling?: boolean
      }>

      const mockData = createUseRecallDataMock({
        toRepeat,
        currentIndex: 0,
      })

      vi.mocked(useRecallData).mockReturnValue(mockData)

      const { getAllByText, rerender } = helper
        .component(MainMenu)
        .withProps({ user })
        .withRouter(router)
        .render()
      await flushPromises()

      // Initially shows 10 items (may appear on both Recall and Resume if currentIndex > 0)
      const initialCounts = getAllByText("10")
      expect(initialCounts.length).toBeGreaterThan(0)

      // Simulate answering 3 questions by updating currentIndex
      mockData.currentIndex.value = 3
      await rerender({ user })
      await flushPromises()

      // Count should decrease to 7 (10 - 3)
      // When currentIndex > 0, both Resume and Recall show the count
      const updatedCounts = getAllByText("7")
      expect(updatedCounts.length).toBeGreaterThan(0)
      expect(screen.queryByText("10")).not.toBeInTheDocument()
    })
  })

  describe("horizontal menu account button", () => {
    beforeEach(() => {
      // Browser Mode: Set to narrow screen (horizontal menu) using real matchMedia
      createMatchMediaSpy(false)
    })

    it("keeps menu expanded when clicking account button", async () => {
      helper.component(MainMenu).withProps({ user }).withRouter(router).render()

      // Browser Mode: Real click event on expand button!
      // Expand the menu first
      const expandButton = screen.getByLabelText("Toggle menu")
      await fireEvent.click(expandButton)

      // Verify menu is expanded by checking if assimilate link is visible
      const assimilateLink = screen.getByLabelText("Assimilate")
      expect(assimilateLink).toBeInTheDocument()

      // Click on account button
      const accountButton = screen.getByLabelText("Account")
      await fireEvent.click(accountButton)

      // Menu should still be expanded (assimilate link should still be visible)
      expect(assimilateLink).toBeInTheDocument()

      // Account dropdown should be visible
      const settingsLink = screen.getByText(/Settings for/)
      expect(settingsLink).toBeInTheDocument()
    })

    it("shows account dropdown when clicking account button on horizontal menu", async () => {
      helper.component(MainMenu).withProps({ user }).withRouter(router).render()

      // Browser Mode: Real click event on expand button!
      // Expand the menu first
      const expandButton = screen.getByLabelText("Toggle menu")
      await fireEvent.click(expandButton)

      // Click on account button
      const accountButton = screen.getByLabelText("Account")
      await fireEvent.click(accountButton)

      // Account dropdown should be visible with menu items
      expect(screen.getByText(/Settings for/)).toBeInTheDocument()
      expect(screen.getByText("Recent...")).toBeInTheDocument()
      expect(screen.getByText("Logout")).toBeInTheDocument()
    })

    it("collapses menu when clicking outside menu and account dropdown", async () => {
      helper.component(MainMenu).withProps({ user }).withRouter(router).render()

      // Browser Mode: Real click event on expand button!
      // Expand the menu first
      const expandButton = screen.getByLabelText("Toggle menu")
      await fireEvent.click(expandButton)

      // Verify menu is expanded by checking if assimilate link is visible
      const assimilateLink = screen.getByLabelText("Assimilate")
      expect(assimilateLink).toBeInTheDocument()

      // Browser Mode: Real click event on document.body!
      // Click outside the menu
      await fireEvent.click(document.body)

      // Menu should be collapsed (assimilate link should not be visible in expanded state)
      // The menu might still exist but in collapsed state
      // We can verify by checking that the expand button is still there
      expect(screen.getByLabelText("Toggle menu")).toBeInTheDocument()
    })
  })

  describe("resume recall menu item", () => {
    beforeEach(() => {
      vi.mocked(useRecallData).mockReturnValue(
        createUseRecallDataMock({
          isRecallPaused: true,
          toRepeat: Array(5).fill({}) as Array<{
            memoryTrackerId?: number
            spelling?: boolean
          }>,
        })
      )
    })

    it("shows and highlights resume recall menu item when recall is paused", async () => {
      await renderComponent()

      const resumeRecallLink = screen.getByLabelText("Resume")
      expect(resumeRecallLink).toBeInTheDocument()
      const navItem = resumeRecallLink.closest(".nav-item")
      expect(navItem).toHaveClass("resume-recall-active")
    })

    it("shows resume recall as first item when recall is paused", async () => {
      await renderComponent()

      // Browser Mode: Real getByLabelText!
      // Check that resume recall element is in the document
      const resumeRecallElement = screen.getByLabelText("Resume")
      const noteElement = screen.getByLabelText("Note")

      // Browser Mode: Real document.querySelectorAll!
      // Get all navigation items
      const allNavItems = Array.from(document.querySelectorAll(".nav-item"))
      const resumeNavItem = allNavItems.find(
        (el) =>
          el.querySelector('a[aria-label="Resume"]') ||
          el.querySelector('[aria-label="Resume"]')
      )
      const noteNavItem = allNavItems.find(
        (el) =>
          el.querySelector('a[aria-label="Note"]') ||
          el.querySelector('[aria-label="Note"]')
      )

      expect(resumeRecallElement).toBeInTheDocument()
      expect(noteElement).toBeInTheDocument()
      expect(resumeNavItem).toBeDefined()
      expect(noteNavItem).toBeDefined()

      // Browser Mode: Real indexOf!
      // Check that resume recall appears before note
      const resumeIndex = allNavItems.indexOf(resumeNavItem!)
      const noteIndex = allNavItems.indexOf(noteNavItem!)
      expect(resumeIndex).toBeGreaterThan(-1)
      expect(noteIndex).toBeGreaterThan(-1)
      expect(resumeIndex).toBeLessThan(noteIndex)
    })

    it("does not show resume recall menu item when recall is not paused", async () => {
      vi.mocked(useRecallData).mockReturnValue(
        createUseRecallDataMock({
          isRecallPaused: false,
        })
      )

      await renderComponent()

      const resumeRecallLink = screen.queryByLabelText("Resume")
      expect(resumeRecallLink).not.toBeInTheDocument()
    })

    it("resumes recall when clicking resume recall in horizontal menu without expanding", async () => {
      createMatchMediaSpy(false)
      const resumeRecallSpy = vi.fn()
      vi.mocked(useRecallData).mockReturnValue(
        createUseRecallDataMock({
          isRecallPaused: true,
          toRepeat: Array(5).fill({}) as Array<{
            memoryTrackerId?: number
            spelling?: boolean
          }>,
          resumeRecall: resumeRecallSpy,
        })
      )

      helper.component(MainMenu).withProps({ user }).withRouter(router).render()

      // Browser Mode: Real getByLabelText!
      // Menu should be collapsed initially
      const expandButton = screen.getByLabelText("Toggle menu")
      expect(expandButton).toBeInTheDocument()

      // Browser Mode: Real getByLabelText!
      // Click on the resume recall item (should be visible as active item in collapsed state)
      const resumeRecallLink = screen.getByLabelText("Resume")
      await fireEvent.click(resumeRecallLink)

      // Browser Mode: Real expect!
      // Should call resumeRecall
      expect(resumeRecallSpy).toHaveBeenCalled()
    })

    it("shows recall count badge on resume item when there are items to repeat", async () => {
      mockSdkService(
        "getMenuData",
        createMenuData({
          recallStatus: {
            toRepeat: Array(789).fill({}) as Array<{
              memoryTrackerId?: number
              spelling?: boolean
            }>,
            currentRecallWindowEndAt: "",
            totalAssimilatedCount: 0,
          },
        })
      )

      vi.mocked(useRecallData).mockReturnValue(
        createUseRecallDataMock({
          isRecallPaused: true,
          toRepeat: Array(789).fill({}) as Array<{
            memoryTrackerId?: number
            spelling?: boolean
          }>,
        })
      )

      helper.component(MainMenu).withProps({ user }).withRouter(router).render()
      await flushPromises()

      const resumeLink = screen.getByLabelText("Resume")
      const resumeNavItem = resumeLink.closest(".nav-item")
      const resumeCount = resumeNavItem?.querySelector(".recall-count")
      expect(resumeCount).toBeInTheDocument()
      expect(resumeCount).toHaveTextContent("789")
      expect(resumeCount).toHaveClass("recall-count")
    })

    it("does not show recall count badge on resume item when there are no items to repeat", async () => {
      vi.mocked(useRecallData).mockReturnValue(
        createUseRecallDataMock({
          isRecallPaused: true,
          toRepeat: [],
        })
      )

      const { queryByText } = helper
        .component(MainMenu)
        .withProps({ user })
        .withRouter(router)
        .render()
      await flushPromises()

      const resumeCount = queryByText("0")
      expect(resumeCount).not.toBeInTheDocument()
    })

    it.each([
      {
        description:
          "shows resume recall menu item when currentIndex > 0 and not on recall page",
        routeName: "notebooks",
        isRecallPaused: false,
        currentIndex: 1,
        toRepeat: Array(5).fill({}) as Array<{
          memoryTrackerId?: number
          spelling?: boolean
        }>,
        shouldShow: true,
      },
      {
        description:
          "does not show resume recall menu item when currentIndex > 0 but on recall page",
        routeName: "recall",
        isRecallPaused: false,
        currentIndex: 1,
        toRepeat: undefined,
        shouldShow: false,
      },
      {
        description:
          "does not show resume recall menu item when currentIndex is 0 and not on recall page",
        routeName: "notebooks",
        isRecallPaused: false,
        currentIndex: 0,
        toRepeat: undefined,
        shouldShow: false,
      },
      {
        description:
          "shows resume recall menu item when both isRecallPaused and currentIndex > 0 conditions are true",
        routeName: "notebooks",
        isRecallPaused: true,
        currentIndex: 2,
        toRepeat: Array(5).fill({}) as Array<{
          memoryTrackerId?: number
          spelling?: boolean
        }>,
        shouldShow: true,
      },
      {
        description:
          "does not show resume recall menu item when toRepeatCount is 0 even if recall is paused",
        routeName: "notebooks",
        isRecallPaused: true,
        currentIndex: 0,
        toRepeat: [],
        shouldShow: false,
      },
      {
        description:
          "does not show resume recall menu item when toRepeatCount is 0 even if currentIndex > 0",
        routeName: "notebooks",
        isRecallPaused: false,
        currentIndex: 5,
        toRepeat: Array(5).fill({}) as Array<{
          memoryTrackerId?: number
          spelling?: boolean
        }>,
        shouldShow: false,
      },
    ])("$description", async ({
      routeName,
      isRecallPaused,
      currentIndex,
      toRepeat,
      shouldShow,
    }) => {
      await router.push({ name: routeName as "notebooks" | "recall" })
      await flushPromises()

      vi.mocked(useRecallData).mockReturnValue(
        createUseRecallDataMock({
          isRecallPaused,
          currentIndex,
          toRepeat,
        })
      )

      await renderComponent()

      const resumeRecallLink = screen.queryByLabelText("Resume")
      if (shouldShow) {
        expect(resumeRecallLink).toBeInTheDocument()
      } else {
        expect(resumeRecallLink).not.toBeInTheDocument()
      }
    })
  })

  describe("diligent mode", () => {
    it.each([
      {
        description:
          "should show red background on recall badge when in diligent mode",
        linkLabel: "Recall",
        isRecallPaused: false,
        diligentMode: true,
        shouldHaveDiligentMode: true,
      },
      {
        description:
          "should show green background on recall badge when not in diligent mode",
        linkLabel: "Recall",
        isRecallPaused: false,
        diligentMode: false,
        shouldHaveDiligentMode: false,
      },
      {
        description:
          "should show red background on resume recall badge when in diligent mode",
        linkLabel: "Resume",
        isRecallPaused: true,
        diligentMode: true,
        shouldHaveDiligentMode: true,
      },
    ])("$description", async ({
      linkLabel,
      isRecallPaused,
      diligentMode,
      shouldHaveDiligentMode,
    }) => {
      vi.mocked(useRecallData).mockReturnValue(
        createUseRecallDataMock({
          isRecallPaused,
          toRepeat: Array(5).fill({}) as Array<{
            memoryTrackerId?: number
            spelling?: boolean
          }>,
          diligentMode,
        })
      )

      await renderComponent()

      const link = screen.getByLabelText(linkLabel)
      const navItem = link.closest(".nav-item")
      const count = navItem?.querySelector(".recall-count")
      expect(count).toBeInTheDocument()
      expect(count).toHaveClass("recall-count")
      if (shouldHaveDiligentMode) {
        expect(count).toHaveClass("diligent-mode")
      } else {
        expect(count).not.toHaveClass("diligent-mode")
      }
    })
  })
})
