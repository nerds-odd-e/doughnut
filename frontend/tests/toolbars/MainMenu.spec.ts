import { UserController } from "@generated/doughnut-backend-api/sdk.gen"
import MainMenu from "@/components/toolbars/MainMenu.vue"
import { useRecallData } from "@/composables/useRecallData"
import timezoneParam from "@/managedApi/window/timezoneParam"
import routes from "@/routes/routes"
import type { MemoryTrackerLite, User } from "@generated/doughnut-backend-api"
import { fireEvent, screen } from "@testing-library/vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, vi } from "vitest"
import { computed, ref } from "vue"
import {
  createRouter,
  createWebHistory,
  type RouteLocationRaw,
} from "vue-router"

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
const memoryTrackerLitesStub = (n: number): MemoryTrackerLite[] =>
  Array.from({ length: n }, (_, i) => ({
    memoryTrackerId: i + 1,
    spelling: false,
  }))

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
    toRepeat: [] as MemoryTrackerLite[],
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
  toRepeat?: MemoryTrackerLite[]
  isRecallPaused?: boolean
  currentIndex?: number
  resumeRecall?: () => void
  diligentMode?: boolean
}) => {
  const toRepeat = ref<MemoryTrackerLite[] | undefined>(
    overrides?.toRepeat ?? []
  )
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
    dueRecallsRefreshNonce: ref(0),
    requestDueRecallsRefresh: vi.fn(),
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

  const mountMainMenu = () =>
    helper.component(MainMenu).withProps({ user }).withRouter(router).render()

  // Helper to render component and expand menu if needed
  const renderComponent = async () => {
    const result = mountMainMenu()
    await expandMenuIfNeeded()
    return result
  }

  const expectNavLinkPrimary = (ariaLabel: string) => {
    const link = screen.getByLabelText(ariaLabel)
    expect(link.closest(".nav-item")).toHaveClass("text-primary")
  }

  beforeEach(() => {
    vi.clearAllMocks()
    // Browser Mode: Default to lg or larger (vertical menu) for tests
    createMatchMediaSpy(true)
    mockSdkService(UserController, "getMenuData", defaultMenuData)
    vi.mocked(useRecallData).mockReturnValue(createUseRecallDataMock())
    user = makeMe.aUser.please()
  })

  afterEach(() => {
    document.body.innerHTML = ""
    vi.restoreAllMocks()
  })

  it("shows assimilate link as an anchor in the menu", async () => {
    await renderComponent()
    const assimilateLink = screen.getByLabelText("Assimilate")
    expect(assimilateLink).toBeInTheDocument()
    expect(assimilateLink.tagName).toBe("A")
  })

  it.each([
    {
      linkLabel: "Note",
      route: { name: "notebooks" } as RouteLocationRaw,
      context: "notebooks",
    },
    {
      linkLabel: "Note",
      route: {
        name: "notebookPage",
        params: { notebookId: "1" },
      } as RouteLocationRaw,
      context: "notebook page",
    },
    {
      linkLabel: "Note",
      route: {
        name: "folderPage",
        params: { notebookId: "1", folderId: "2" },
      } as RouteLocationRaw,
      context: "folder page",
    },
    {
      linkLabel: "Circles",
      route: {
        name: "circleShow",
        params: { circleId: "1" },
      } as RouteLocationRaw,
      context: "circle show page",
    },
  ])("applies primary nav styling to $linkLabel on $context", async ({
    linkLabel,
    route,
  }) => {
    await router.push(route)
    await flushPromises()
    await renderComponent()
    expectNavLinkPrimary(linkLabel)
  })

  describe("assimilate due count", () => {
    it("shows due count when there are due items", async () => {
      mockSdkService(
        UserController,
        "getMenuData",
        createMenuData({
          assimilationCount: {
            dueCount: 5,
            assimilatedCountOfTheDay: 0,
            totalUnassimilatedCount: 0,
          },
        })
      )

      const { getByText } = mountMainMenu()
      await flushPromises()

      const dueCount = getByText("5")
      expect(dueCount).toBeInTheDocument()
      expect(dueCount).toHaveClass("due-count")
    })

    it("does not show due count when there are no due items", async () => {
      const { queryByText } = mountMainMenu()
      await flushPromises()

      const dueCount = queryByText("0")
      expect(dueCount).not.toBeInTheDocument()
    })

    it("calls getMenuData with timezone and refetches when user changes", async () => {
      const getMenuDataSpy = mockSdkService(
        UserController,
        "getMenuData",
        createMenuData({
          assimilationCount: {
            dueCount: 3,
            assimilatedCountOfTheDay: 0,
            totalUnassimilatedCount: 0,
          },
        })
      )

      const { rerender } = mountMainMenu()
      await flushPromises()

      expect(getMenuDataSpy).toHaveBeenCalledWith({
        query: { timezone: timezoneParam() },
      })

      await rerender({ user: { ...user, id: 2 } })
      await flushPromises()

      expect(getMenuDataSpy).toHaveBeenCalledTimes(2)
    })
  })

  describe("recall count", () => {
    it.each([
      { linkLabel: "Recall", isRecallPaused: false },
      { linkLabel: "Resume", isRecallPaused: true },
    ])("shows recall count on $linkLabel when there are items to repeat", async ({
      linkLabel,
      isRecallPaused,
    }) => {
      mockSdkService(
        UserController,
        "getMenuData",
        createMenuData({
          recallStatus: {
            toRepeat: memoryTrackerLitesStub(789),
            currentRecallWindowEndAt: "",
            totalAssimilatedCount: 0,
          },
        })
      )

      vi.mocked(useRecallData).mockReturnValue(
        createUseRecallDataMock({
          isRecallPaused,
          toRepeat: memoryTrackerLitesStub(789),
        })
      )

      mountMainMenu()
      await flushPromises()

      const link = screen.getByLabelText(linkLabel)
      const recallCount = link
        .closest(".nav-item")
        ?.querySelector(".recall-count")
      expect(recallCount).toBeInTheDocument()
      expect(recallCount).toHaveTextContent("789")
      expect(recallCount).toHaveClass("recall-count")
    })

    it("does not show recall count when there are no items to repeat", async () => {
      const { queryByText } = mountMainMenu()
      await flushPromises()

      const recallCount = queryByText("0")
      expect(recallCount).not.toBeInTheDocument()
    })

    it("decreases recall count when questions are answered (currentIndex increases)", async () => {
      const toRepeat = memoryTrackerLitesStub(10)

      const mockData = createUseRecallDataMock({
        toRepeat,
        currentIndex: 0,
      })

      vi.mocked(useRecallData).mockReturnValue(mockData)

      const { getAllByText, rerender } = mountMainMenu()
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

    it("collapses menu when clicking outside menu and account dropdown", async () => {
      mountMainMenu()

      const expandButton = screen.getByLabelText("Toggle menu")
      await fireEvent.click(expandButton)

      const assimilateLink = screen.getByLabelText("Assimilate")
      expect(assimilateLink).toBeInTheDocument()

      await fireEvent.click(document.body)

      expect(screen.getByLabelText("Toggle menu")).toBeInTheDocument()
    })
  })

  describe("resume recall menu item", () => {
    beforeEach(() => {
      vi.mocked(useRecallData).mockReturnValue(
        createUseRecallDataMock({
          isRecallPaused: true,
          toRepeat: memoryTrackerLitesStub(5),
        })
      )
    })

    it("shows highlighted Resume before Note when recall is paused", async () => {
      await renderComponent()

      const resumeRecallLink = screen.getByLabelText("Resume")
      expect(resumeRecallLink.closest(".nav-item")).toHaveClass(
        "resume-recall-active"
      )

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

      expect(resumeNavItem).toBeDefined()
      expect(noteNavItem).toBeDefined()
      expect(allNavItems.indexOf(resumeNavItem!)).toBeLessThan(
        allNavItems.indexOf(noteNavItem!)
      )
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
          toRepeat: memoryTrackerLitesStub(5),
          resumeRecall: resumeRecallSpy,
        })
      )

      mountMainMenu()

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

    it("does not show recall count badge on resume item when there are no items to repeat", async () => {
      vi.mocked(useRecallData).mockReturnValue(
        createUseRecallDataMock({
          isRecallPaused: true,
          toRepeat: [],
        })
      )

      const { queryByText } = mountMainMenu()
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
        toRepeat: memoryTrackerLitesStub(5),
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
        toRepeat: memoryTrackerLitesStub(5),
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
        toRepeat: memoryTrackerLitesStub(5),
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
          toRepeat: memoryTrackerLitesStub(5),
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
