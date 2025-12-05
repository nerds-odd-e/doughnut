import type { Meta, StoryObj } from "@storybook/vue3"
import QuillStoryWrapper from "./QuillStoryWrapper.vue"
import type { QuillOptions } from "quill"

const meta = {
  title: "Form/Quill",
  component: QuillStoryWrapper,
  tags: ["autodocs"],
  parameters: {
    layout: "padded",
  },
} satisfies Meta<typeof QuillStoryWrapper>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
  args: {
    initialContent: "<p>Hello <strong>World</strong>!</p>",
  },
}

export const WithSimpleContent: Story = {
  args: {
    initialContent:
      "<p>This is a simple paragraph with <em>italic</em> and <strong>bold</strong> text.</p>",
  },
}

export const WithList: Story = {
  args: {
    initialContent:
      "<h1>My List</h1><ul><li>First item</li><li>Second item</li><li>Third item</li></ul>",
  },
}

export const WithBlockquote: Story = {
  args: {
    initialContent:
      "<p>Here is a quote:</p><blockquote>This is a blockquote example.</blockquote>",
  },
}

export const ReadOnly: Story = {
  args: {
    initialContent: "<p>This is read-only content. You cannot edit it.</p>",
    options: { readOnly: true } as QuillOptions,
  },
}
