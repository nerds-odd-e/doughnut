import { useBookReadingBootstrap } from "@/composables/useBookReadingBootstrap"
import type { PdfLocatorFull } from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { flushPromises, mount, type VueWrapper } from "@vue/test-utils"
import { defineComponent } from "vue"
import { expect, vi } from "vitest"

export const notebookId = 7

export const fakePdfBytes = new Uint8Array([0x25, 0x50, 0x44, 0x46]).buffer
export const fakeEpubBytes = new Uint8Array([0x50, 0x4b, 0x03, 0x04]).buffer

export function mockGetBookPdf() {
  return mockSdkService(
    NotebookBooksController,
    "getBook",
    makeMe.aBook.notebookId(String(notebookId)).please()
  )
}

export function mockGetBookEpub() {
  return mockSdkService(
    NotebookBooksController,
    "getBook",
    makeMe.aBook
      .notebookId(String(notebookId))
      .format("epub")
      .blocks([])
      .please()
  )
}

export function mockReadingPositionWithPdfLocator(options?: {
  pageIndex?: number
  bboxTop?: number
  selectedBookBlockId?: number
}) {
  return vi
    .spyOn(NotebookBooksController, "getNotebookBookReadingPosition")
    .mockResolvedValue(
      wrapSdkResponse({
        id: 1,
        locator: {
          type: "PdfLocator_Full",
          pageIndex: options?.pageIndex ?? 2,
          bbox: [0, options?.bboxTop ?? 750, 100, 600],
        } satisfies PdfLocatorFull,
        selectedBookBlockId: options?.selectedBookBlockId ?? 42,
      }) as Awaited<
        ReturnType<
          typeof NotebookBooksController.getNotebookBookReadingPosition
        >
      >
    )
}

export function mockNoReadingPosition() {
  return mockSdkService(
    NotebookBooksController,
    "getNotebookBookReadingPosition",
    undefined
  )
}

export function mockFetchBytes(bytes: ArrayBuffer, contentType: string) {
  return vi.spyOn(globalThis, "fetch").mockResolvedValue(
    new Response(bytes, {
      status: 200,
      headers: { "Content-Type": contentType },
    })
  )
}

export function mockFetchNotFound() {
  return vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValue(new Response(null, { status: 404 }))
}

export function mountBootstrap() {
  const Root = defineComponent({
    props: { nid: { type: Number, required: true } },
    setup(props: { nid: number }) {
      return useBookReadingBootstrap(props.nid)
    },
    template: "<div />",
  })
  return mount(Root, { props: { nid: notebookId } })
}

export async function waitForBootstrap<T>(wrapper: VueWrapper): Promise<T> {
  type Vm = { bootstrap: T | null }
  for (let attempt = 0; attempt < 40; attempt++) {
    await flushPromises()
    const vm = wrapper.vm as unknown as Vm
    if (vm.bootstrap !== null) {
      return vm.bootstrap
    }
    await wrapper.vm.$nextTick()
  }
  const vm = wrapper.vm as unknown as Vm
  expect(vm.bootstrap).not.toBeNull()
  return vm.bootstrap as T
}
