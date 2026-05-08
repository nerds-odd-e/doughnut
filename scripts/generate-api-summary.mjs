import { readFile, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { parse } from 'yaml'

const httpMethods = new Set([
  'delete',
  'get',
  'head',
  'options',
  'patch',
  'post',
  'put',
  'trace',
])

const defaultInputPath = path.resolve('open_api_docs.yaml')
const defaultOutputPath = path.resolve(
  'packages/generated/doughnut-backend-api/api-summary.md'
)

export async function generateApiSummary({
  inputPath = defaultInputPath,
  outputPath = defaultOutputPath,
} = {}) {
  const openApi = parse(await readFile(inputPath, 'utf8'))
  await writeFile(outputPath, renderApiSummary(openApi, inputPath), 'utf8')
}

export function renderApiSummary(openApi, inputPath = defaultInputPath) {
  const groups = collectOperations(openApi)
  const sourceName = path.basename(inputPath)
  const lines = [
    '# Doughnut Backend API Summary',
    '',
    `Generated from \`${sourceName}\` by \`scripts/generate-api-summary.mjs\`.`,
    'Use this for endpoint lookup; open `sdk.gen.ts` or `types.gen.ts` only for exact generated signatures.',
    '',
  ]

  for (const [tag, operations] of groups) {
    lines.push(`## ${humanizeTag(tag)}`, '')
    for (const operation of operations) {
      lines.push(renderOperationLine(operation))
    }
    lines.push('')
  }

  return `${lines
    .join('\n')
    .replace(/\n{3,}/g, '\n\n')
    .trimEnd()}\n`
}

function collectOperations(openApi) {
  const groups = new Map()
  for (const [endpointPath, pathItem] of Object.entries(openApi.paths ?? {})) {
    for (const [method, operation] of Object.entries(pathItem ?? {})) {
      if (!httpMethods.has(method)) continue

      const tag = operation.tags?.[0] ?? 'untagged'
      if (!groups.has(tag)) groups.set(tag, [])
      groups.get(tag).push({ endpointPath, method, operation })
    }
  }
  return groups
}

function renderOperationLine({ endpointPath, method, operation }) {
  const operationId = operation.operationId ?? `${method} ${endpointPath}`
  const typePrefix = toPascalCase(operationId)
  const responseType = `${typePrefix}Response`
  const request = requestDescription(operation, typePrefix)
  const body = requestBodyType(operation)
  const response = responseSchemaType(operation)
  const details = [
    `request: ${request}`,
    body ? `body: ${body}` : undefined,
    response === responseType ? undefined : `response body: ${response}`,
  ].filter(Boolean)

  return `- \`${operationId}\`: ${method.toUpperCase()} \`${endpointPath}\` -> \`${responseType}\` (${details.join('; ')})`
}

function requestDescription(operation, typePrefix) {
  const body = requestBodyType(operation)
  const pathParams = parameterNames(operation, 'path')
  const queryParams = parameterNames(operation, 'query')
  const parts = []
  if (pathParams.length > 0) parts.push(`path: ${pathParams.join(', ')}`)
  if (queryParams.length > 0) parts.push(`query: ${queryParams.join(', ')}`)

  if (!body && parts.length === 0) return 'none'
  if (parts.length === 0) return `\`${typePrefix}Data\``
  return `\`${typePrefix}Data\`; ${parts.join('; ')}`
}

function requestBodyType(operation) {
  const schema = preferredContentSchema(operation.requestBody?.content)
  return schema ? schemaType(schema) : undefined
}

function responseSchemaType(operation) {
  const responses = operation.responses ?? {}
  const status = preferredSuccessStatus(Object.keys(responses))
  if (!status) return 'void'

  const schema = preferredContentSchema(responses[status]?.content)
  return schema ? schemaType(schema) : 'void'
}

function preferredSuccessStatus(statuses) {
  const successful = statuses
    .filter((status) => /^2\d\d$/.test(status))
    .sort((a, b) => Number(a) - Number(b))
  return successful.includes('200') ? '200' : successful[0]
}

function preferredContentSchema(content) {
  if (!content) return
  const preferred =
    content['application/json'] ??
    content['*/*'] ??
    Object.values(content).find((entry) => entry?.schema)
  return preferred?.schema
}

function parameterNames(operation, location) {
  return (operation.parameters ?? [])
    .filter((parameter) => parameter.in === location)
    .map((parameter) => parameter.name)
}

function schemaType(schema) {
  if (!schema) return 'unknown'
  if (schema.$ref) return toPascalCase(schema.$ref.split('/').pop())
  if (schema.oneOf) return schema.oneOf.map(schemaType).join(' | ')
  if (schema.anyOf) return schema.anyOf.map(schemaType).join(' | ')
  if (schema.allOf) return schema.allOf.map(schemaType).join(' & ')
  if (schema.type === 'array') return `Array<${schemaType(schema.items)}>`
  if (Array.isArray(schema.type)) return schema.type.join(' | ')
  if (schema.type === 'integer') return 'number'
  if (schema.type === 'object' && schema.additionalProperties) {
    return `Record<string, ${schemaType(schema.additionalProperties)}>`
  }
  return schema.type ?? 'unknown'
}

function humanizeTag(tag) {
  return words(tag)
    .map((word) => capitalize(word))
    .join(' ')
}

function toPascalCase(value) {
  return words(value)
    .map((word) => capitalize(word.toLowerCase()))
    .join('')
}

function words(value) {
  return (
    String(value)
      .replace(/[^A-Za-z0-9]+/g, ' ')
      .match(/[A-Z]+(?=[A-Z][a-z]|\d|\b)|[A-Z]?[a-z]+|\d+/g) ?? []
  )
}

function capitalize(value) {
  return `${value.charAt(0).toUpperCase()}${value.slice(1)}`
}

if (import.meta.url === `file://${process.argv[1]}`) {
  await generateApiSummary({
    inputPath: process.argv[2]
      ? path.resolve(process.argv[2])
      : defaultInputPath,
    outputPath: process.argv[3]
      ? path.resolve(process.argv[3])
      : defaultOutputPath,
  })
}
