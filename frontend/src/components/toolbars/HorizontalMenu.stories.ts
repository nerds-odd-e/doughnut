import type { Meta, StoryObj } from "@storybook/vue3"
import HorizontalMenu from "./HorizontalMenu.vue"
import makeMe from "@tests/fixtures/makeMe"
import SvgNote from "@/components/svgs/SvgNote.vue"
import SvgAssimilate from "@/components/svgs/SvgAssimilate.vue"
import SvgCalendarCheck from "@/components/svgs/SvgCalendarCheck.vue"
import SvgShop from "@/components/svgs/SvgShop.vue"
import SvgPeople from "@/components/svgs/SvgPeople.vue"
import SvgChat from "@/components/svgs/SvgChat.vue"
import type { Component } from "vue"

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
  withBadges = false
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
        isActive: false,
      },
      {
        name: "assimilate",
        label: "Assimilate",
        icon: SvgAssimilate,
        badge: withBadges ? 5 : undefined,
        badgeClass: "due-count",
        isActive: false,
      },
      {
        name: "recall",
        label: "Recall",
        icon: SvgCalendarCheck,
        badge: withBadges ? 789 : undefined,
        badgeClass: "recall-count",
        isActive: false,
      },
    ],
    lowerNavItems: [
      {
        name: "circles",
        label: "Circles",
        icon: SvgPeople,
        isActive: false,
      },
      {
        name: "bazaar",
        label: "Bazaar",
        icon: SvgShop,
        isActive: false,
      },
      {
        name: "messageCenter",
        label: "Messages",
        icon: SvgChat,
        badge: withBadges ? 3 : undefined,
        badgeClass: "unread-count",
        isActive: false,
      },
    ],
  }
}

const meta = {
  title: "Toolbars/HorizontalMenu",
  component: HorizontalMenu,
  tags: ["autodocs"],
  parameters: {
    test: {
      disable: true,
    },
  },
  argTypes: {
    user: {
      control: "object",
      description: "The current user. If not provided, shows login button.",
    },
    upperNavItems: {
      control: "object",
      description: "Upper navigation items to display.",
    },
    lowerNavItems: {
      control: "object",
      description: "Lower navigation items to display.",
    },
    isHomePage: {
      control: "boolean",
      description: "Whether the current page is the home page.",
    },
    showUserSettingsDialog: {
      action: "showUserSettingsDialog",
      description: "Function to show user settings dialog.",
    },
    logout: {
      action: "logout",
      description: "Function to handle logout.",
    },
  },
  decorators: [
    (story) => {
      return {
        components: { story },
        template:
          '<div style="width: 100%; height: 80px; background: #f5f5f5; padding: 1rem;"><story /></div>',
      }
    },
  ],
} satisfies Meta<typeof HorizontalMenu>

export default meta
type Story = StoryObj<typeof meta>

const defaultNavItems = createMockNavItems()

const noop = () => {
  // No-op function for storybook actions
}

export const WithUser: Story = {
  args: {
    user: makeMe.aUser.please(),
    ...defaultNavItems,
    isHomePage: false,
    showUserSettingsDialog: noop,
    logout: noop,
  },
}

export const WithoutUser: Story = {
  args: {
    user: undefined,
    ...defaultNavItems,
    isHomePage: false,
    showUserSettingsDialog: noop,
    logout: noop,
  },
}

export const WithBadges: Story = {
  args: {
    user: makeMe.aUser.please(),
    ...createMockNavItems(true),
    isHomePage: false,
    showUserSettingsDialog: noop,
    logout: noop,
  },
}

export const OnHomePage: Story = {
  args: {
    user: makeMe.aUser.please(),
    ...defaultNavItems,
    isHomePage: true,
    showUserSettingsDialog: noop,
    logout: noop,
  },
}

export const WithActiveItem: Story = {
  args: {
    user: makeMe.aUser.please(),
    upperNavItems: [
      {
        name: "notebooks",
        label: "Note",
        icon: SvgNote,
        isActive: false,
      },
      {
        name: "assimilate",
        label: "Assimilate",
        icon: SvgAssimilate,
        isActive: true,
        badge: 5,
        badgeClass: "due-count",
      },
      {
        name: "recall",
        label: "Recall",
        icon: SvgCalendarCheck,
        isActive: false,
      },
    ],
    lowerNavItems: defaultNavItems.lowerNavItems,
    isHomePage: false,
    showUserSettingsDialog: noop,
    logout: noop,
  },
}
