import type { StorybookConfig } from "@storybook/vue3-vite"
import { fileURLToPath } from "node:url"
import { mergeConfig } from "vite"
import tsconfigPaths from "vite-tsconfig-paths"

const config: StorybookConfig = {
  stories: ["../src/**/*.mdx", "../src/**/*.stories.@(js|jsx|mjs|ts|tsx)"],
  addons: ["@storybook/addon-essentials", "@storybook/addon-interactions"],
  framework: {
    name: "@storybook/vue3-vite",
    options: {},
  },
  async viteFinal(config) {
    return mergeConfig(config, {
      resolve: {
        alias: {
          "@": fileURLToPath(new URL("../src", import.meta.url)),
          "@tests": fileURLToPath(new URL("../tests", import.meta.url)),
          "@generated": fileURLToPath(new URL("../generated", import.meta.url)),
        },
      },
      plugins: [
        tsconfigPaths({
          root: fileURLToPath(new URL("..", import.meta.url)),
        }),
      ],
    })
  },
}
export default config
