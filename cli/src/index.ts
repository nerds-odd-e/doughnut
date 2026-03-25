import { main } from './main.js'

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
