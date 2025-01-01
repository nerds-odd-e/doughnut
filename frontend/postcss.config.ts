import tailwindcss from "tailwindcss"
import autoprefixer from "autoprefixer"
import type { Config } from "postcss-load-config"

const config: Config = {
  plugins: [tailwindcss(), autoprefixer()],
}

export default config
