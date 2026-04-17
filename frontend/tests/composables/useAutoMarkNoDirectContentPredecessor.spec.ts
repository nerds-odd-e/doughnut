import { useAutoMarkNoDirectContentPredecessor } from "@/composables/useAutoMarkNoDirectContentPredecessor"
import type { BookBlockReadingDisposition } from "@/lib/book-reading/readBlockIdsFromRecords"
import type {
  BookBlockFull,
  EpubLocatorFull,
} from "@generated/doughnut-backend-api"
import { flushPromises, mount } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { computed, defineComponent, nextTick, ref } from "vue"

function epubLoc(href: string, fragment?: string): EpubLocatorFull {
  return {
    type: "EpubLocator_Full",
    href,
    ...(fragment !== undefined ? { fragment } : {}),
  }
}

function stubBlock(
  id: number,
  contentLocators: BookBlockFull["contentLocators"]
): BookBlockFull {
  return {
    id,
    depth: 0,
    title: `block-${id}`,
    contentLocators,
    contentBlocks: [],
  }
}

describe("useAutoMarkNoDirectContentPredecessor", () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  function mountHarness(options: {
    blocks: BookBlockFull[]
    hasRecordedDisposition: (id: number) => boolean
    submitReadingDisposition: (
      bookBlockId: number,
      status: BookBlockReadingDisposition
    ) => Promise<boolean>
  }) {
    const currentBlockId = ref<number | null>(null)
    const bookBlocks = computed(() => options.blocks)

    const Root = defineComponent({
      setup() {
        useAutoMarkNoDirectContentPredecessor({
          bookBlocks,
          currentBlockId,
          hasRecordedDisposition: options.hasRecordedDisposition,
          submitReadingDisposition: options.submitReadingDisposition,
        })
        return { currentBlockId }
      },
      template: "<div />",
    })

    const wrapper = mount(Root)
    return { wrapper, currentBlockId }
  }

  it("does not call submit when predecessor has direct content (multiple locators)", async () => {
    const submit = vi
      .fn<
        (
          bookBlockId: number,
          status: BookBlockReadingDisposition
        ) => Promise<boolean>
      >()
      .mockResolvedValue(true)
    const pred = stubBlock(1, [epubLoc("a.xhtml"), epubLoc("a.xhtml", "frag")])
    const cur = stubBlock(2, [epubLoc("b.xhtml")])
    const { currentBlockId } = mountHarness({
      blocks: [pred, cur],
      hasRecordedDisposition: () => false,
      submitReadingDisposition: submit,
    })

    currentBlockId.value = 2
    await nextTick()
    await flushPromises()

    expect(submit).not.toHaveBeenCalled()
  })

  it("does not call submit when predecessor has one locator but already has a disposition", async () => {
    const submit = vi
      .fn<
        (
          bookBlockId: number,
          status: BookBlockReadingDisposition
        ) => Promise<boolean>
      >()
      .mockResolvedValue(true)
    const pred = stubBlock(1, [epubLoc("a.xhtml")])
    const cur = stubBlock(2, [epubLoc("b.xhtml")])
    const { currentBlockId } = mountHarness({
      blocks: [pred, cur],
      hasRecordedDisposition: (id) => id === 1,
      submitReadingDisposition: submit,
    })

    currentBlockId.value = 2
    await nextTick()
    await flushPromises()

    expect(submit).not.toHaveBeenCalled()
  })

  it("calls submit with READ when predecessor has one locator and no record", async () => {
    const submit = vi
      .fn<
        (
          bookBlockId: number,
          status: BookBlockReadingDisposition
        ) => Promise<boolean>
      >()
      .mockResolvedValue(true)
    const pred = stubBlock(1, [epubLoc("a.xhtml")])
    const cur = stubBlock(2, [epubLoc("b.xhtml")])
    const { currentBlockId } = mountHarness({
      blocks: [pred, cur],
      hasRecordedDisposition: () => false,
      submitReadingDisposition: submit,
    })

    currentBlockId.value = 2
    await nextTick()
    await flushPromises()

    expect(submit).toHaveBeenCalledTimes(1)
    expect(submit).toHaveBeenCalledWith(1, "READ")
  })

  it("does not call submit when predecessor has empty contentLocators", async () => {
    const submit = vi
      .fn<
        (
          bookBlockId: number,
          status: BookBlockReadingDisposition
        ) => Promise<boolean>
      >()
      .mockResolvedValue(true)
    const pred = stubBlock(1, [])
    const cur = stubBlock(2, [epubLoc("b.xhtml")])
    const { currentBlockId } = mountHarness({
      blocks: [pred, cur],
      hasRecordedDisposition: () => false,
      submitReadingDisposition: submit,
    })

    currentBlockId.value = 2
    await nextTick()
    await flushPromises()

    expect(submit).not.toHaveBeenCalled()
  })
})
