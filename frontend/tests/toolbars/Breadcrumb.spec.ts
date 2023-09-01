import BreadcrumbMain from "@/components/toolbars/BreadcrumbMain.vue";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("breadcrumb with circles", () => {
  it("render the breadcrumber", async () => {
    const wrapper = helper
      .component(BreadcrumbMain)
      .withStorageProps({})
      .mount();
    expect(wrapper.find(".breadcrumb-item").text()).toEqual("My Notes");
  });
});
