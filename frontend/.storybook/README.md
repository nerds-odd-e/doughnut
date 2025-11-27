# Storybook Documentation

This document explains how to use Storybook in the Doughnut frontend project.

## Running Storybook

To start Storybook locally for component development and debugging:

```bash
pnpm storybook
```

This will start Storybook on `http://localhost:6006` by default.

## Shared Test Data Builders

Storybook stories reuse the same test data builders that are used in unit tests. These builders are located in `tests/fixtures/` and can be imported using the `@tests` alias:

```typescript
import makeMe from '@tests/fixtures/makeMe'
```

### Available Builders

The `makeMe` object provides access to all test data builders:

- `makeMe.anAnsweredQuestion` - Creates answered question data
- `makeMe.aPredefinedQuestion` - Creates predefined question data
- `makeMe.aNote` - Creates note data
- `makeMe.aNoteRealm` - Creates note realm data
- And many more...

### Example Usage

```typescript
import makeMe from '@tests/fixtures/makeMe'

// Create an answered question with correct answer
const correctQuestion = makeMe.anAnsweredQuestion
  .answerCorrect(true)
  .withChoiceIndex(0)
  .please()

// Create a question with a note
const questionWithNote = makeMe.anAnsweredQuestion
  .withNote(makeMe.aNote.topicConstructor('TypeScript').please())
  .answerCorrect(true)
  .please()
```

## Writing a New Story

To create a new Storybook story for a component:

1. Create a file named `ComponentName.stories.ts` in the same directory as your component
2. Import the component and necessary builders
3. Define the story meta and stories

### Example Story

```typescript
import type { Meta, StoryObj } from '@storybook/vue3'
import MyComponent from './MyComponent.vue'
import makeMe from '@tests/fixtures/makeMe'

const meta = {
  title: 'Category/MyComponent',
  component: MyComponent,
  tags: ['autodocs'],
} satisfies Meta<typeof MyComponent>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
  args: {
    prop1: makeMe.someBuilder.please(),
  },
}
```

### Story Variants

Create multiple story variants to showcase different states:

```typescript
export const Variant1: Story = {
  args: {
    // ... props for variant 1
  },
}

export const Variant2: Story = {
  args: {
    // ... props for variant 2
  },
}
```

## Module Aliases

Storybook is configured to support the same module aliases as the main application:

- `@/` - Points to `src/`
- `@tests/` - Points to `tests/`
- `@generated/` - Points to `generated/`

## Global Styles

Storybook automatically loads the global styles from `src/assets/daisyui.css`, which includes:
- Tailwind CSS base styles
- DaisyUI component styles
- Custom application styles

## Existing Stories

The following stories are currently available:

### Review Components

- **AnsweredQuestionComponent** (`src/components/review/AnsweredQuestionComponent.stories.ts`)
  - Shows answered questions with multiple variants:
    - Correct answer
    - Incorrect answer
    - With note/explanation
    - In sequence
    - Without conversation button
    - Custom questions

- **AnswerResult** (`src/components/review/AnswerResult.stories.ts`)
  - Shows answer result feedback:
    - Correct answer display
    - Incorrect answer display
    - With custom display text

- **QuestionDisplay** (`src/components/review/QuestionDisplay.stories.ts`)
  - Shows question display with choices:
    - Unanswered question
    - With correct answer selected
    - With incorrect answer selected
    - Disabled state
    - Long questions with many choices

## Best Practices

1. **Reuse Builders**: Always use the shared test data builders from `@tests/fixtures/makeMe` instead of creating mock data
2. **No Duplication**: Never duplicate mock data - use builders for consistency
3. **Multiple Variants**: Create multiple story variants to showcase different component states
4. **Documentation**: Use the `autodocs` tag to automatically generate documentation
5. **Framework Neutral**: Builders are framework-neutral and work in both tests and Storybook

## Troubleshooting

### Storybook won't start

- Ensure all dependencies are installed: `pnpm install`
- Check that port 6006 is available
- Review the console for error messages

### Module resolution errors

- Verify that the module aliases are correctly configured in `.storybook/main.ts`
- Ensure the `@tests` alias points to the correct location
- Check that TypeScript paths are configured in `tsconfig.json`

### Styles not loading

- Verify that `daisyui.css` is imported in `.storybook/preview.ts`
- Check that Tailwind is properly configured
- Ensure PostCSS is working correctly
