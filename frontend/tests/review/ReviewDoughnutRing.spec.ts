import ReviewDoughnutRing from "@/components/review/ReviewDoughnutRing.vue";
import { mount } from "@vue/test-utils";

describe("doughnut ring", () => {
  it("draw the rings", async () => {
    const reviewing = {
      learntCount: 10,
      notLearntCount: 10,
      toRepeatCount: 5,
      toInitialReviewCount: 5,
    } as Generated.ReviewStatus;
    const wrapper = mount(ReviewDoughnutRing, {
      propsData: { reviewing },
    });
    expect(wrapper.find("#initial-curve").attributes("d")).toContain("M0 -40");
    expect(wrapper.find("#initial-curve").attributes("d")).toContain(
      ",0.00 40.00"
    );
    expect(wrapper.find("#repeat-curve").attributes("d")).toContain(
      "M0.00 40.00"
    );
    expect(wrapper.find("#repeat-curve").attributes("d")).toContain(",0 -40");
  });

  it("draw the rings propotionally", async () => {
    const reviewing = {
      learntCount: 10,
      notLearntCount: 10,
      toRepeatCount: 10,
      toInitialReviewCount: 5,
    } as Generated.ReviewStatus;
    const wrapper = mount(ReviewDoughnutRing, {
      propsData: { reviewing },
    });
    expect(wrapper.find("#initial-curve").attributes("d")).toContain("M0 -40");
    expect(wrapper.find("#initial-curve").attributes("d")).toContain(
      ",20.00 34.64"
    );
    expect(wrapper.find("#repeat-curve").attributes("d")).toContain(
      "A40 40 0 1 1,0 -40"
    );
  });

  it("draw the rings when nothing to initial or repeat", async () => {
    const reviewing = {
      learntCount: 0,
      notLearntCount: 0,
      toRepeatCount: 0,
      toInitialReviewCount: 0,
    } as Generated.ReviewStatus;
    const wrapper = mount(ReviewDoughnutRing, {
      propsData: { reviewing },
    });
    expect(wrapper.find("#initial-curve").attributes("d")).toContain("M0 -40");
    expect(wrapper.find("#initial-curve").attributes("d")).toContain(
      ",0.00 40.00"
    );
  });
});
