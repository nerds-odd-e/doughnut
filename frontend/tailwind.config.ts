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
    themes: false,
  },
} satisfies Config
