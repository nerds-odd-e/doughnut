import NotebookAttachedBookSection from "@/components/notebook/NotebookAttachedBookSection.vue"
import GlobalApiLoadingModal from "@tests/helpers/GlobalApiLoadingModal"
import helper from "@tests/helpers"
import { defineComponent } from "vue"

export const notebookId = 77

const NotebookAttachedBookSectionWithGlobalLoading = defineComponent({
  components: { GlobalApiLoadingModal, NotebookAttachedBookSection },
  props: {
    notebookId: { type: Number, required: true },
  },
  template: `
    <NotebookAttachedBookSection :notebook-id="notebookId" />
    <GlobalApiLoadingModal />
  `,
})

export function mountNotebookAttachedBookSection() {
  return helper
    .component(NotebookAttachedBookSectionWithGlobalLoading)
    .withRouter()
    .withProps({ notebookId })
    .mount()
}

export async function selectAttachBookFile(
  wrapper: ReturnType<typeof mountNotebookAttachedBookSection>,
  file: File
) {
  const input = wrapper.find('input[type="file"]').element as HTMLInputElement
  Object.defineProperty(input, "files", {
    value: [file],
    writable: false,
    configurable: true,
  })
  await wrapper.find('input[type="file"]').trigger("change")
}
