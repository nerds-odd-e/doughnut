import type { Meta, StoryObj } from "@storybook/vue3"
import NotebookPageView from "./NotebookPageView.vue"
import makeMe from "@tests/fixtures/makeMe"
import type { User } from "@generated/backend"

const meta = {
  title: "Page Views/NotebookPageView",
  component: NotebookPageView,
  tags: ["autodocs"],
  argTypes: {
    notebook: {
      control: "object",
    },
    user: {
      control: "object",
    },
  },
  parameters: {
    test: {
      disable: true,
    },
  },
} satisfies Meta<typeof NotebookPageView>

export default meta
type Story = StoryObj<typeof meta>

const mockUser: User = makeMe.aUser.please()
const mockAdminUser: User = makeMe.aUser.admin(true).please()

// Default notebook settings
export const Default: Story = {
  args: {
    notebook: makeMe.aNotebook.please(),
    user: mockUser,
    approval: undefined,
    approvalLoaded: true,
    additionalInstructions: "",
  },
}

// Notebook with custom settings
export const WithCustomSettings: Story = {
  args: {
    notebook: {
      ...makeMe.aNotebook.numberOfQuestionsInAssessment(5).please(),
      notebookSettings: {
        ...makeMe.aNotebook.numberOfQuestionsInAssessment(5).please()
          .notebookSettings,
        certificateExpiry: "2y 3m",
      },
    },
    user: mockUser,
    approval: undefined,
    approvalLoaded: true,
    additionalInstructions: "",
  },
}

// Admin user view (shows admin section)
export const AdminView: Story = {
  args: {
    notebook: makeMe.aNotebook.please(),
    user: mockAdminUser,
    approval: undefined,
    approvalLoaded: true,
    additionalInstructions: "Custom AI instructions",
  },
}
