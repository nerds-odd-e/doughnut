import type { Meta, StoryObj } from "@storybook/vue3-vite"
import NotebooksPageView from "./NotebooksPageView.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import type {
  Notebook,
  Subscription,
  User,
} from "@generated/doughnut-backend-api"
import type { NotebookCatalogEntry } from "@/components/notebook/patchNotebookInCatalogItems"

function catalogFromNotebooks(notebooks: Notebook[]): NotebookCatalogEntry[] {
  return notebooks.map((notebook) => ({ type: "notebook", notebook }))
}

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
    catalogItems: catalogFromNotebooks([
      { ...makeMe.aNotebook.please(), title: "My First Notebook" },
      { ...makeMe.aNotebook.please(), title: "Learning TypeScript" },
      { ...makeMe.aNotebook.please(), title: "Project Documentation" },
    ]),
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
    catalogItems: catalogFromNotebooks([
      { ...makeMe.aNotebook.please(), title: "Personal Notes" },
      { ...makeMe.aNotebook.please(), title: "Work Projects" },
    ]),
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
    catalogItems: (() => {
      const loose = {
        ...makeMe.aNotebook.please(),
        title: "Loose notebook",
      }
      const m1 = { ...makeMe.aNotebook.please(), title: "Member Alpha" }
      const m2 = { ...makeMe.aNotebook.please(), title: "Member Beta" }
      const m3 = { ...makeMe.aNotebook.please(), title: "Member Gamma" }
      const m4 = { ...makeMe.aNotebook.please(), title: "Member Delta" }
      const group: NotebookCatalogEntry = {
        type: "notebookGroup",
        id: 9001,
        name: "Reading list",
        createdAt: "2024-06-01T12:00:00.000Z",
        notebooks: [m1, m2, m3, m4],
      }
      return [
        { type: "notebook", notebook: loose },
        group,
      ] as NotebookCatalogEntry[]
    })(),
    subscriptions: [],
    user: mockUser,
  },
}
