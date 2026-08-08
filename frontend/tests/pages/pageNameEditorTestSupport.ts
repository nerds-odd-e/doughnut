import { flushPromises, type VueWrapper } from "@vue/test-utils"

export async function editPageName(
  wrapper: VueWrapper,
  editorDataTest: string,
  name: string,
  blur = true
) {
  const editor = wrapper.get(`[data-test="${editorDataTest}"]`)
  ;(editor.element as HTMLElement).innerText = name
  await editor.trigger("input")
  if (blur) await editor.trigger("blur")
  await flushPromises()
}
