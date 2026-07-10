import { describe, it, expect, vi } from "vitest"
import { nextTick } from "vue"
import {
  allNavLabels,
  ariaLabelEl,
  clickAriaLabel,
  clickToggleMenu,
  expectAllNavLabelsVisible,
  expectMenuCollapsed,
  expectMenuExpanded,
  menuContentEl,
  menuWrapperEl,
  renderHorizontalMenu,
  setupHorizontalMenuTests,
  useRouteValue,
} from "@tests/toolbars/horizontalMenuTestSupport"

describe("HorizontalMenu", () => {
  setupHorizontalMenuTests()

  describe("initial state", () => {
    it("starts collapsed with toggle button visible", async () => {
      renderHorizontalMenu({ activeItemName: "assimilate" })
      await nextTick()

      expectMenuCollapsed()
      expect(ariaLabelEl("Toggle menu")).toBeTruthy()
    })
  })

  describe("collapsed display", () => {
    it("shows active item label when an item is active", () => {
      renderHorizontalMenu({ activeItemName: "assimilate" })
      expect(ariaLabelEl("Assimilate")).toBeTruthy()
      expect(ariaLabelEl("Menu")).toBeNull()
    })

    it("shows menu icon when collapsed with no active item", () => {
      renderHorizontalMenu()
      expect(ariaLabelEl("Menu")).toBeTruthy()
    })

    it("hides menu icon on home page", () => {
      renderHorizontalMenu({ isHomePage: true })
      expect(ariaLabelEl("Menu")).toBeNull()
    })
  })

  describe("expand/collapse behavior", () => {
    it("toggles expanded state via expand button and shows all nav items", async () => {
      renderHorizontalMenu({ activeItemName: "assimilate" })
      expectMenuCollapsed()

      await clickToggleMenu()
      expectMenuExpanded()
      expectAllNavLabelsVisible()

      await clickToggleMenu()
      expectMenuCollapsed()
    })

    it("hides menu icon after expanding from collapsed menu-icon state", async () => {
      renderHorizontalMenu()
      expect(ariaLabelEl("Menu")).toBeTruthy()

      await clickToggleMenu()
      expect(ariaLabelEl("Menu")).toBeNull()
    })

    it("expands when clicking menu icon while collapsed", async () => {
      renderHorizontalMenu()
      expectMenuCollapsed()
      await clickAriaLabel("Menu")
      expectMenuExpanded()
    })

    it("expands when clicking active item while collapsed", async () => {
      renderHorizontalMenu({ activeItemName: "notebooks" })
      expectMenuCollapsed()
      ;(menuWrapperEl() as HTMLElement).click()
      await nextTick()
      expectMenuExpanded()
    })

    it("expands when clicking menu content while collapsed", async () => {
      renderHorizontalMenu({ activeItemName: "assimilate" })
      expectMenuCollapsed()
      ;(menuContentEl() as HTMLElement).click()
      await nextTick()
      expectMenuExpanded()
    })
  })

  describe("auto-collapse", () => {
    it("collapses when clicking outside", async () => {
      renderHorizontalMenu({ activeItemName: "assimilate" })
      await clickToggleMenu()
      expectMenuExpanded()

      document.body.click()
      await nextTick()
      expectMenuCollapsed()
    })

    it("collapses when losing focus", async () => {
      renderHorizontalMenu({ activeItemName: "assimilate" })
      await clickToggleMenu()
      expectMenuExpanded()

      const menuWrapper = menuWrapperEl()
      expect(menuWrapper).toBeTruthy()

      vi.useFakeTimers()
      try {
        ;(menuWrapper as HTMLElement).focus()
        ;(menuWrapper as HTMLElement).blur()
        await vi.advanceTimersByTimeAsync(0)
        await nextTick()
      } finally {
        vi.useRealTimers()
      }

      expectMenuCollapsed()
    })

    it("collapses when route changes", async () => {
      renderHorizontalMenu({ activeItemName: "assimilate" })
      await clickToggleMenu()
      expectMenuExpanded()

      useRouteValue.fullPath = "/recall"
      useRouteValue.name = "recall"
      await nextTick()

      expectMenuCollapsed()
    })
  })

  describe("login button", () => {
    it("shows login button when no user", () => {
      renderHorizontalMenu({ user: undefined })
      expect(ariaLabelEl("Login via Github")).toBeTruthy()
    })

    it("keeps login button visible and hides toggle when no user", () => {
      renderHorizontalMenu({ user: undefined })
      expectMenuExpanded()
      expect(ariaLabelEl("Login via Github")).toBeTruthy()
      expect(ariaLabelEl("Toggle menu")).toBeNull()
    })
  })

  describe("home page behavior", () => {
    it("does not show navigation items on home page", () => {
      renderHorizontalMenu({ activeItemName: "assimilate", isHomePage: true })
      for (const label of allNavLabels) {
        expect(ariaLabelEl(label)).toBeNull()
      }
    })
  })
})
