import GlobalBar from "@/components/toolbars/GlobalBar.vue";
import usePopups, {
  PopupInfo,
} from "../../src/components/commons/Popups/usePopups";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("global bar", () => {
  const popupInfo = [] as PopupInfo[];
  beforeEach(() => {
    usePopups().popups.register({ popupInfo });
  });

  it("opens the circles selection", async () => {
    const wrapper = helper.component(GlobalBar).mount();
    wrapper.find("[role='button']").trigger("click");
    expect(popupInfo).toHaveLength(1);
  });
});
