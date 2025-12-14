import type { Meta, StoryObj } from "@storybook/vue3"
import MainMenu from "./MainMenu.vue"
import makeMe from "@tests/fixtures/makeMe"
import { UserController } from "@generated/backend/sdk.gen"
import type { MenuDataDto, ConversationMessage } from "@generated/backend"
import type { Options } from "@generated/backend/client/types.gen"
import type { GetMenuDataData } from "@generated/backend"

const meta = {
  title: "Toolbars/MainMenu",
  component: MainMenu,
  tags: ["autodocs"],
  parameters: {
    test: {
      disable: true,
    },
  },
  argTypes: {
    user: {
      control: "object",
      description: "The current user. If provided, menu data will be fetched.",
    },
  },
  decorators: [
    (story) => {
      // Mock UserController.getMenuData to avoid API calls
      const originalGetMenuData = UserController.getMenuData
      const mockMenuData: MenuDataDto = {
        assimilationCount: {
          dueCount: 0,
          assimilatedCountOfTheDay: 0,
          totalUnassimilatedCount: 0,
        },
        recallStatus: {
          toRepeat: [],
          totalAssimilatedCount: 0,
        },
        unreadConversations: [],
      }
      UserController.getMenuData = (async <
        ThrowOnError extends boolean = false,
      >(
        _options: Options<GetMenuDataData, ThrowOnError>
      ) => {
        return {
          data: mockMenuData,
          error: undefined,
          request: {} as Request,
          response: {} as Response,
        }
      }) as typeof UserController.getMenuData

      return {
        components: { story },
        template:
          '<div style="width: 200px; height: 100vh; height: 100dvh; background: #f5f5f5;"><story /></div>',
        beforeUnmount() {
          // Restore original method when story unmounts
          UserController.getMenuData = originalGetMenuData
        },
      }
    },
  ],
} satisfies Meta<typeof MainMenu>

export default meta
type Story = StoryObj<typeof meta>

export const WithUser: Story = {
  args: {
    user: makeMe.aUser.please(),
  },
}

export const WithoutUser: Story = {
  args: {
    user: undefined,
  },
}

export const WithDueCount: Story = {
  args: {
    user: makeMe.aUser.please(),
  },
  decorators: [
    (story) => {
      // Override the default mock to return a due count
      const originalGetMenuData = UserController.getMenuData
      const mockMenuData: MenuDataDto = {
        assimilationCount: {
          dueCount: 5,
          assimilatedCountOfTheDay: 0,
          totalUnassimilatedCount: 0,
        },
        recallStatus: {
          toRepeat: [],
          totalAssimilatedCount: 0,
        },
        unreadConversations: [],
      }
      UserController.getMenuData = (async <
        ThrowOnError extends boolean = false,
      >(
        _options: Options<GetMenuDataData, ThrowOnError>
      ) => {
        return {
          data: mockMenuData,
          error: undefined,
          request: {} as Request,
          response: {} as Response,
        }
      }) as typeof UserController.getMenuData

      return {
        components: { story },
        template:
          '<div style="width: 200px; height: 100vh; height: 100dvh; background: #f5f5f5;"><story /></div>',
        beforeUnmount() {
          UserController.getMenuData = originalGetMenuData
        },
      }
    },
  ],
}

export const WithRecallCount: Story = {
  args: {
    user: makeMe.aUser.please(),
  },
  decorators: [
    (story) => {
      // Override the default mock to return a recall count
      const originalGetMenuData = UserController.getMenuData
      const mockMenuData: MenuDataDto = {
        assimilationCount: {
          dueCount: 0,
          assimilatedCountOfTheDay: 0,
          totalUnassimilatedCount: 0,
        },
        recallStatus: {
          toRepeat: Array(789).fill({}),
          totalAssimilatedCount: 0,
        },
        unreadConversations: [],
      }
      UserController.getMenuData = (async <
        ThrowOnError extends boolean = false,
      >(
        _options: Options<GetMenuDataData, ThrowOnError>
      ) => {
        return {
          data: mockMenuData,
          error: undefined,
          request: {} as Request,
          response: {} as Response,
        }
      }) as typeof UserController.getMenuData

      return {
        components: { story },
        template:
          '<div style="width: 200px; height: 100vh; height: 100dvh; background: #f5f5f5;"><story /></div>',
        beforeUnmount() {
          UserController.getMenuData = originalGetMenuData
        },
      }
    },
  ],
}

export const WithUnreadMessages: Story = {
  args: {
    user: makeMe.aUser.please(),
  },
  decorators: [
    (story) => {
      // Override the default mock to return unread conversations
      const originalGetMenuData = UserController.getMenuData
      const mockConversations: ConversationMessage[] = [
        { id: 1, message: "Test message 1" },
        { id: 2, message: "Test message 2" },
        { id: 3, message: "Test message 3" },
      ]
      const mockMenuData: MenuDataDto = {
        assimilationCount: {
          dueCount: 0,
          assimilatedCountOfTheDay: 0,
          totalUnassimilatedCount: 0,
        },
        recallStatus: {
          toRepeat: [],
          totalAssimilatedCount: 0,
        },
        unreadConversations: mockConversations,
      }
      UserController.getMenuData = (async <
        ThrowOnError extends boolean = false,
      >(
        _options: Options<GetMenuDataData, ThrowOnError>
      ) => {
        return {
          data: mockMenuData,
          error: undefined,
          request: {} as Request,
          response: {} as Response,
        }
      }) as typeof UserController.getMenuData

      return {
        components: { story },
        template:
          '<div style="width: 200px; height: 100vh; height: 100dvh; background: #f5f5f5;"><story /></div>',
        beforeUnmount() {
          UserController.getMenuData = originalGetMenuData
        },
      }
    },
  ],
}
