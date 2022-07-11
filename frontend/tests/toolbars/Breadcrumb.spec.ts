/**
 * @jest-environment jsdom
 */
import BreadcrumbWithCircle from "@/components/toolbars/BreadcrumbWithCircle.vue";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("breadcrumb with circles", () => {
  it("render the breadcrumber", async () => {
    const wrapper = helper
      .component(BreadcrumbWithCircle)
      .withProps({})
      .mount();
    expect(wrapper.find(".breadcrumb-item").text()).toEqual("My Notes");
  });

  it("opens the circles selection", async () => {
    const wrapper = helper
      .component(BreadcrumbWithCircle)
      .withProps({})
      .mount();
    wrapper.find("[role='button']").trigger("click");
    expect(wrapper.emitted("open-circle-selector")).toBeTruthy();
  });
});
