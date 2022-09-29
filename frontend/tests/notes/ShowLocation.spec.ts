/**
 * @jest-environment jsdom
 */

import ShowLocation from "@/components/notes/ShowLocation.vue";
import { screen } from "@testing-library/vue";
import helper from "../helpers";

describe("ShowLocation tests", () => {
  const LOCATION_PHOTO_ALT = "location photo";
  const LOCATION_MAP_ALT = "location map";
  const createTestWrapper = (props: { photoUrl?: string; mapUrl?: string }) =>
    helper.component(ShowLocation).withStorageProps(props).render();

  function getLocationPhotoElement(): HTMLImageElement {
    return screen.getByAltText(LOCATION_PHOTO_ALT) as HTMLImageElement;
  }

  function findLocationPhotoElement(): HTMLImageElement {
    return screen.queryByAltText(LOCATION_PHOTO_ALT) as HTMLImageElement;
  }

  function getLocationMapElement(): HTMLImageElement {
    return screen.getByAltText(LOCATION_MAP_ALT) as HTMLImageElement;
  }

  function findLocationMapElement(): HTMLImageElement {
    return screen.queryByAltText(LOCATION_MAP_ALT) as HTMLImageElement;
  }

  it("should show location photo img tag when location data include photo url", async () => {
    const expectedPhotoUrl = "http://test_photo_url/photo.jpg";

    createTestWrapper({
      photoUrl: expectedPhotoUrl,
    });

    expect(getLocationPhotoElement().src).toEqual(expectedPhotoUrl);
  });

  it("should hide location photo img tag when location data not include photo url", () => {
    createTestWrapper({});

    expect(findLocationPhotoElement()).not.toBeInTheDocument();
  });

  it("should show location map when location data include map url", async () => {
    const expectedMapUrl = "http://test_map_url/map.jpg";
    createTestWrapper({
      mapUrl: expectedMapUrl,
    });

    expect(getLocationMapElement().src).toEqual(expectedMapUrl);
  });

  it("should hide location map when location data not include map url", () => {
    createTestWrapper({});

    expect(findLocationMapElement()).not.toBeInTheDocument();
  });
});
