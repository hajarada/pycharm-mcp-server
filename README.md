# PyCharm MCP Server

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Expose PyCharm's powerful refactoring capabilities to Claude Code via the Model Context Protocol (MCP).

## Overview

This project provides a two-part system that bridges Claude Code to PyCharm's refactoring engine:

1. **PyCharm Plugin** (`pycharm-refactoring-bridge/`) - A Kotlin plugin that exposes refactoring operations via HTTP
2. **MCP Server** (`pycharm-mcp/`) - A Python MCP server that Claude Code connects to

```
┌─────────────┐      ┌──────────────────┐      ┌─────────────────────┐
│ Claude Code │ ──── │ MCP Server       │ ──── │ PyCharm Plugin      │
│             │ MCP  │ (Python/FastMCP) │ HTTP │ (Kotlin)            │
│             │stdio │                  │:9876 │ Refactoring Bridge  │
└─────────────┘      └──────────────────┘      └─────────────────────┘
```

## Features

- **Rename Symbol** - Rename variables, functions, classes across the entire project
- **Move Element** - Move classes/functions between modules with automatic import updates
- **Extract Method** - Extract code blocks into new methods with automatic parameter detection
- **Extract Variable** - Extract expressions into variables
- **Inline** - Inline variables or methods (replace usages with definitions)
- **Change Signature** - Modify function parameters and return types
- **Safe Delete** - Delete elements only if they have no usages
- **Find Usages** - Preview what a refactoring would affect

## Installation

### 1. Install the PyCharm Plugin

**Option A: Build from source**
```bash
cd pycharm-refactoring-bridge
./gradlew buildPlugin
# Install from build/distributions/pycharm-refactoring-bridge-*.zip
```

**Option B: Install from JetBrains Marketplace** (coming soon)

### 2. Install the MCP Server

```bash
# Using pip
pip install pycharm-mcp

# Using uvx (recommended)
uvx pycharm-mcp
```

### 3. Configure Claude Code

Add to your Claude Code MCP configuration (`~/.claude/mcp.json`):

```json
{
  "mcpServers": {
    "pycharm": {
      "command": "uvx",
      "args": ["pycharm-mcp"],
      "env": {
        "PYCHARM_BRIDGE_URL": "http://localhost:9876"
      }
    }
  }
}
```

## Usage

### In Claude Code

Once configured, you can ask Claude to perform refactorings:

```
> Rename the function `process_data` to `transform_data` in src/processor.py
> Move the `DataProcessor` class to utils/processor.py
> Extract lines 45-60 in handler.py into a method called `validate_input`
> Find all usages of the `CONFIG` constant
```

### Available Tools

| Tool | Description |
|------|-------------|
| `pycharm_list_projects` | List open projects in PyCharm |
| `pycharm_rename_symbol` | Rename a symbol across the project |
| `pycharm_move_element` | Move class/function to another module |
| `pycharm_extract_method` | Extract code into a new method |
| `pycharm_extract_variable` | Extract expression into a variable |
| `pycharm_inline_element` | Inline a variable or method |
| `pycharm_change_signature` | Modify function signature |
| `pycharm_safe_delete` | Delete if no usages exist |
| `pycharm_find_usages` | Find all usages of a symbol |

## Configuration

### Plugin Settings

In PyCharm: **Settings → Tools → Refactoring Bridge**

- **Port**: HTTP server port (default: 9876)
- **Enabled**: Enable/disable the HTTP server
- **Auth Token**: Optional bearer token for authentication

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PYCHARM_BRIDGE_URL` | URL to the PyCharm plugin | `http://localhost:9876` |
| `PYCHARM_BRIDGE_TOKEN` | Optional auth token | (none) |

## Security

- The plugin binds to `127.0.0.1` only (localhost)
- Optional authentication via bearer token
- Project allowlist to restrict which projects can be refactored

## Development

### Building the Plugin

```bash
cd pycharm-refactoring-bridge
./gradlew build
./gradlew runIde  # Test in sandbox IDE
```

### Running the MCP Server Locally

```bash
cd pycharm-mcp
uv venv
source .venv/bin/activate
uv pip install -e ".[dev]"
python -m pycharm_mcp.server
```

### Running Tests

```bash
# Plugin tests
cd pycharm-refactoring-bridge
./gradlew test

# MCP server tests
cd pycharm-mcp
pytest
```

## Requirements

- PyCharm 2024.1+ (Professional or Community)
- Python 3.10+
- Claude Code CLI

## License

MIT License - see [LICENSE](LICENSE) for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
