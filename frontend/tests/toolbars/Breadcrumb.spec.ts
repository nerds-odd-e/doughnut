import Breadcrumb from "@/components/toolbars/Breadcrumb.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("breadcrumb with circles", () => {
  it("render the breadcrumber", async () => {
    const notePosition = makeMe.aNotePosition.please();
    const wrapper = helper
      .component(Breadcrumb)
      .withProps(notePosition)
      .mount();
    expect(wrapper.find(".breadcrumb-item").text()).toEqual("My Notes");
  });
});
