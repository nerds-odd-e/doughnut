import { screen } from "@testing-library/vue";
import Breadcrumb from "@/components/toolbars/Breadcrumb.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("breadcrumb with circles", () => {
  it("render the breadcrumber", async () => {
    const notePosition = makeMe.aNotePosition.please();
    const wrapper = helper
      .component(Breadcrumb)
      .withProps({ notePosition })
      .mount();
    expect(wrapper.find(".breadcrumb-item").text()).toEqual("My Notes");
  });

  it("view note belongs to other people in bazaar", async () => {
    const notePosition = makeMe.aNotePosition.inBazaar().please();
    helper.component(Breadcrumb).withProps({ notePosition }).render();
    await screen.findByText("Bazaar");
  });
});
