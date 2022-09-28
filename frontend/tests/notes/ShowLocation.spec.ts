/**
 * @jest-environment jsdom
 */

import ShowLocation from "@/components/notes/ShowLocation.vue";
import { screen } from "@testing-library/vue";
import helper from "../helpers";

describe("ShowLocation tests", () => {
  const createTestWrapper = (props: { photoUrl: string; mapUrl: string }) =>
    helper.component(ShowLocation).withStorageProps(props).render();

  it("should show country photo img tag when location data include photo url", async () => {
    const expectedPhotoUrl = "http://test_photo_url/test.jpg";
    createTestWrapper({
      photoUrl: expectedPhotoUrl,
      mapUrl: "",
    });

    const countryPhotoElement = screen.getByTestId(
      "country-photo"
    ) as HTMLImageElement;

    expect(countryPhotoElement.src).toEqual(expectedPhotoUrl);
  });

  it("should hide country photo img tag when location data not include photo url", () => {
    createTestWrapper({
      photoUrl: "",
      mapUrl: "",
    });

    expect(screen.queryByTestId("country-photo")).not.toBeInTheDocument();
  });
});
