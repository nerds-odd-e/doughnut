import { readFileSync } from 'node:fs'
import { parse } from 'yaml'

const config = (path) =>
  parse(readFileSync(new URL(path, import.meta.url), 'utf8'))

export const workflow = (name) => config(`../../.github/workflows/${name}.yml`)
export const action = (name) => config(`../../.github/${name}/action.yml`)
