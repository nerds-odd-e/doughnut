import { advanceAnimationFrame } from "@tests/helpers/focusTargetTestSupport"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, it, vi } from "vitest"
import { expectPresetOptions, propertyInputEl } from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor occupied preset slots", () => {
  const h = createRichMarkdownEditorTestHarness()

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ["requestAnimationFrame"] })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
    vi.useRealTimers()
  })

  it.each([
    {
      name: "canonical note slots",
      frontmatter:
        'aliases: color\noverlaps: "[[Other]]"\nnote_level: 2\nimage: /x.png\nwikidata_id: Q1\nquestion_generation_instruction: Recall',
      isReadmeContext: false,
      options: ["url", "example of"],
    },
    {
      name: "legacy aliases and numbered note slots",
      frontmatter:
        'aliases 2: color\noverlaps 2: "[[Other]]"\nnoteLevel 2: 2\nimage 2: /x.png\nwikidataId 2: Q1\nquestionGenerationInstruction 2: Recall',
      isReadmeContext: false,
      options: ["url", "example of"],
    },
    {
      name: "readme title-pattern alias",
      frontmatter: 'titlePattern: "Topic *"',
      isReadmeContext: true,
      options: [
        "image",
        "wikidata_id",
        "url",
        "example of",
        "question_generation_instruction",
      ],
    },
    {
      name: "numbered readme title-pattern slot",
      frontmatter: 'title_pattern 2: "Topic *"',
      isReadmeContext: true,
      options: [
        "image",
        "wikidata_id",
        "url",
        "example of",
        "question_generation_instruction",
      ],
    },
  ])(
    "omits occupied $name without a numbered substitute",
    async ({ frontmatter, isReadmeContext, options }) => {
      await h.mountEditor(`---\n${frontmatter}\n---\n\n# Body`, {
        attachToBody: true,
        isReadmeContext,
      })
      await h.openAddProperty()
      await advanceAnimationFrame()
      expectPresetOptions(options)
    }
  )

  it("keeps the current row's own structural slot available", async () => {
    await h.mountEditor("---\nimage: /x.png\n---\n\n# Body", {
      attachToBody: true,
    })
    propertyInputEl('[data-testid="rich-note-property-row-key-input"]').focus()
    await flushPromises()
    expectPresetOptions([
      "aliases",
      "overlaps",
      "note_level",
      "image",
      "wikidata_id",
      "url",
      "example of",
      "question_generation_instruction",
    ])
  })
})
