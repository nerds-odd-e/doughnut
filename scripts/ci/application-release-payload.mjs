import { statSync } from 'node:fs'
import { join } from 'node:path'

const { DEPLOY_JAR_PATH, FRONTEND_STATIC_DIR } = process.env
for (const path of [DEPLOY_JAR_PATH, join(FRONTEND_STATIC_DIR, 'index.html')]) {
  const file = statSync(path)
  if (!file.isFile() || file.size === 0) {
    throw new Error(`Release payload requires a nonempty file: ${path}`)
  }
}
console.log('Complete application release payload validated')
