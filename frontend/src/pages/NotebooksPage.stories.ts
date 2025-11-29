import type { Meta, StoryObj } from "@storybook/vue3"
import NotebooksPageView from "./NotebooksPageView.vue"
import makeMe from "@tests/fixtures/makeMe"
import type { Subscription, User } from "@generated/backend"

const meta = {
  title: "Page Views/NotebooksPageView",
  component: NotebooksPageView,
  tags: ["autodocs"],
  argTypes: {
    notebooks: {
      control: "object",
    },
    subscriptions: {
      control: "object",
    },
    user: {
      control: "object",
    },
  },
} satisfies Meta<typeof NotebooksPageView>

export default meta
type Story = StoryObj<typeof meta>

const mockUser: User = makeMe.aUser.please()

// Page with notebooks and subscriptions
export const WithNotebooksAndSubscriptions: Story = {
  args: {
    notebooks: [
      { ...makeMe.aNotebook.please(), title: "My First Notebook" },
      { ...makeMe.aNotebook.please(), title: "Learning TypeScript" },
      { ...makeMe.aNotebook.please(), title: "Project Documentation" },
    ],
    subscriptions: [
      {
        id: 1,
        notebook: {
          ...makeMe.aNotebook.please(),
          title: "Shared Knowledge Base",
        },
        user: mockUser,
      } as Subscription,
      {
        id: 2,
        notebook: { ...makeMe.aNotebook.please(), title: "Community Notes" },
        user: mockUser,
      } as Subscription,
    ],
    user: mockUser,
  },
}

// Page with only notebooks, no subscriptions
export const WithNotebooksOnly: Story = {
  args: {
    notebooks: [
      { ...makeMe.aNotebook.please(), title: "Personal Notes" },
      { ...makeMe.aNotebook.please(), title: "Work Projects" },
    ],
    subscriptions: [],
    user: mockUser,
  },
}

// Empty state - no notebooks or subscriptions
export const Empty: Story = {
  args: {
    notebooks: [],
    subscriptions: [],
    user: mockUser,
  },
}

// Loading state - data not yet loaded
export const Loading: Story = {
  args: {
    notebooks: undefined,
    subscriptions: undefined,
    user: mockUser,
  },
}
