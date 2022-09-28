/**
 * @jest-environment jsdom
 */

import ShowLocation from "@/components/notes/ShowLocation.vue";
import helper from "../helpers";

describe("ShowLocation tests", () => {
  const createTestWrapper = (props: { photoUrl: string; mapUrl: string }) =>
    helper.component(ShowLocation).withStorageProps(props).mount();

  it("should show country photo img tag when location data include photo url", () => {
    const expectedPhotoUrl = "http://test_photo_url/test.jpg";
    const wrapper = createTestWrapper({
      photoUrl: expectedPhotoUrl,
      mapUrl: "",
    });

    const countryPhotoImg = wrapper.get("#country-photo")
      .element as HTMLImageElement;
    expect(countryPhotoImg.src).toEqual(expectedPhotoUrl);
  });

  it("should hide country photo img tag when location data not include photo url", () => {
    const wrapper = createTestWrapper({
      photoUrl: "",
      mapUrl: "",
    });

    expect(wrapper.find("#country-photo").exists()).toBeFalsy();
  });
});
