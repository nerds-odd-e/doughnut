/**
 * @jest-environment jsdom
 */
import DragListner from "@/components/commons/DragListner.vue";
import { mount } from "@vue/test-utils";

describe("DragListner", () => {

  test("mouse move only", async () => {
    const wrapper = mount(DragListner, {propsData: { modelValue: {x: 10, y: 20}}});
    await wrapper.find("div").trigger("pointermove", {clientX: 100, clientY: 200});
    expect(wrapper.emitted()).not.toHaveProperty('update:modelValue')
  });

  test("mouse down then move", async () => {
    const wrapper = mount(DragListner, {propsData: { modelValue: {x: 10, y: 20}}});
    await wrapper.find("div").trigger("pointerdown", {clientX: 100, clientY: 200})
    await wrapper.find("div").trigger("pointermove", {clientX: 1000, clientY: 2000});
    expect(wrapper.emitted()['update:modelValue'][0][0]).toEqual({x: 910, y: 1820})
    await wrapper.find("div").trigger("pointerup")
    await wrapper.find("div").trigger("pointermove", {clientX: 10000, clientY: 20000});
    expect(wrapper.emitted()['update:modelValue']).toHaveLength(1)
  });

  test("touch move", async () => {
    const wrapper = mount(DragListner, {propsData: { modelValue: {x: 10, y: 20}}});
    await wrapper.find("div").trigger("pointerdown", { clientX: 100, clientY: 200})
    await wrapper.find("div").trigger("touchmove", { changedTouches: [{clientX: 1000, clientY: 2000}]})
    expect(wrapper.emitted()['update:modelValue'][0][0]).toEqual({x: 910, y: 1820})
  });

  test("wheel", async () => {
    const wrapper = mount(DragListner, {propsData: { modelValue: {x: 10, y: 20, scale: 1.5}}});
    await wrapper.find("div").trigger("mousewheel", { clientX: 100, clientY: 200, deltaY: 200})
    expect(wrapper.emitted()['update:modelValue'][0][0]).toEqual({x: -110, y: -220, scale: 3.5})
  });

  test("wheel upper limit", async () => {
    const wrapper = mount(DragListner, {propsData: { modelValue: {x: 10, y: 20, scale: 1.5}}});
    await wrapper.find("div").trigger("mousewheel", { clientX: 100, clientY: 200, deltaY: 2000})
    expect(wrapper.emitted()['update:modelValue'][0][0].scale).toEqual(5)
  });

  test("wheel lower limit", async () => {
    const wrapper = mount(DragListner, {propsData: { modelValue: {x: 10, y: 20, scale: 1.5}}});
    await wrapper.find("div").trigger("mousewheel", { clientX: 100, clientY: 200, deltaY: -2000})
    expect(wrapper.emitted()['update:modelValue'][0][0].scale).toEqual(0.1)
  });

  test("pinch not zooming but move in parallel", async () => {
    const wrapper = mount(DragListner, {propsData: { modelValue: {x: 10, y: 20, scale: 1}}});
    await wrapper.find("div").trigger("pointerdown", {pointerId: 1, clientX: 100, clientY: 200})
    await wrapper.find("div").trigger("pointerdown", {pointerId: 2, clientX: 200, clientY: 400})
    await wrapper.find("div").trigger("pointermove", {pointerId: 1, clientX: 1100, clientY: 2200});
    await wrapper.find("div").trigger("pointermove", {pointerId: 2, clientX: 1200, clientY: 2400});
    expect(wrapper.emitted()['update:modelValue'][1][0]).toEqual({x: 1010, y: 2020, scale: 1})
  });

});
