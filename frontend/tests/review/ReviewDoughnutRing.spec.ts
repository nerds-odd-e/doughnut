/**
 * @jest-environment jsdom
 */
import ReviewDoughnutRing from "@/components/review/ReviewDoughnutRing.vue";
import helper from "../helpers";

describe("doughnut ring", () => {
  it("draw the rings", async () => {
    const reviewing = {
      learntCount: 10,
      notLearntCount: 10,
      toRepeatCount: 5,
      toInitialReviewCount: 5,
    } as Generated.ReviewStatus;
    const wrapper = helper
      .component(ReviewDoughnutRing)
      .withProps({ reviewing })
      .mount();
    expect(wrapper.find("#initial-curve").attributes("d")).toContain("M0 -40");
    expect(wrapper.find("#initial-curve").attributes("d")).toContain(",0 40");
    expect(wrapper.find("#repeat-curve").attributes("d")).toContain("M0 40");
    expect(wrapper.find("#repeat-curve").attributes("d")).toContain(",0 -40");
  });

  it("draw the rings propotionally", async () => {
    const reviewing = {
      learntCount: 10,
      notLearntCount: 10,
      toRepeatCount: 10,
      toInitialReviewCount: 5,
    } as Generated.ReviewStatus;
    const wrapper = helper
      .component(ReviewDoughnutRing)
      .withProps({ reviewing })
      .mount();
    expect(wrapper.find("#initial-curve").attributes("d")).toContain("M0 -40");
    expect(wrapper.find("#initial-curve").attributes("d")).toContain(",0 40");
    expect(wrapper.find("#repeat-curve").attributes("d")).toContain("M0 40");
    expect(wrapper.find("#repeat-curve").attributes("d")).toContain(",0 -40");
  });
});
