import type { Meta, StoryObj } from "@storybook/vue3-vite"
import DiffView from "./DiffView.vue"

const meta = {
  title: "Commons/DiffView",
  component: DiffView,
  tags: ["autodocs"],
  parameters: {
    layout: "padded",
  },
  decorators: [
    () => ({
      template:
        '<div style="width: 100%; max-width: 1200px; margin: 0 auto;"><story /></div>',
    }),
  ],
  argTypes: {
    current: {
      control: "text",
      description: "Current version of the text",
    },
    old: {
      control: "text",
      description: "Old version of the text to restore to",
    },
    maxHeight: {
      control: "text",
      description: "Maximum height of the diff panes",
    },
  },
} satisfies Meta<typeof DiffView>

export default meta
type Story = StoryObj<typeof meta>

export const IdenticalContent: Story = {
  args: {
    current: "Line 1\nLine 2\nLine 3",
    old: "Line 1\nLine 2\nLine 3",
    maxHeight: "300px",
  },
}

export const SingleLineChange: Story = {
  args: {
    current: "Line 1\nModified Line 2\nLine 3",
    old: "Line 1\nLine 2\nLine 3",
    maxHeight: "300px",
  },
}

export const LinesAddedInMiddle: Story = {
  args: {
    current:
      "First line\nNew line inserted\nAnother new line\nSecond line\nThird line",
    old: "First line\nSecond line\nThird line",
    maxHeight: "300px",
  },
}

export const LinesDeletedFromMiddle: Story = {
  args: {
    current: "First line\nThird line",
    old: "First line\nDeleted line\nAnother deleted\nThird line",
    maxHeight: "300px",
  },
}

export const MixedChanges: Story = {
  args: {
    current:
      "Header\nNew content here\nMiddle section\nMore new content\nFooter changed",
    old: "Header\nMiddle section\nOld footer",
    maxHeight: "300px",
  },
}

export const LongContentWithScroll: Story = {
  args: {
    current: Array.from({ length: 30 }, (_, i) => `Current Line ${i + 1}`).join(
      "\n"
    ),
    old: Array.from({ length: 30 }, (_, i) => `Old Line ${i + 1}`).join("\n"),
    maxHeight: "200px",
  },
}

export const CodeDiff: Story = {
  args: {
    current: `function greet(name) {
  const greeting = "Hello, " + name;
  console.log(greeting);
  return greeting;
}

// New function added
function farewell(name) {
  return "Goodbye, " + name;
}`,
    old: `function greet(name) {
  console.log("Hello, " + name);
}`,
    maxHeight: "400px",
  },
}

export const MarkdownDiff: Story = {
  args: {
    current: `# Updated Title

This is the **updated** introduction paragraph with some new content.

## New Section

- Item 1
- Item 2
- Item 3

## Conclusion

The conclusion has been rewritten.`,
    old: `# Original Title

This is the introduction paragraph.

## Conclusion

Original conclusion.`,
    maxHeight: "400px",
  },
}

export const EmptyOld: Story = {
  args: {
    current: "Line 1\nLine 2\nLine 3",
    old: "",
    maxHeight: "300px",
  },
}

export const EmptyCurrent: Story = {
  args: {
    current: "",
    old: "Line 1\nLine 2\nLine 3",
    maxHeight: "300px",
  },
}
