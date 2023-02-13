import BreadcrumbMain from "@/components/toolbars/BreadcrumbMain.vue";
import usePopups, {
  PopupInfo,
} from "../../src/components/commons/Popups/usePopups";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("breadcrumb with circles", () => {
  const popupInfo = [] as PopupInfo[];
  beforeEach(() => {
    usePopups().popups.register({ popupInfo });
  });
  it("render the breadcrumber", async () => {
    const wrapper = helper
      .component(BreadcrumbMain)
      .withStorageProps({})
      .mount();
    expect(wrapper.find(".breadcrumb-item").text()).toEqual("My Notes");
  });

  it("opens the circles selection", async () => {
    const wrapper = helper
      .component(BreadcrumbMain)
      .withStorageProps({})
      .mount();
    wrapper.find("[role='button']").trigger("click");
    expect(popupInfo).toHaveLength(1);
  });
});
