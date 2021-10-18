/**
 * @jest-environment jsdom
 */
import DragListner from "@/components/commons/DragListner.vue";
import { mount } from "@vue/test-utils";

describe("DragListner", () => {

  test("mousemove only", async () => {
    const wrapper = mount(DragListner, {propsData: { modelValue: {x: 10, y: 20}}});
    await wrapper.find("div").trigger("mousemove", {clientX: 100, clientY: 200});
    expect(wrapper.emitted()).not.toHaveProperty('update:modelValue')
  });

  test("mouse down then move", async () => {
    const wrapper = mount(DragListner, {propsData: { modelValue: {x: 10, y: 20}}});
    await wrapper.find("div").trigger("mousedown", {clientX: 100, clientY: 200})
    await wrapper.find("div").trigger("mousemove", {clientX: 1000, clientY: 2000});
    expect(wrapper.emitted()['update:modelValue'][0][0]).toEqual({x: 910, y: 1820})
    await wrapper.find("div").trigger("mouseup")
    await wrapper.find("div").trigger("mousemove", {clientX: 10000, clientY: 20000});
    expect(wrapper.emitted()['update:modelValue']).toHaveLength(1)
  });

  test("touch move", async () => {
    const wrapper = mount(DragListner, {propsData: { modelValue: {x: 10, y: 20}}});
    await wrapper.find("div").trigger("touchstart", { changedTouches: [{clientX: 100, clientY: 200}]})
    await wrapper.find("div").trigger("touchmove", { changedTouches: [{clientX: 1000, clientY: 2000}]})
    expect(wrapper.emitted()['update:modelValue'][0][0]).toEqual({x: 910, y: 1820})
  });

});
