import type { Config } from "tailwindcss"
import daisyui from "daisyui"

export default {
  content: ["./index.html", "./src/**/*.{vue,js,ts,jsx,tsx}"],
  prefix: "daisy-",
  theme: {
    extend: {},
  },
  plugins: [daisyui],
  daisyui: {
    // Let's use dark theme while we are converting to daisyui.
    // Dark theme helps to see where in the current design need to be changed.
    themes: ["dark"],
  },
} satisfies Config
