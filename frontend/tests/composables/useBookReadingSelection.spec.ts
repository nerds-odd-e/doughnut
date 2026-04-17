import { useBookReadingSelection } from "@/composables/useBookReadingSelection"
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

describe("useBookReadingSelection", () => {
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
    initialSelectedBlockId?: number | null
  }) {
    const currentBlockId = ref<number | null>(null)
    const bookBlocks = computed(() => options.blocks)
    const onAdvance = vi.fn().mockResolvedValue(undefined)

    const Root = defineComponent({
      setup() {
        useBookReadingSelection({
          bookBlocks,
          currentBlockId,
          hasRecordedDisposition: options.hasRecordedDisposition,
          submitReadingDisposition: options.submitReadingDisposition,
          onAdvance,
          initialSelectedBlockId: options.initialSelectedBlockId ?? null,
        })
        return { currentBlockId }
      },
      template: "<div />",
    })

    const wrapper = mount(Root)
    return { wrapper, currentBlockId, onAdvance }
  }

  it("auto-mark: does not call submit when predecessor has direct content (multiple locators)", async () => {
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

  it("auto-mark: does not call submit when predecessor has one locator but already has a disposition", async () => {
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

  it("auto-mark: calls submit with READ when predecessor has one locator and no record", async () => {
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

  it("auto-mark: does not call submit when predecessor has empty contentLocators", async () => {
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

  it("mark disposition advances via onAdvance to the next block when present", async () => {
    const submit = vi.fn().mockResolvedValue(true)
    const b1 = stubBlock(1, [epubLoc("a.xhtml")])
    const b2 = stubBlock(2, [epubLoc("b.xhtml")])
    const onAdvance = vi.fn().mockResolvedValue(undefined)

    const Root = defineComponent({
      setup() {
        const currentBlockId = ref<number | null>(2)
        const bookBlocks = computed(() => [b1, b2])
        const { markSelectedBlockDisposition } = useBookReadingSelection({
          bookBlocks,
          currentBlockId,
          hasRecordedDisposition: () => false,
          submitReadingDisposition: submit,
          onAdvance,
          initialSelectedBlockId: 1,
        })
        return { markSelectedBlockDisposition }
      },
      template: "<div />",
    })

    const wrapper = mount(Root)
    const vm = wrapper.vm as {
      markSelectedBlockDisposition: (
        s: BookBlockReadingDisposition
      ) => Promise<void>
    }
    await vm.markSelectedBlockDisposition("READ")
    await flushPromises()

    expect(submit).toHaveBeenCalledWith(1, "READ")
    expect(onAdvance).toHaveBeenCalledTimes(1)
    expect(onAdvance).toHaveBeenCalledWith(b2)
  })
})
