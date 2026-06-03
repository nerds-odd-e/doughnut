import PathNameEditor from "@/components/notes/core/PathNameEditor.vue"
import SeamlessTextEditor from "@/components/form/SeamlessTextEditor.vue"
import { flushPromises, mount } from "@vue/test-utils"
import { describe, expect, it } from "vitest"

function findSeamless(wrapper: ReturnType<typeof mount>) {
  return wrapper.findComponent(SeamlessTextEditor)
}

async function emitEditorValue(
  wrapper: ReturnType<typeof mount>,
  value: string
) {
  findSeamless(wrapper).vm.$emit("update:modelValue", value)
  await flushPromises()
}

async function settlePathNameAutofocus() {
  await flushPromises()
  await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
}

describe("PathNameEditor.vue", () => {
  it("replaces \\ / : with fullwidth forms and emits the sanitized title", async () => {
    const wrapper = mount(PathNameEditor, {
      props: { modelValue: "" },
    })
    await emitEditorValue(wrapper, "a\\b/c:d")
    expect(wrapper.emitted("update:modelValue")?.at(-1)?.[0]).toBe("a＼b／c：d")
  })

  it("shows a replacement warning naming the first illegal character and its fullwidth substitute", async () => {
    const wrapper = mount(PathNameEditor, {
      props: { modelValue: "x" },
    })
    await emitEditorValue(wrapper, "x/y")
    const warn = wrapper.find(".text-warning")
    expect(warn.exists()).toBe(true)
    expect(warn.text()).toContain(
      "'/' was replaced with fullwidth '／' (the only separator between alternative title spellings)"
    )
  })

  it("shows the link warning when the title contains # ^ [ ] or |", async () => {
    const wrapper = mount(PathNameEditor, {
      props: { modelValue: "" },
    })
    await emitEditorValue(wrapper, "see #tag")
    const warn = wrapper.find(".text-warning")
    expect(warn.exists()).toBe(true)
    expect(warn.text()).toContain(
      "Links will not work with names containing any of `#^[]|`"
    )
  })

  it("shows errorMessage instead of warnings when errorMessage is set", async () => {
    const wrapper = mount(PathNameEditor, {
      props: {
        modelValue: "ok",
        errorMessage: "Name already taken",
      },
    })
    await emitEditorValue(wrapper, "bad|name")
    expect(wrapper.find(".text-error").text()).toBe("Name already taken")
    expect(wrapper.find(".text-warning").exists()).toBe(false)
  })

  it("clears the replacement warning on a later edit that introduces no path-illegal characters", async () => {
    const wrapper = mount(PathNameEditor, {
      props: { modelValue: "" },
    })
    await emitEditorValue(wrapper, "a:b")
    expect(wrapper.find(".text-warning").exists()).toBe(true)
    await emitEditorValue(wrapper, "a：bx")
    expect(wrapper.find(".text-warning").exists()).toBe(false)
  })

  it("combines replacement and link warnings when both apply", async () => {
    const wrapper = mount(PathNameEditor, {
      props: { modelValue: "" },
    })
    await emitEditorValue(wrapper, "a|b:c")
    const warn = wrapper.find(".text-warning").text()
    expect(warn).toContain("fullwidth")
    expect(warn).toContain("Links will not work")
  })

  it("does not replace path-illegal characters when readonly", async () => {
    const wrapper = mount(PathNameEditor, {
      props: { modelValue: "x", readonly: true },
    })
    await emitEditorValue(wrapper, "x/y:z")
    expect(wrapper.emitted("update:modelValue")?.at(-1)?.[0]).toBe("x/y:z")
    expect(wrapper.find(".text-warning").exists()).toBe(false)
  })

  it("focuses and selects the editor through the shared autofocus target", async () => {
    const wrapper = mount(PathNameEditor, {
      props: {
        modelValue: "Untitled",
        autofocus: true,
        initialSelectAll: true,
      },
      attachTo: document.body,
    })

    await settlePathNameAutofocus()

    expect(document.activeElement?.classList.contains("seamless-editor")).toBe(
      true
    )
    expect(window.getSelection()?.toString()).toBe("Untitled")
    wrapper.unmount()
  })
})
