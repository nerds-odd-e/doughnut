import type { StorybookConfig } from "@storybook/vue3-vite"
import { mergeConfig } from "vite"
import tsconfigPaths from "vite-tsconfig-paths"
import { fileURLToPath } from "node:url"
import path from "path"

const config: StorybookConfig = {
  stories: ["../src/**/*.mdx", "../src/**/*.stories.@(js|jsx|mjs|ts|tsx)"],
  addons: ["@storybook/addon-essentials", "@storybook/addon-interactions"],
  framework: {
    name: "@storybook/vue3-vite",
    options: {},
  },
  async viteFinal(config) {
    // Storybook automatically merges with vite.config.ts, but we need to ensure
    // tsconfigPaths plugin is present to resolve @generated/* from tsconfig.json
    const frontendDir = fileURLToPath(new URL("..", import.meta.url))

    return mergeConfig(config, {
      plugins: [
        // Ensure tsconfigPaths is present to resolve path aliases from tsconfig.json
        // This will handle @generated/* automatically based on tsconfig.json paths
        tsconfigPaths({
          root: frontendDir,
        }),
      ],
    })
  },
}
export default config
