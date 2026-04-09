import type { Meta, StoryObj } from "@storybook/vue3-vite"
import NotebooksPageView from "./NotebooksPageView.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import type { Subscription, User } from "@generated/doughnut-backend-api"

const meta = {
  title: "Page Views/NotebooksPageView",
  component: NotebooksPageView,
  tags: ["autodocs"],
  argTypes: {
    catalogItems: {
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
    catalogItems: makeMe.notebookCatalog
      .notebook("My First Notebook")
      .notebook("Learning TypeScript")
      .notebook("Project Documentation")
      .please(),
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
    catalogItems: makeMe.notebookCatalog
      .notebook("Personal Notes")
      .notebook("Work Projects")
      .please(),
    subscriptions: [],
    user: mockUser,
  },
}

// Empty state - no notebooks or subscriptions
export const Empty: Story = {
  args: {
    catalogItems: [],
    subscriptions: [],
    user: mockUser,
  },
}

// Empty state with no notebooks
export const NoNotebooks: Story = {
  args: {
    catalogItems: [],
    subscriptions: [],
    user: mockUser,
  },
}

export const WithNotebookGroup: Story = {
  args: {
    catalogItems: makeMe.notebookCatalog
      .notebook("Loose notebook")
      .entry(
        makeMe.notebookCatalogGroup
          .id(9001)
          .name("Reading list")
          .createdAt("2024-06-01T12:00:00.000Z")
          .titles("Member Alpha", "Member Beta", "Member Gamma", "Member Delta")
          .please()
      )
      .please(),
    subscriptions: [],
    user: mockUser,
  },
}
