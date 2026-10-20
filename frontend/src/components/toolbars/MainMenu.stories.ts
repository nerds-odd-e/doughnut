import type { Meta, StoryObj } from "@storybook/vue3-vite"
import MainMenu from "./MainMenu.vue"
import makeMe from "donut-test-fixtures/makeMe"

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
      description: "The current user, or none for a visitor.",
    },
  },
  decorators: [
    (story) => ({
      components: { story },
      template:
        '<div style="width: 200px; height: 100vh; height: 100dvh; background: #f5f5f5;"><story /></div>',
    }),
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
