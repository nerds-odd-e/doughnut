# Style Guide

## For biome to work correctly for _frontend_ Vue3 source files, you need IDE to open code from root of `doughnut/frontend` if you require linting/formatting rules for Vue3 work

## For biome to work correctly for _cypress e2e_ source files, you need IDE to open code from root of `doughnut` if you require linting/formatting rules for cypress e2e work

- Cursor IDE:
  - Install these following Cursor/VSCode extensions:
    1. [Biome LSP Cursor/VSCode Extension](https://marketplace.visualstudio.com/items?itemName=biomejs.biome)
    2. [Vue - Official Cursor/VSCode Typescript extension for Vue3](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (more info [here](https://blog.vuejs.org/posts/volar-a-new-beginning))

- IntelliJ Ultimate IDE:
  - Install the following IntelliJ Ultimate plugin:
    1. [Biome plugin for Jetbrains IDEs](https://plugins.jetbrains.com/plugin/22761-biome)

- Zed IDE:
  - Install the following Zed extension:
    1. Open the Command Palette (View or Ctrl/⌘+⇧+P)
    2. Select zed: extensions
    3. Search Biome
    4. Select Install

```json
"editor.codeActionsOnSave": {
  "source.organizeImports": "explicit",
  "source.fixAll": "explicit"
}
```

- Use [EditorConfig](https://editorconfig.org/) for general coding styles.
  - Most Editors / IDEs support EditorConfig already.  
    [Download a plugin](https://editorconfig.org/#download) if your tool doesn't support it
    natively.
- Use [Spotless](https://github.com/diffplug/spotless)
  with [google-java-format](https://github.com/google/google-java-format) for Java source code.
  - Follow [Using the formatter](https://github.com/google/google-java-format#using-the-formatter)
    to set up formatter or plugin for your IDE.
    - :warning: For IntelliJ, download
      the [IntelliJ Java Google Style file](https://raw.githubusercontent.com/google/styleguide/gh-pages/intellij-java-google-style.xml)
      and import it into File→Settings→Editor→Code Style.  
      ![Google Java Style](images/import_google_java_style.png)
