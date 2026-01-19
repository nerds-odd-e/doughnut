import HorizontalMenu from "@/components/toolbars/HorizontalMenu.vue"
import type { User } from "@generated/backend"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { beforeEach, vi, describe, it, expect } from "vitest"
import { markRaw, reactive, nextTick } from "vue"
import { page } from "vitest/browser"
import SvgNote from "@/components/svgs/SvgNote.vue"
import SvgAssimilate from "@/components/svgs/SvgAssimilate.vue"
import SvgCalendarCheck from "@/components/svgs/SvgCalendarCheck.vue"
import SvgPeople from "@/components/svgs/SvgPeople.vue"
import SvgShop from "@/components/svgs/SvgShop.vue"
import SvgChat from "@/components/svgs/SvgChat.vue"
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
} => {
  return {
    upperNavItems: [
      {
        name: "notebooks",
        label: "Note",
        icon: markRaw(SvgNote),
        isActive: activeItemName === "notebooks",
      },
      {
        name: "assimilate",
        label: "Assimilate",
        icon: markRaw(SvgAssimilate),
        isActive: activeItemName === "assimilate",
      },
      {
        name: "recall",
        label: "Recall",
        icon: markRaw(SvgCalendarCheck),
        isActive: activeItemName === "recall",
      },
    ],
    lowerNavItems: [
      {
        name: "circles",
        label: "Circles",
        icon: markRaw(SvgPeople),
        isActive: activeItemName === "circles",
      },
      {
        name: "bazaar",
        label: "Bazaar",
        icon: markRaw(SvgShop),
        isActive: activeItemName === "bazaar",
      },
      {
        name: "messageCenter",
        label: "Messages",
        icon: markRaw(SvgChat),
        isActive: activeItemName === "messageCenter",
      },
    ],
  }
}

const noop = () => {
  // No-op function
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
    it("starts in collapsed state", () => {
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

      const menuWrapper = document.querySelector(".menu-wrapper")
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

      const expandButton = page.getByLabelText("Toggle menu")
      await expect.element(expandButton).toBeInTheDocument()
    })

    it("shows expand button when expanded", async () => {
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

      // Expand button should be visible initially
      const expandButton = page.getByLabelText("Toggle menu")
      await expect.element(expandButton).toBeInTheDocument()

      // Expand the menu
      await expandButton.click()

      // Expand button should still be visible after expansion
      await expect.element(expandButton).toBeInTheDocument()
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

      const activeItem = page.getByLabelText("Assimilate")
      await expect.element(activeItem).toBeInTheDocument()
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

      const menuIcon = page.getByLabelText("Menu", { exact: true })
      await expect.element(menuIcon).toBeInTheDocument()
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

      // Menu icon should be visible when collapsed
      await expect.element(page.getByLabelText("Menu", { exact: true })).toBeInTheDocument()

      // Expand the menu
      const expandButton = page.getByLabelText("Toggle menu")
      await expandButton.click()

      // Menu icon should not be visible when expanded
      await expect.element(page.getByLabelText("Menu", { exact: true })).not.toBeInTheDocument()
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

      // Active item should be visible
      await expect.element(page.getByLabelText("Assimilate")).toBeInTheDocument()
      // Menu icon should not be visible
      await expect.element(page.getByLabelText("Menu", { exact: true })).not.toBeInTheDocument()
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

      // Menu icon should not be visible on home page
      await expect.element(page.getByLabelText("Menu", { exact: true })).not.toBeInTheDocument()
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

      // Menu should start collapsed
      let menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-collapsed")

      // Click on the menu icon
      const menuIcon = page.getByLabelText("Menu", { exact: true })
      await menuIcon.click()

      // Menu should now be expanded
      menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-expanded")
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

      const noteLink = page.getByLabelText("Note")
      await expect.element(noteLink).toBeInTheDocument()
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

      // Click on the active item (which is visible when collapsed)
      const activeItem = page.getByLabelText("Assimilate")
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
        ;(menuContent as HTMLElement).click(); await nextTick()
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

      const expandButton = page.getByLabelText("Toggle menu")

      // Expand
      await expandButton.click()
      let menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-expanded")

      // Collapse
      await expandButton.click()
      menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-collapsed")
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

      const expandButton = page.getByLabelText("Toggle menu")
      await expandButton.click()

      // All items should be visible
      await expect.element(page.getByLabelText("Note")).toBeInTheDocument()
      await expect.element(page.getByLabelText("Assimilate")).toBeInTheDocument()
      await expect.element(page.getByLabelText("Recall")).toBeInTheDocument()
      await expect.element(page.getByLabelText("Circles")).toBeInTheDocument()
      await expect.element(page.getByLabelText("Bazaar")).toBeInTheDocument()
      await expect.element(page.getByLabelText("Messages")).toBeInTheDocument()
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

      const expandButton = page.getByLabelText("Toggle menu")
      await expandButton.click()

      let menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-expanded")

      // Click outside
      document.body.click(); await nextTick()

      menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-collapsed")
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
      expect(menuWrapper).toHaveClass("is-expanded")

      // Trigger blur event
      const menuContent = document.querySelector(".menu-wrapper")
      if (menuContent) {
        ;(menuContent as HTMLElement).focus()
        ;(menuContent as HTMLElement).blur(); await nextTick()
      }

      // Wait for setTimeout in handleFocusLoss
      await new Promise((resolve) => setTimeout(resolve, 10))

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

      // LoginButton should be visible
      const loginButton = page.getByLabelText("Login via Github")
      await expect.element(loginButton).toBeInTheDocument()
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

      // Menu should be expanded when no user
      const menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-expanded")
      expect(menuWrapper).not.toHaveClass("is-collapsed")

      const loginButton = page.getByLabelText("Login via Github")
      await expect.element(loginButton).toBeInTheDocument()

      // Chevron should not be visible when no user
      const expandButton = page.getByLabelText("Toggle menu")
      await expect.element(expandButton).not.toBeInTheDocument()
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

      // Navigation items should not be visible
      await expect.element(page.getByLabelText("Note")).not.toBeInTheDocument()
      await expect.element(page.getByLabelText("Assimilate")).not.toBeInTheDocument()
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

      // Expand the menu first
      const expandButton = page.getByLabelText("Toggle menu")
      await expandButton.click()

      let menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-expanded")

      // Simulate route change by updating the mocked route fullPath
      useRouteValue.fullPath = "/d/recall"
      useRouteValue.name = "recall"

      // Wait for watcher to process
      await new Promise((resolve) => setTimeout(resolve, 10))

      menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-collapsed")
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

      // Menu should start collapsed
      let menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-collapsed")

      // Simulate route change
      useRouteValue.fullPath = "/d/notebooks"
      useRouteValue.name = "notebooks"

      // Wait for watcher to process
      await new Promise((resolve) => setTimeout(resolve, 10))

      // Menu should still be collapsed
      menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-collapsed")
    })
  })
})
