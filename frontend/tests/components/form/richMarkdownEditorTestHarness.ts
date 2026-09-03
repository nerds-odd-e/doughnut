import RichMarkdownEditor from "@/components/form/RichMarkdownEditor.vue"
import routes from "@/routes/routes"
import helper from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import {
  createRouter,
  createWebHistory,
  type RouteLocationRaw,
} from "vue-router"

export function createRichMarkdownEditorTestHarness() {
  let wrapper: VueWrapper

  async function setPropertyValueField(
    field: ReturnType<VueWrapper["find"]>,
    text: string
  ) {
    const el = field.element as HTMLElement
    el.textContent = text
    await field.trigger("input")
    await flushPromises()
  }

  function lastEmittedMarkdown(): string {
    const emitted = wrapper.emitted()["update:modelValue"]
    expect(emitted?.length).toBeGreaterThan(0)
    return emitted![emitted!.length - 1]![0] as string
  }

  function lastEmittedPasteComplete(): string {
    const emitted = wrapper.emitted("pasteComplete")
    expect(emitted?.length).toBeGreaterThan(0)
    return emitted![emitted!.length - 1]![0] as string
  }

  function quillComponent() {
    return wrapper.findComponent({ name: "QuillEditor" })
  }

  function quillEditorEl(): HTMLElement {
    return quillComponent().vm.$el.querySelector(".ql-editor") as HTMLElement
  }

  function quillModelHtml(): string {
    return String(quillComponent().props("modelValue"))
  }

  function quillReadonly(): boolean {
    return Boolean(quillComponent().props("readonly"))
  }

  function emitQuillModelValue(html: string) {
    quillComponent().vm.$emit("update:modelValue", html)
  }

  function emitQuillPasteComplete(html: string) {
    quillComponent().vm.$emit("pasteComplete", html)
  }

  async function dispatchPasteHtmlToQuill(html: string) {
    const qlEditor = quillEditorEl()
    qlEditor.focus()
    const clipboardData = new DataTransfer()
    clipboardData.setData("text/html", html)
    qlEditor.dispatchEvent(
      new ClipboardEvent("paste", {
        bubbles: true,
        cancelable: true,
        clipboardData,
      })
    )
    await flushPromises()
  }

  function tapAddProperty() {
    const addBtn = wrapper
      .findAll("button")
      .find((w) => w.text().includes("Add property"))
    expect(addBtn).toBeDefined()
    ;(addBtn!.element as HTMLButtonElement).click()
  }

  function propertyValueFieldElement() {
    const valField = wrapper.find(
      '[data-testid="rich-note-property-row-value-input"]'
    )
    expect(valField.exists()).toBe(true)
    return valField.element as HTMLElement
  }

  function pointerdownPropertyValueField() {
    propertyValueFieldElement().dispatchEvent(
      new PointerEvent("pointerdown", { bubbles: true })
    )
  }

  /** Simulates browser focus after pointerdown primer (vitest has no real touch focus). */
  function completePropertyValueFieldTap() {
    const el = propertyValueFieldElement()
    el.dispatchEvent(new PointerEvent("pointerup", { bubbles: true }))
    el.focus()
  }

  async function openAddProperty() {
    tapAddProperty()
    await flushPromises()
  }

  async function commitInsertProperty(key: string, value: string) {
    await openAddProperty()
    const keyInput = wrapper.find('[data-testid="rich-note-property-key"]')
    const valInput = wrapper.find('[data-testid="rich-note-property-value"]')
    await keyInput.setValue(key)
    await setPropertyValueField(valInput, value)
    await valInput.trigger("blur")
    await flushPromises()
  }

  async function mountEditor(
    initialValue: string,
    options: Record<string, unknown> & {
      attachToBody?: boolean
      route?: RouteLocationRaw
    } = {}
  ) {
    const { attachToBody = false, route, ...props } = options
    const builder = helper.component(RichMarkdownEditor)
    if (route !== undefined) {
      const router = createRouter({
        history: createWebHistory(),
        routes,
      })
      await router.push(route)
      builder.withRouter(router)
    } else {
      builder.withRouter()
    }
    wrapper = builder
      .withProps({
        modelValue: initialValue,
        wikiLinks: [],
        ...props,
      })
      .mount(attachToBody ? { attachTo: document.body } : undefined)
    await flushPromises()
    return wrapper
  }

  function cleanup() {
    wrapper?.unmount()
    document.body.innerHTML = ""
  }

  return {
    mountEditor,
    /** Same instance as the last `mountEditor` return value. */
    getWrapper: () => wrapper,
    cleanup,
    setPropertyValueField,
    lastEmittedMarkdown,
    lastEmittedPasteComplete,
    quillEditorEl,
    quillModelHtml,
    quillReadonly,
    emitQuillModelValue,
    emitQuillPasteComplete,
    dispatchPasteHtmlToQuill,
    tapAddProperty,
    pointerdownPropertyValueField,
    completePropertyValueFieldTap,
    propertyValueFieldElement,
    openAddProperty,
    commitInsertProperty,
  }
}
