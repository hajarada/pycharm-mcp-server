# PyCharm MCP Server

MCP server that exposes PyCharm's refactoring capabilities to Claude Code.

## Installation

```bash
# Using pip
pip install pycharm-mcp

# Using uvx (recommended for Claude Code)
uvx pycharm-mcp
```

## Prerequisites

1. PyCharm with the **Refactoring Bridge** plugin installed
2. PyCharm running with a project open

## Configuration

### Claude Code MCP Config

Add to `~/.claude/mcp.json`:

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

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PYCHARM_BRIDGE_URL` | URL to PyCharm plugin | `http://localhost:9876` |
| `PYCHARM_BRIDGE_TOKEN` | Optional auth token | (none) |

## Available Tools

### `pycharm_list_projects`

List all projects currently open in PyCharm.

```
Returns project names, paths, and which one is currently active.
```

### `pycharm_rename_symbol`

Rename a symbol (variable, function, class, etc.) across the entire project.

**Parameters:**
- `project_path`: Absolute path to the project
- `file_path`: Path to the file containing the symbol
- `line`: Line number (1-indexed)
- `column`: Column number (1-indexed)
- `new_name`: The new name for the symbol
- `search_in_comments`: Also rename in comments (default: True)
- `search_in_strings`: Also rename in strings (default: False)
- `preview`: Show changes without applying (default: False)

### `pycharm_move_element`

Move a class, function, or variable to a different module.

**Parameters:**
- `project_path`: Absolute path to the project
- `file_path`: Path to the source file
- `line`: Line number (1-indexed)
- `column`: Column number (1-indexed)
- `target_file`: Path to the destination file
- `preview`: Show changes without applying (default: False)

### `pycharm_extract_method`

Extract selected code into a new method.

**Parameters:**
- `project_path`: Absolute path to the project
- `file_path`: Path to the file
- `start_line`, `start_column`: Selection start (1-indexed)
- `end_line`, `end_column`: Selection end (1-indexed)
- `method_name`: Name for the new method
- `preview`: Show changes without applying (default: False)

### `pycharm_extract_variable`

Extract an expression into a variable.

**Parameters:**
- `project_path`: Absolute path to the project
- `file_path`: Path to the file
- `start_line`, `start_column`: Expression start (1-indexed)
- `end_line`, `end_column`: Expression end (1-indexed)
- `variable_name`: Name for the new variable
- `replace_all`: Replace all occurrences (default: True)
- `preview`: Show changes without applying (default: False)

### `pycharm_inline_element`

Inline a variable or method (replace usages with definition).

**Parameters:**
- `project_path`: Absolute path to the project
- `file_path`: Path to the file
- `line`: Line number (1-indexed)
- `column`: Column number (1-indexed)
- `preview`: Show changes without applying (default: False)

### `pycharm_change_signature`

Change a function's signature (name, parameters, return type).

**Parameters:**
- `project_path`: Absolute path to the project
- `file_path`: Path to the file
- `line`: Line number (1-indexed)
- `column`: Column number (1-indexed)
- `new_name`: New function name (optional)
- `parameters`: New parameter list (optional)
- `return_type`: New return type (optional)
- `preview`: Show changes without applying (default: False)

### `pycharm_safe_delete`

Delete an element only if it has no usages.

**Parameters:**
- `project_path`: Absolute path to the project
- `file_path`: Path to the file
- `line`: Line number (1-indexed)
- `column`: Column number (1-indexed)
- `search_for_usages`: Check usages first (default: True)

### `pycharm_find_usages`

Find all usages of a symbol across the project.

**Parameters:**
- `project_path`: Absolute path to the project
- `file_path`: Path to the file
- `line`: Line number (1-indexed)
- `column`: Column number (1-indexed)

## Usage Examples

In Claude Code:

```
> List the projects open in PyCharm
> Rename the `process_data` function on line 42 of src/handler.py to `transform_data`
> Move the DataProcessor class from src/core.py to src/utils/processor.py
> Extract lines 50-65 in main.py into a method called validate_input
> Find all usages of the CONFIG variable in src/settings.py
```

## Development

```bash
# Install in development mode
uv venv
source .venv/bin/activate
uv pip install -e ".[dev]"

# Run tests
pytest

# Type checking
mypy src/

# Linting
ruff check src/
```

## License

MIT License
