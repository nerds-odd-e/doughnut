import HorizontalMenu from "@/components/toolbars/HorizontalMenu.vue"
import type { User } from "@generated/backend"
import { screen, fireEvent } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { beforeEach, vi, describe, it, expect } from "vitest"
import { markRaw, reactive } from "vue"
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

    it("shows menu icon when collapsed and no active item exists", () => {
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

      const menuIcon = screen.getByLabelText("Menu")
      expect(menuIcon).toBeInTheDocument()
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
      expect(screen.getByLabelText("Menu")).toBeInTheDocument()

      // Expand the menu
      const expandButton = screen.getByLabelText("Toggle menu")
      await fireEvent.click(expandButton)

      // Menu icon should not be visible when expanded
      expect(screen.queryByLabelText("Menu")).not.toBeInTheDocument()
    })

    it("hides menu icon when there is an active item", () => {
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
      expect(screen.getByLabelText("Assimilate")).toBeInTheDocument()
      // Menu icon should not be visible
      expect(screen.queryByLabelText("Menu")).not.toBeInTheDocument()
    })

    it("hides menu icon on home page", () => {
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
      expect(screen.queryByLabelText("Menu")).not.toBeInTheDocument()
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
      const menuIcon = screen.getByLabelText("Menu")
      await fireEvent.click(menuIcon)

      // Menu should now be expanded
      menuWrapper = document.querySelector(".menu-wrapper")
      expect(menuWrapper).toHaveClass("is-expanded")
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
      const activeItem = screen.getByLabelText("Assimilate")
      await fireEvent.click(activeItem)

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
        await fireEvent.click(menuContent)
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

    it("login button is always visible when no user and menu is expanded", () => {
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

      const loginButton = screen.getByLabelText("Login via Github")
      expect(loginButton).toBeInTheDocument()

      // Chevron should not be visible when no user
      const expandButton = screen.queryByLabelText("Toggle menu")
      expect(expandButton).not.toBeInTheDocument()
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
      const expandButton = screen.getByLabelText("Toggle menu")
      await fireEvent.click(expandButton)

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
