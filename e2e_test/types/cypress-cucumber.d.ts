declare module '@badeball/cypress-cucumber-preprocessor' {
  export interface DataTable {
    hashes(): Record<string, string>[]
  }

  type StepCallback<T extends unknown[]> = (...args: T) => void | Promise<void>

  export function Given<T extends unknown[]>(
    pattern: string,
    callback: StepCallback<T>
  ): void
  export function When<T extends unknown[]>(
    pattern: string,
    callback: StepCallback<T>
  ): void
  export function Then<T extends unknown[]>(
    pattern: string,
    callback: StepCallback<T>
  ): void
  export function defineParameterType(options: {
    name: string
    regexp: RegExp
    transformer: (s: string) => unknown
  }): void
}

declare module '@badeball/cypress-cucumber-preprocessor/esbuild' {
  import type { PluginConfigOptions } from 'cypress'
  import type { Plugin } from 'esbuild'

  export function createEsbuildPlugin(config: PluginConfigOptions): Plugin
}

// Add any additional Cypress type declarations here
