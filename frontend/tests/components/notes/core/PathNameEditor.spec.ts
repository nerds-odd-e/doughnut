import PathNameEditor from "@/components/notes/core/PathNameEditor.vue"
import SeamlessTextEditor from "@/components/form/SeamlessTextEditor.vue"
import { flushPromises, mount } from "@vue/test-utils"
import { afterEach, describe, expect, it } from "vitest"

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

describe("PathNameEditor.vue", () => {
  afterEach(() => {
    document.body.innerHTML = ""
  })

  it("replaces \\ / : with fullwidth forms and emits the sanitized title", async () => {
    const wrapper = mount(PathNameEditor, {
      props: { modelValue: "" },
      attachTo: document.body,
    })
    await emitEditorValue(wrapper, "a\\b/c:d")
    expect(wrapper.emitted("update:modelValue")?.at(-1)?.[0]).toBe("a＼b／c：d")
    wrapper.unmount()
  })

  it("shows a replacement warning naming the first illegal character and its fullwidth substitute", async () => {
    const wrapper = mount(PathNameEditor, {
      props: { modelValue: "x" },
      attachTo: document.body,
    })
    await emitEditorValue(wrapper, "x/y")
    const warn = wrapper.find(".daisy-text-warning")
    expect(warn.exists()).toBe(true)
    expect(warn.text()).toContain(
      "'/' was replaced with fullwidth '／' (the only separator between alternative title spellings)"
    )
    wrapper.unmount()
  })

  it("shows the link warning when the title contains # ^ [ ] or |", async () => {
    const wrapper = mount(PathNameEditor, {
      props: { modelValue: "" },
      attachTo: document.body,
    })
    await emitEditorValue(wrapper, "see #tag")
    const warn = wrapper.find(".daisy-text-warning")
    expect(warn.exists()).toBe(true)
    expect(warn.text()).toContain(
      "Links will not work with names containing any of `#^[]|`"
    )
    wrapper.unmount()
  })

  it("shows errorMessage instead of warnings when errorMessage is set", async () => {
    const wrapper = mount(PathNameEditor, {
      props: {
        modelValue: "ok",
        errorMessage: "Name already taken",
      },
      attachTo: document.body,
    })
    await emitEditorValue(wrapper, "bad|name")
    expect(wrapper.find(".daisy-text-error").text()).toBe("Name already taken")
    expect(wrapper.find(".daisy-text-warning").exists()).toBe(false)
    wrapper.unmount()
  })

  it("clears the replacement warning on a later edit that introduces no path-illegal characters", async () => {
    const wrapper = mount(PathNameEditor, {
      props: { modelValue: "" },
      attachTo: document.body,
    })
    await emitEditorValue(wrapper, "a:b")
    expect(wrapper.find(".daisy-text-warning").exists()).toBe(true)
    await emitEditorValue(wrapper, "a：bx")
    expect(wrapper.find(".daisy-text-warning").exists()).toBe(false)
    wrapper.unmount()
  })

  it("combines replacement and link warnings when both apply", async () => {
    const wrapper = mount(PathNameEditor, {
      props: { modelValue: "" },
      attachTo: document.body,
    })
    await emitEditorValue(wrapper, "a|b:c")
    const warn = wrapper.find(".daisy-text-warning").text()
    expect(warn).toContain("fullwidth")
    expect(warn).toContain("Links will not work")
    wrapper.unmount()
  })

  it("does not replace path-illegal characters when readonly", async () => {
    const wrapper = mount(PathNameEditor, {
      props: { modelValue: "x", readonly: true },
      attachTo: document.body,
    })
    await emitEditorValue(wrapper, "x/y:z")
    expect(wrapper.emitted("update:modelValue")?.at(-1)?.[0]).toBe("x/y:z")
    expect(wrapper.find(".daisy-text-warning").exists()).toBe(false)
    wrapper.unmount()
  })
})
