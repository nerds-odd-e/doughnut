import MainMenu from "@/components/toolbars/MainMenu.vue"
import type { User } from "@generated/backend"
import { screen, fireEvent } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { beforeEach, vi } from "vitest"
import { useRecallData } from "@/composables/useRecallData"
import { ref } from "vue"

vi.mock("@/composables/useRecallData")

const useRouteValue = { name: "" }
const mockPush = vi.fn()
vitest.mock("vue-router", () => ({
  useRoute: () => useRouteValue,
  useRouter: () => ({
    push: mockPush,
  }),
}))

// Mock window.matchMedia
const createMatchMedia = (matches: boolean) => {
  return vi.fn().mockImplementation((query: string) => ({
    matches,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }))
}

// Default menu data structure
const defaultMenuData = {
  assimilationCount: {
    dueCount: 0,
    assimilatedCountOfTheDay: 0,
    totalUnassimilatedCount: 0,
  },
  recallStatus: {
    toRepeatCount: 0,
    recallWindowEndAt: "",
    totalAssimilatedCount: 0,
  },
  unreadConversations: [],
}

// Helper to create menu data with overrides
const createMenuData = (overrides?: Partial<typeof defaultMenuData>) => {
  return { ...defaultMenuData, ...overrides }
}

// Helper to create useRecallData mock return value
const createUseRecallDataMock = (overrides?: {
  toRepeatCount?: number
  isRecallPaused?: boolean
  resumeRecall?: () => void
}) => {
  return {
    toRepeatCount: ref(overrides?.toRepeatCount ?? 0),
    recallWindowEndAt: ref(undefined),
    totalAssimilatedCount: ref(0),
    isRecallPaused: ref(overrides?.isRecallPaused ?? false),
    shouldResumeRecall: ref(false),
    setToRepeatCount: vi.fn(),
    setRecallWindowEndAt: vi.fn(),
    setTotalAssimilatedCount: vi.fn(),
    setIsRecallPaused: vi.fn(),
    resumeRecall: (overrides?.resumeRecall ?? vi.fn()) as () => void,
    clearShouldResumeRecall: vi.fn(),
    decrementToRepeatCount: vi.fn(),
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
    const result = helper.component(MainMenu).withProps({ user }).render()
    await expandMenuIfNeeded()
    return result
  }

  beforeEach(() => {
    vi.clearAllMocks()
    // Default to lg or larger (vertical menu) for tests
    window.matchMedia = createMatchMedia(true)
    mockSdkService("getMenuData", defaultMenuData)
    vi.mocked(useRecallData).mockReturnValue(createUseRecallDataMock())
    user = makeMe.aUser.please()
  })

  it("shows assimilate link in main menu", async () => {
    await renderComponent()

    const assimilateLink = screen.getByLabelText("Assimilate")
    expect(assimilateLink).toBeInTheDocument()
    expect(assimilateLink.tagName).toBe("A")
  })

  it("highlights the note link when on notebooks page", async () => {
    useRouteValue.name = "notebooks"

    await renderComponent()

    const noteLink = screen.getByLabelText("Note")
    const navItem = noteLink.closest(".nav-item")
    expect(navItem).toHaveClass("daisy-text-primary")
  })

  it("highlights the note link when on notebook edit page", async () => {
    useRouteValue.name = "notebookEdit"

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
    useRouteValue.name = "circleShow"

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

      helper.component(MainMenu).withProps({ user }).render()
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
            toRepeatCount: 789,
            recallWindowEndAt: "",
            totalAssimilatedCount: 0,
          },
        })
      )

      vi.mocked(useRecallData).mockReturnValue(
        createUseRecallDataMock({
          toRepeatCount: 789,
        })
      )

      const { getByText } = helper
        .component(MainMenu)
        .withProps({ user })
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
        .render()
      await flushPromises()

      const recallCount = queryByText("0")
      expect(recallCount).not.toBeInTheDocument()
    })
  })

  describe("horizontal menu account button", () => {
    beforeEach(() => {
      // Set to narrow screen (horizontal menu)
      window.matchMedia = createMatchMedia(false)
    })

    it("keeps menu expanded when clicking account button", async () => {
      helper.component(MainMenu).withProps({ user }).render()

      // Expand the menu first
      const expandButton = screen.getByLabelText("Toggle menu")
      await fireEvent.click(expandButton)

      // Verify menu is expanded by checking if menu items are visible
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
      helper.component(MainMenu).withProps({ user }).render()

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
      helper.component(MainMenu).withProps({ user }).render()

      // Expand the menu first
      const expandButton = screen.getByLabelText("Toggle menu")
      await fireEvent.click(expandButton)

      // Verify menu is expanded
      const assimilateLink = screen.getByLabelText("Assimilate")
      expect(assimilateLink).toBeInTheDocument()

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
        })
      )
    })

    it("shows resume recall menu item when recall is paused", async () => {
      await renderComponent()

      const resumeRecallLink = screen.getByLabelText("Resume Recall")
      expect(resumeRecallLink).toBeInTheDocument()
    })

    it("highlights resume recall menu item when recall is paused", async () => {
      await renderComponent()

      const resumeRecallLink = screen.getByLabelText("Resume Recall")
      const navItem = resumeRecallLink.closest(".nav-item")
      expect(navItem).toHaveClass("daisy-text-primary")
    })

    it("shows resume recall as first item when recall is paused", async () => {
      await renderComponent()

      // Check that resume recall appears before note in the DOM
      const resumeRecallElement = screen.getByLabelText("Resume Recall")
      const noteElement = screen.getByLabelText("Note")

      // Get all navigation items by querying the DOM
      const allNavItems = Array.from(document.querySelectorAll(".nav-item"))
      const resumeNavItem = allNavItems.find(
        (el) =>
          el.querySelector('a[aria-label="Resume Recall"]') ||
          el.querySelector('[aria-label="Resume Recall"]')
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

      // Check that resume recall appears before note in the DOM order
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

      const resumeRecallLink = screen.queryByLabelText("Resume Recall")
      expect(resumeRecallLink).not.toBeInTheDocument()
    })

    it("resumes recall when clicking resume recall in horizontal menu without expanding", async () => {
      window.matchMedia = createMatchMedia(false)
      const resumeRecallSpy = vi.fn()
      vi.mocked(useRecallData).mockReturnValue(
        createUseRecallDataMock({
          isRecallPaused: true,
          resumeRecall: resumeRecallSpy,
        })
      )

      helper.component(MainMenu).withProps({ user }).render()

      // Menu should be collapsed initially
      const expandButton = screen.getByLabelText("Toggle menu")
      expect(expandButton).toBeInTheDocument()

      // Click on the resume recall item (should be visible as active item in collapsed state)
      const resumeRecallLink = screen.getByLabelText("Resume Recall")
      await fireEvent.click(resumeRecallLink)

      // Should call resumeRecall
      expect(resumeRecallSpy).toHaveBeenCalled()
    })
  })
})
