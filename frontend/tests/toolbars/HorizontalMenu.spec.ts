import HorizontalMenu from "@/components/toolbars/HorizontalMenu.vue"
import type { User } from "@generated/backend"
import { screen, fireEvent } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { beforeEach, vi, describe, it, expect } from "vitest"
import SvgNote from "@/components/svgs/SvgNote.vue"
import SvgAssimilate from "@/components/svgs/SvgAssimilate.vue"
import SvgCalendarCheck from "@/components/svgs/SvgCalendarCheck.vue"
import SvgPeople from "@/components/svgs/SvgPeople.vue"
import SvgShop from "@/components/svgs/SvgShop.vue"
import SvgChat from "@/components/svgs/SvgChat.vue"
import type { Component } from "vue"

const useRouteValue = { name: "" }
vi.mock("vue-router", () => ({
  useRoute: () => useRouteValue,
}))

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
        icon: SvgNote,
        isActive: activeItemName === "notebooks",
      },
      {
        name: "assimilate",
        label: "Assimilate",
        icon: SvgAssimilate,
        isActive: activeItemName === "assimilate",
      },
      {
        name: "recall",
        label: "Recall",
        icon: SvgCalendarCheck,
        isActive: activeItemName === "recall",
      },
    ],
    lowerNavItems: [
      {
        name: "circles",
        label: "Circles",
        icon: SvgPeople,
        isActive: activeItemName === "circles",
      },
      {
        name: "bazaar",
        label: "Bazaar",
        icon: SvgShop,
        isActive: activeItemName === "bazaar",
      },
      {
        name: "messageCenter",
        label: "Messages",
        icon: SvgChat,
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

    it("shows expand button when collapsed", () => {
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

      const expandButton = screen.getByLabelText("Toggle menu")
      expect(expandButton).toBeInTheDocument()
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
      const expandButton = screen.getByLabelText("Toggle menu")
      expect(expandButton).toBeInTheDocument()

      // Expand the menu
      await fireEvent.click(expandButton)

      // Expand button should still be visible after expansion
      expect(expandButton).toBeInTheDocument()
    })
  })

  describe("active item display", () => {
    it("shows active item when collapsed", () => {
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

      const activeItem = screen.getByLabelText("Assimilate")
      expect(activeItem).toBeInTheDocument()
    })

    it("hides active item area when collapsed and no active item exists", () => {
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

      const activeItemOnly = document.querySelector(".active-item-only")
      expect(activeItemOnly).not.toBeInTheDocument()
    })

    it("shows active item icon and label correctly when collapsed", () => {
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

      const noteLink = screen.getByLabelText("Note")
      expect(noteLink).toBeInTheDocument()
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

      const expandButton = screen.getByLabelText("Toggle menu")
      await fireEvent.click(expandButton)

      const menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-expanded")
      expect(menuWrapper).not.toHaveClass("is-collapsed")
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

      const expandButton = screen.getByLabelText("Toggle menu")

      // Expand
      await fireEvent.click(expandButton)
      let menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-expanded")

      // Collapse
      await fireEvent.click(expandButton)
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

      const expandButton = screen.getByLabelText("Toggle menu")
      await fireEvent.click(expandButton)

      // All items should be visible
      expect(screen.getByLabelText("Note")).toBeInTheDocument()
      expect(screen.getByLabelText("Assimilate")).toBeInTheDocument()
      expect(screen.getByLabelText("Recall")).toBeInTheDocument()
      expect(screen.getByLabelText("Circles")).toBeInTheDocument()
      expect(screen.getByLabelText("Bazaar")).toBeInTheDocument()
      expect(screen.getByLabelText("Messages")).toBeInTheDocument()
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

      const expandButton = screen.getByLabelText("Toggle menu")
      await fireEvent.click(expandButton)

      let menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-expanded")

      // Click outside
      await fireEvent.click(document.body)

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

      const expandButton = screen.getByLabelText("Toggle menu")
      await fireEvent.click(expandButton)

      let menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-expanded")

      // Trigger blur event
      const menuContent = document.querySelector(".menu-wrapper")
      if (menuContent) {
        await fireEvent.blur(menuContent)
      }

      // Wait for setTimeout in handleFocusLoss
      await new Promise((resolve) => setTimeout(resolve, 10))

      menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-collapsed")
    })
  })

  describe("login button", () => {
    it("shows login button when no user", () => {
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
      const loginButton = screen.getByLabelText("Login via Github")
      expect(loginButton).toBeInTheDocument()
    })

    it("login button is always visible when no user (collapsed state)", () => {
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

      // Menu should be collapsed but login button visible
      const menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-collapsed")

      const loginButton = screen.getByLabelText("Login via Github")
      expect(loginButton).toBeInTheDocument()
    })
  })

  describe("home page behavior", () => {
    it("does not show navigation items on home page", () => {
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
      expect(screen.queryByLabelText("Note")).not.toBeInTheDocument()
      expect(screen.queryByLabelText("Assimilate")).not.toBeInTheDocument()
    })
  })
})
