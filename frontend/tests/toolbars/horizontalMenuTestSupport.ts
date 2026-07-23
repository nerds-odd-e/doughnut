import HorizontalMenu from "@/components/toolbars/HorizontalMenu.vue"
import type { User } from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { beforeEach, expect, vi } from "vitest"
import { markRaw, reactive, nextTick } from "vue"
import {
  BookText,
  CalendarCheck,
  CircleCheck,
  MessageCircle,
  Store,
  Users,
} from "@lucide/vue"
import type { Component } from "vue"

export const useRouteValue = reactive({ name: "", fullPath: "/" })

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

export const allNavLabels = [
  "Note",
  "Assimilate",
  "Recall",
  "Circles",
  "Bazaar",
  "Messages",
] as const

const noop = () => {
  // intentional no-op for required callback props
}

export function createMockNavItems(activeItemName?: string) {
  return {
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
  } satisfies {
    upperNavItems: NavigationItemType[]
    lowerNavItems: NavigationItemType[]
  }
}

export type RenderHorizontalMenuOptions = {
  user?: User | undefined
  activeItemName?: string
  isHomePage?: boolean
}

export function renderHorizontalMenu(
  options: RenderHorizontalMenuOptions = {}
) {
  const user = "user" in options ? options.user : makeMe.aUser.please()
  const navItems = createMockNavItems(options.activeItemName)
  helper
    .component(HorizontalMenu)
    .withProps({
      user,
      ...navItems,
      isHomePage: options.isHomePage ?? false,
      logout: noop,
    })
    .render()
}

export function menuWrapperEl() {
  return document.querySelector(".menu-wrapper")
}

export function ariaLabelEl(label: string) {
  return document.querySelector(`[aria-label="${label}"]`)
}

export function menuContentEl() {
  return document.querySelector(".menu-content")
}

export async function clickAriaLabel(label: string) {
  const el = ariaLabelEl(label)
  expect(el).toBeTruthy()
  ;(el as HTMLElement).click()
  await nextTick()
}

export async function clickToggleMenu() {
  await clickAriaLabel("Toggle menu")
}

export function expectMenuCollapsed() {
  expect(menuWrapperEl()).toHaveClass("is-collapsed")
  expect(menuWrapperEl()).not.toHaveClass("is-expanded")
}

export function expectMenuExpanded() {
  expect(menuWrapperEl()).toHaveClass("is-expanded")
  expect(menuWrapperEl()).not.toHaveClass("is-collapsed")
}

export function expectAllNavLabelsVisible() {
  for (const label of allNavLabels) {
    expect(ariaLabelEl(label)).toBeTruthy()
  }
}

export function setupHorizontalMenuTests() {
  beforeEach(() => {
    vi.clearAllMocks()
    useRouteValue.name = ""
    useRouteValue.fullPath = "/"
  })
}
