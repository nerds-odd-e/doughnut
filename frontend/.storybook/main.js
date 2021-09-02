module.exports = {
  stories: ["../src/**/*.stories.mdx", "../src/**/*.stories.@(js|jsx|ts|tsx)"],
  addons: [
    "@storybook/addon-links",
    "@storybook/preset-scss",
    "@storybook/addon-essentials",
  ],
  options: {
    sassOptions: {
      indentedSyntax: true,
    },
  },
};
