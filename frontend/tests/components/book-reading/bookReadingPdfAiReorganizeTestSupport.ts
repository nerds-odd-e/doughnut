import BookReadingPdf from "@/components/book-reading/BookReadingPdf.vue"
import GlobalApiLoadingModal from "@tests/helpers/GlobalApiLoadingModal"
import helper from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { vi } from "vitest"
import { defineComponent } from "vue"

export const mockToast = {
  error: vi.fn(),
}

export const bookReadingPdfStubs = {
  GlobalBar: { template: "<div><slot /></div>" },
  PdfBookViewer: { template: '<div data-testid="pdf-stub" />' },
  ReadingControlPanel: true,
  CurrentBlockNavigationBar: true,
}

export type BookReadingPdfProps = {
  book: ReturnType<typeof makeMe.aBook.please>
  bookPdfBytes: ArrayBuffer
  initialLastRead: null
}

export function bookReadingPdfProps(
  book: BookReadingPdfProps["book"] = makeMe.aBook.notebookId("9").please()
): BookReadingPdfProps {
  return {
    book,
    bookPdfBytes: new ArrayBuffer(0),
    initialLastRead: null,
  }
}

export function mountBookReadingPdf(contentProps: BookReadingPdfProps) {
  return helper
    .component(BookReadingPdf)
    .withRouter()
    .withProps(contentProps)
    .mount({
      global: {
        stubs: bookReadingPdfStubs,
      },
    })
}

export function mountBookReadingWithGlobalModal(
  contentProps: BookReadingPdfProps
) {
  const Host = defineComponent({
    components: { BookReadingPdf, GlobalApiLoadingModal },
    props: {
      contentProps: {
        type: Object as () => BookReadingPdfProps,
        required: true,
      },
    },
    template: `
      <BookReadingPdf v-bind="contentProps" />
      <GlobalApiLoadingModal />
    `,
  })

  return helper
    .component(Host)
    .withRouter()
    .withProps({ contentProps })
    .mount({
      global: {
        stubs: bookReadingPdfStubs,
      },
    })
}

export const loadingModal = () => document.querySelector(".loading-modal-mask")

export async function clickAiReorganize(wrapper: {
  find: (selector: string) => { trigger: (event: string) => Promise<unknown> }
}) {
  await wrapper
    .find('[data-testid="book-reading-ai-reorganize-layout"]')
    .trigger("click")
}
