import { mount } from "@vue/test-utils";
import DragListner from "@/components/commons/DragListner.vue";

describe("DragListner", () => {
  const currentTarget = {
    getBoundingClientRect: vi
      .fn()
      .mockReturnValue({ width: 1000, height: 1000, top: 0 }),
  };

  it("mouse move only", async () => {
    const wrapper = mount(DragListner, {
      propsData: { modelValue: { x: 10, y: 20 } },
    });
    await wrapper
      .find("div")
      .trigger("pointermove", { clientX: 100, clientY: 200, currentTarget });
    expect(wrapper.emitted()).not.toHaveProperty("update:modelValue");
  });

  it("mouse down then move", async () => {
    const wrapper = mount(DragListner, {
      propsData: { modelValue: { x: 10, y: 20 } },
    });
    await wrapper
      .find("div")
      .trigger("pointerdown", { clientX: 100, clientY: 200 });
    await wrapper
      .find("div")
      .trigger("pointermove", { clientX: 1000, clientY: 2000 });
    const updated = wrapper.emitted()["update:modelValue"] as unknown[][];
    expect(updated[0]![0]).toEqual({
      x: 910,
      y: 1820,
    });
    await wrapper.find("div").trigger("pointerup");
    await wrapper.find("div").trigger("pointermove", {
      clientX: 10000,
      clientY: 20000,
      currentTarget,
    });
    expect(wrapper.emitted()["update:modelValue"]).toHaveLength(1);
  });

  it("touch move", async () => {
    const wrapper = mount(DragListner, {
      propsData: { modelValue: { x: 10, y: 20 } },
    });
    await wrapper
      .find("div")
      .trigger("pointerdown", { clientX: 100, clientY: 200 });
    await wrapper.find("div").trigger("touchmove", {
      changedTouches: [{ clientX: 1000, clientY: 2000, currentTarget }],
    });
    const updated = wrapper.emitted()["update:modelValue"] as unknown[][];
    expect(updated[0]![0]).toEqual({
      x: 910,
      y: 1820,
    });
  });

  it("wheel", async () => {
    const wrapper = mount(DragListner, {
      propsData: { modelValue: { x: 10, y: 20, scale: 1.5 } },
    });
    await wrapper
      .find("div")
      .trigger("mousewheel", { clientX: 100, clientY: 200, deltaY: 200 });
    const updated = wrapper.emitted()["update:modelValue"] as unknown[][];
    expect(updated[0]![0]).toEqual({
      x: -110,
      y: -220,
      scale: 3.5,
    });
  });

  it("wheel upper limit", async () => {
    const wrapper = mount(DragListner, {
      propsData: { modelValue: { x: 10, y: 20, scale: 1.5 } },
    });
    await wrapper
      .find("div")
      .trigger("mousewheel", { clientX: 100, clientY: 200, deltaY: 2000 });
    const updated = wrapper.emitted()["update:modelValue"] as unknown[][];
    expect(updated[0]![0]).toMatchObject({ scale: 5 });
  });

  it("wheel lower limit", async () => {
    const wrapper = mount(DragListner, {
      propsData: { modelValue: { x: 10, y: 20, scale: 1.5 } },
    });
    await wrapper
      .find("div")
      .trigger("mousewheel", { clientX: 100, clientY: 200, deltaY: -2000 });
    const updated = wrapper.emitted()["update:modelValue"] as unknown[][];
    expect(updated[0]![0]).toMatchObject({ scale: 0.1 });
  });

  it("pinch not zooming but move in parallel", async () => {
    const wrapper = mount(DragListner, {
      propsData: { modelValue: { x: 10, y: 20, scale: 1, rotate: 0 } },
    });
    await wrapper
      .find("div")
      .trigger("pointerdown", { pointerId: 1, clientX: 100, clientY: 200 });
    await wrapper
      .find("div")
      .trigger("pointerdown", { pointerId: 2, clientX: 200, clientY: 400 });
    await wrapper.find("div").trigger("pointermove", {
      pointerId: 1,
      clientX: 1100,
      clientY: 2200,
      currentTarget,
    });
    await wrapper.find("div").trigger("pointermove", {
      pointerId: 2,
      clientX: 1200,
      clientY: 2400,
      currentTarget,
    });
    const updated: Array<Array<any>> = wrapper.emitted()[
      "update:modelValue"
    ] as Array<Array<any>>;
    expect(updated[1]![0]).toEqual({
      x: 1010,
      y: 2020,
      scale: 1,
      rotate: 0,
    });
  });
});
