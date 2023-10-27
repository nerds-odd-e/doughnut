const scrollToElement = (elm: HTMLElement) => {
  elm.scrollIntoView({ behavior: "smooth" as const });
};

export default scrollToElement;
