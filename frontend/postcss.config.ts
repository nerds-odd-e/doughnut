import tailwindcssPlugin from "@tailwindcss/postcss"
import autoprefixer from "autoprefixer"
import type { Config } from "postcss-load-config"

const config: Config = {
  plugins: {
    tailwindcss: tailwindcssPlugin,
    autoprefixer,
  },
}

export default config
