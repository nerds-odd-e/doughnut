declare module '@badeball/cypress-cucumber-preprocessor/esbuild' {
  import { PluginConfigOptions } from 'cypress'
  import { Plugin } from 'esbuild'

  export function createEsbuildPlugin(config: PluginConfigOptions): Plugin
}
