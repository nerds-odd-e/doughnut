import type { Config } from "tailwindcss"
import daisyui from "daisyui"

// Define a proper interface for DaisyUI configuration
interface DaisyUIConfig {
  themes: string[]
  darkTheme: string
  base: boolean
  styled: boolean
  utils: boolean
  themeRoot: string
  logs: boolean
  prefix?: string
}

// Define a type for the extended Tailwind config
interface ExtendedConfig extends Config {
  daisyui?: DaisyUIConfig
  prefix?: string
}

const config: ExtendedConfig = {
  content: ["./index.html", "./src/**/*.{vue,js,ts,jsx,tsx}"],
  prefix: "daisy",
  theme: {
    extend: {},
  },
  // biome-ignore lint/suspicious/noExplicitAny: DaisyUI plugin type is complex
  plugins: [daisyui as any],
  daisyui: {
    themes: ["light", "dark"],
    darkTheme: "dark",
    base: true,
    styled: true,
    utils: true,
    themeRoot: ":root",
    logs: false,
    prefix: "daisy",
  },
}

export default config
