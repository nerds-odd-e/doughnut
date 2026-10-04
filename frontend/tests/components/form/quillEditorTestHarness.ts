import { mount, type VueWrapper } from "@vue/test-utils"
import QuillEditor from "@/components/form/QuillEditor.vue"
import { nextTick } from "vue"
import type Quill from "quill"
import type { Router } from "vue-router"
import { productionRouterAt } from "@tests/helpers"

export function createQuillEditorTestHarness() {
  let router: Router
  let wrapper: VueWrapper

  beforeEach(async () => {
    router = await productionRouterAt({ name: "root" })
  })

  async function mountEditor(
    props: Record<string, unknown> = { modelValue: "" }
  ) {
    wrapper = mount(QuillEditor, {
      props,
      attachTo: document.body,
      global: { plugins: [router] },
    })
    await nextTick()
    return wrapper
  }

  function quillInstance(): Quill {
    // biome-ignore lint/suspicious/noExplicitAny: Quill instance is not part of the public API
    const quill = (wrapper.vm as any).quill as Quill | null
    expect(quill).not.toBeNull()
    return quill!
  }

  async function clickEditorAnchor(selector: string) {
    await vi.waitUntil(() => document.querySelector(selector))
    const anchor = document.querySelector(selector) as HTMLAnchorElement
    anchor.dispatchEvent(
      new MouseEvent("click", { bubbles: true, cancelable: true })
    )
    await nextTick()
  }

  function cleanup() {
    wrapper?.unmount()
    document.body.innerHTML = ""
  }

  return {
    get router() {
      return router
    },
    mountEditor,
    quillInstance,
    clickEditorAnchor,
    cleanup,
  }
}
