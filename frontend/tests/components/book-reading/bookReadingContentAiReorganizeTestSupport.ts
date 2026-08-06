import BookReadingContent from "@/components/book-reading/BookReadingContent.vue"
import GlobalApiLoadingModal from "@tests/helpers/GlobalApiLoadingModal"
import helper from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { vi } from "vitest"
import { defineComponent } from "vue"

export const mockToast = {
  error: vi.fn(),
}

export const bookReadingContentStubs = {
  GlobalBar: { template: "<div><slot /></div>" },
  PdfBookViewer: { template: '<div data-testid="pdf-stub" />' },
  ReadingControlPanel: true,
  CurrentBlockNavigationBar: true,
}

export type BookReadingContentProps = {
  book: ReturnType<typeof makeMe.aBook.please>
  bookPdfBytes: ArrayBuffer
  initialLastRead: null
}

export function bookReadingContentProps(
  book: BookReadingContentProps["book"] = makeMe.aBook.notebookId("9").please()
): BookReadingContentProps {
  return {
    book,
    bookPdfBytes: new ArrayBuffer(0),
    initialLastRead: null,
  }
}

export function mountBookReadingContent(contentProps: BookReadingContentProps) {
  return helper
    .component(BookReadingContent)
    .withRouter()
    .withProps(contentProps)
    .mount({
      global: {
        stubs: bookReadingContentStubs,
      },
    })
}

export function mountBookReadingWithGlobalModal(
  contentProps: BookReadingContentProps
) {
  const Host = defineComponent({
    components: { BookReadingContent, GlobalApiLoadingModal },
    props: {
      contentProps: {
        type: Object as () => BookReadingContentProps,
        required: true,
      },
    },
    template: `
      <BookReadingContent v-bind="contentProps" />
      <GlobalApiLoadingModal />
    `,
  })

  return helper
    .component(Host)
    .withRouter()
    .withProps({ contentProps })
    .mount({
      global: {
        stubs: bookReadingContentStubs,
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
