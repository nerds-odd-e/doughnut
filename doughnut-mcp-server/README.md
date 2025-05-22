# doughnut-mcp-server MCP Server

A Model Context Protocol server for Doughnut.

This is a TypeScript-based MCP server that demonstrates core MCP concepts by providing:

- Tools for getting instructions and updating note text content via the Doughnut backend API.

## How to use this MCP Server

Run this command to build the mcp server

```sh
CURSOR_DEV=true nix develop -c pnpm mcp-server:bundle
```
Add the below command to your AI MCP Server configuration

```json
{
  "mcpServers": {
    "doughnut": {
      "disabled": false,
      "timeout": 60,
      "transportType": "stdio",
      "command": "node",
      "args": [
        "/home/csd/doughnut/frontend/public/mcp-server.bundle.mjs"
      ],
      "env": {
        "DOUGHNUT_API_BASE_URL": "http://localhost:9081",
        "DOUGHNUT_API_AUTH_TOKEN": "your-token-here"
      }
    }
  }
}
```

## Features

### Tools

- `get_instruction` - Get instruction of Doughnut
  - Takes an empty object as parameter
  - Returns instruction for Doughnut

- `get_sampleapi` - Get sample API
  - Takes an empty object as parameter
  - Returns a sample API response

- `update_note_text_content` - Update the title and/or details of a note by note ID
  - Parameters:
    - `noteId` (integer, required): The ID of the note to update
    - `newTitle` (string, optional): The new title for the note
    - `newDetails` (string, optional): The new details for the note
  - At least one of `newTitle` or `newDetails` must be provided
  - **Authentication token is always taken from the `DOUGHNUT_API_AUTH_TOKEN` environment variable**

## Environment Variable Configuration

The MCP server supports configuration via environment variables:

- `DOUGHNUT_API_BASE_URL`: The base URL of the Doughnut backend API (default: `http://localhost:9081`)
- `DOUGHNUT_API_AUTH_TOKEN`: The authentication token to use for API requests

You can set these in your MCP server configuration using the `env` property, or when running the bundle.

## How to Add a New API Tool

To add a new tool to the MCP server, edit `src/index.ts`:

1. **Define your tool in the ListTools handler**  
   Add your tool to the `tools` array in the `ListToolsRequestSchema` handler:
   ```ts
   server.setRequestHandler(ListToolsRequestSchema, async () => {
     return {
       tools: [
         {
           name: 'get_instruction',
           description: 'Get instruction',
           inputSchema: { type: 'object' },
         },
         {
           name: 'your_tool_name',
           description: 'Describe your tool',
           inputSchema: { type: 'object' }, // or your schema
         },
       ],
     }
   })
   ```

2. **Handle your tool in the CallTool handler**  
   Add a case for your tool in the `CallToolRequestSchema` handler:
   ```ts
   server.setRequestHandler(CallToolRequestSchema, async (request) => {
     switch (request.params.name) {
       case 'get_instruction':
         // existing code...
         break;
       case 'your_tool_name':
         // Implement your tool logic here
         return {
           content: [
             {
               type: 'text',
               text: 'Your tool response here',
             },
           ],
         }
       default:
         throw new Error('Unknown tool')
     }
   })
   ```

3. **Rebuild the bundle**  
   After making changes, rebuild the bundle:
   ```
   pnpm mcp-server:bundle
   ```
   This will output `mcp-server.bundle.mjs` into the frontend/public folder

## Development

- To run locally (TypeScript, not bundled):
  ```
  CURSOR_DEV=true nix develop -c node src/index.ts
  ```
- To build the bundle for use in MCP configuration:
  ```
  pnpm bundle
  ```
  This will output `build/mcp-server.bundle.mjs`.

## Running the MCP Server Bundle with Custom Environment Variables

To run the ESM bundle with a custom backend URL and authentication token, use:

```sh
DOUGHNUT_API_BASE_URL=http://your-server:port DOUGHNUT_API_AUTH_TOKEN=your-token node build/mcp-server.bundle.mjs
```

Replace `http://your-server:port` and `your-token` with your actual backend address and token.

## Installation

- Install dependencies:
  ```
  CURSOR_DEV=true nix develop -c pnpm install
