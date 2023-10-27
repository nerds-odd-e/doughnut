import { VueWrapper } from "@vue/test-utils";

const matchByText = (wrapper: VueWrapper, reg: RegExp, selector: string) => {
  const btns = wrapper
    .findAll(selector)
    .filter((node) => node.text().match(reg));
  return btns.length === 1 ? btns[0] : undefined;
};

export default matchByText;
