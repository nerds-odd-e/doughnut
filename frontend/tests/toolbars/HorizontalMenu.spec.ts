import HorizontalMenu from "@/components/toolbars/HorizontalMenu.vue"
import type { User } from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { beforeEach, vi, describe, it, expect } from "vitest"
import { markRaw, reactive, nextTick } from "vue"
import { page } from "vitest/browser"
import {
  BookText,
  CalendarCheck,
  CircleCheck,
  MessageCircle,
  Store,
  Users,
} from "@lucide/vue"
import type { Component } from "vue"

const useRouteValue = reactive({ name: "", fullPath: "/" })
vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRoute: () => useRouteValue,
    useRouter: () => ({
      push: vi.fn(),
      replace: vi.fn(),
    }),
  }
})

type NavigationItemType = {
  name?: string
  label: string
  icon: Component
  isActive: boolean
  badge?: number
  badgeClass?: string
  hasDropdown?: boolean
}

const createMockNavItems = (
  activeItemName?: string
): {
  upperNavItems: NavigationItemType[]
  lowerNavItems: NavigationItemType[]
} => ({
  upperNavItems: [
    {
      name: "notebooks",
      label: "Note",
      icon: markRaw(BookText),
      isActive: activeItemName === "notebooks",
    },
    {
      name: "assimilate",
      label: "Assimilate",
      icon: markRaw(CircleCheck),
      isActive: activeItemName === "assimilate",
    },
    {
      name: "recall",
      label: "Recall",
      icon: markRaw(CalendarCheck),
      isActive: activeItemName === "recall",
    },
  ],
  lowerNavItems: [
    {
      name: "circles",
      label: "Circles",
      icon: markRaw(Users),
      isActive: activeItemName === "circles",
    },
    {
      name: "bazaar",
      label: "Bazaar",
      icon: markRaw(Store),
      isActive: activeItemName === "bazaar",
    },
    {
      name: "messageCenter",
      label: "Messages",
      icon: markRaw(MessageCircle),
      isActive: activeItemName === "messageCenter",
    },
  ],
})

const noop = () => {
  // No-op function
}

function menuWrapperEl() {
  return document.querySelector(".menu-wrapper")
}

function ariaLabelEl(label: string) {
  return document.querySelector(`[aria-label="${label}"]`)
}

async function clickToggleMenu() {
  await page.getByLabelText("Toggle menu").click()
  await nextTick()
}

describe("HorizontalMenu", () => {
  let user: User

  beforeEach(() => {
    vi.clearAllMocks()
    useRouteValue.name = ""
    useRouteValue.fullPath = "/"
    user = makeMe.aUser.please()
  })

  describe("initial state", () => {
    it("starts in collapsed state", async () => {
      const navItems = createMockNavItems("assimilate")
      helper
        .component(HorizontalMenu)
        .withProps({
          user,
          ...navItems,
          isHomePage: false,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      await nextTick()

      const menuWrapper = menuWrapperEl()
      expect(menuWrapper).toHaveClass("is-collapsed")
      expect(menuWrapper).not.toHaveClass("is-expanded")
    })

    it("shows expand button when collapsed", async () => {
      const navItems = createMockNavItems("assimilate")
      helper
        .component(HorizontalMenu)
        .withProps({
          user,
          ...navItems,
          isHomePage: false,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      expect(ariaLabelEl("Toggle menu")).toBeTruthy()
    })
  })

  describe("active item display", () => {
    it("shows active item when collapsed", async () => {
      const navItems = createMockNavItems("assimilate")
      helper
        .component(HorizontalMenu)
        .withProps({
          user,
          ...navItems,
          isHomePage: false,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      expect(ariaLabelEl("Assimilate")).toBeTruthy()
    })

    it("shows menu icon when collapsed and no active item exists", async () => {
      const navItems = createMockNavItems()
      helper
        .component(HorizontalMenu)
        .withProps({
          user,
          ...navItems,
          isHomePage: false,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      expect(ariaLabelEl("Menu")).toBeTruthy()
    })

    it("hides menu icon when expanded", async () => {
      const navItems = createMockNavItems()
      helper
        .component(HorizontalMenu)
        .withProps({
          user,
          ...navItems,
          isHomePage: false,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      expect(ariaLabelEl("Menu")).toBeTruthy()

      ;(ariaLabelEl("Toggle menu") as HTMLElement).click()
      await nextTick()

      expect(ariaLabelEl("Menu")).toBeNull()
    })

    it("hides menu icon when there is an active item", async () => {
      const navItems = createMockNavItems("assimilate")
      helper
        .component(HorizontalMenu)
        .withProps({
          user,
          ...navItems,
          isHomePage: false,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      expect(ariaLabelEl("Assimilate")).toBeTruthy()
      expect(ariaLabelEl("Menu")).toBeNull()
    })

    it("hides menu icon on home page", async () => {
      const navItems = createMockNavItems()
      helper
        .component(HorizontalMenu)
        .withProps({
          user,
          ...navItems,
          isHomePage: true,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      expect(ariaLabelEl("Menu")).toBeNull()
    })

    it("expands menu when clicking menu icon", async () => {
      const navItems = createMockNavItems()
      helper
        .component(HorizontalMenu)
        .withProps({
          user,
          ...navItems,
          isHomePage: false,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      await nextTick()
      expect(menuWrapperEl()).toHaveClass("is-collapsed")

      await page.getByLabelText("Menu", { exact: true }).click()
      await nextTick()

      expect(menuWrapperEl()).toHaveClass("is-expanded")
    })

    it("shows active item icon and label correctly when collapsed", async () => {
      const navItems = createMockNavItems("notebooks")
      helper
        .component(HorizontalMenu)
        .withProps({
          user,
          ...navItems,
          isHomePage: false,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      expect(ariaLabelEl("Note")).toBeTruthy()
    })
  })

  describe("expand/collapse behavior", () => {
    it("expands menu when expand button is clicked", async () => {
      const navItems = createMockNavItems("assimilate")
      helper
        .component(HorizontalMenu)
        .withProps({
          user,
          ...navItems,
          isHomePage: false,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      const expandButton = page.getByLabelText("Toggle menu")
      await expandButton.click()

      const menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-expanded")
      expect(menuWrapper).not.toHaveClass("is-collapsed")
    })

    it("expands menu when clicking on active item when collapsed", async () => {
      const navItems = createMockNavItems("notebooks")
      helper
        .component(HorizontalMenu)
        .withProps({
          user,
          ...navItems,
          isHomePage: false,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      // Menu should start collapsed
      let menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-collapsed")

      // Click on the active item (which is visible when collapsed)
      const activeItem = page.getByLabelText("Note")
      await activeItem.click({ force: true })

      // Menu should now be expanded
      menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-expanded")
    })

    it("expands menu when clicking anywhere in menu content when collapsed", async () => {
      const navItems = createMockNavItems("assimilate")
      helper
        .component(HorizontalMenu)
        .withProps({
          user,
          ...navItems,
          isHomePage: false,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      // Menu should start collapsed
      let menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-collapsed")

      // Click on the menu content area
      const menuContent = document.querySelector(".menu-content")
      if (menuContent) {
        ;(menuContent as HTMLElement).click()
        await nextTick()
      }

      // Menu should now be expanded
      menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-expanded")
    })

    it("collapses menu when expand button is clicked again", async () => {
      const navItems = createMockNavItems("assimilate")
      helper
        .component(HorizontalMenu)
        .withProps({
          user,
          ...navItems,
          isHomePage: false,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      await clickToggleMenu()
      expect(menuWrapperEl()).toHaveClass("is-expanded")

      await clickToggleMenu()
      expect(menuWrapperEl()).toHaveClass("is-collapsed")
    })

    it("shows all menu items when expanded", async () => {
      const navItems = createMockNavItems("assimilate")
      helper
        .component(HorizontalMenu)
        .withProps({
          user,
          ...navItems,
          isHomePage: false,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      await page.getByLabelText("Toggle menu").click()
      await nextTick()

      for (const label of [
        "Note",
        "Assimilate",
        "Recall",
        "Circles",
        "Bazaar",
        "Messages",
      ]) {
        expect(ariaLabelEl(label)).toBeTruthy()
      }
    })
  })

  describe("click outside behavior", () => {
    it("collapses menu when clicking outside", async () => {
      const navItems = createMockNavItems("assimilate")
      helper
        .component(HorizontalMenu)
        .withProps({
          user,
          ...navItems,
          isHomePage: false,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      await clickToggleMenu()
      expect(menuWrapperEl()).toHaveClass("is-expanded")

      document.body.click()
      await nextTick()

      expect(menuWrapperEl()).toHaveClass("is-collapsed")
    })
  })

  describe("focus loss behavior", () => {
    it("collapses menu when losing focus", async () => {
      const navItems = createMockNavItems("assimilate")
      helper
        .component(HorizontalMenu)
        .withProps({
          user,
          ...navItems,
          isHomePage: false,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      const expandButton = page.getByLabelText("Toggle menu")
      await expandButton.click()

      let menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toBeTruthy()
      expect(menuWrapper).toHaveClass("is-expanded")

      vi.useFakeTimers()
      try {
        ;(menuWrapper as HTMLElement).focus()
        ;(menuWrapper as HTMLElement).blur()
        await vi.advanceTimersByTimeAsync(0)
        await nextTick()
      } finally {
        vi.useRealTimers()
      }

      menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-collapsed")
    })
  })

  describe("login button", () => {
    it("shows login button when no user", async () => {
      const navItems = createMockNavItems()
      helper
        .component(HorizontalMenu)
        .withProps({
          user: undefined,
          ...navItems,
          isHomePage: false,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      expect(ariaLabelEl("Login via Github")).toBeTruthy()
    })

    it("login button is always visible when no user and menu is expanded", async () => {
      const navItems = createMockNavItems()
      helper
        .component(HorizontalMenu)
        .withProps({
          user: undefined,
          ...navItems,
          isHomePage: false,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      const menuWrapper = menuWrapperEl()
      expect(menuWrapper).toHaveClass("is-expanded")
      expect(menuWrapper).not.toHaveClass("is-collapsed")

      expect(ariaLabelEl("Login via Github")).toBeTruthy()
      expect(ariaLabelEl("Toggle menu")).toBeNull()
    })
  })

  describe("home page behavior", () => {
    it("does not show navigation items on home page", async () => {
      const navItems = createMockNavItems("assimilate")
      helper
        .component(HorizontalMenu)
        .withProps({
          user,
          ...navItems,
          isHomePage: true,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      expect(ariaLabelEl("Note")).toBeNull()
      expect(ariaLabelEl("Assimilate")).toBeNull()
    })
  })

  describe("route change behavior", () => {
    it("collapses menu when route changes", async () => {
      const navItems = createMockNavItems("assimilate")
      helper
        .component(HorizontalMenu)
        .withProps({
          user,
          ...navItems,
          isHomePage: false,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      const expandButton = page.getByLabelText("Toggle menu")
      await expandButton.click()

      const menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-expanded")

      useRouteValue.fullPath = "/recall"
      useRouteValue.name = "recall"
      await nextTick()

      expect(document.querySelector(".menu-wrapper")).toHaveClass(
        "is-collapsed"
      )
    })

    it("collapses menu when route changes even if already collapsed", async () => {
      const navItems = createMockNavItems("assimilate")
      helper
        .component(HorizontalMenu)
        .withProps({
          user,
          ...navItems,
          isHomePage: false,
          showUserSettingsDialog: noop,
          logout: noop,
        })
        .render()

      expect(menuWrapperEl()).toHaveClass("is-collapsed")

      useRouteValue.fullPath = "/notebooks"
      useRouteValue.name = "notebooks"
      await nextTick()

      expect(menuWrapperEl()).toHaveClass("is-collapsed")
    })
  })
})
