"""Tool for renaming symbols in PyCharm."""

from pycharm_mcp.client import PyCharmBridgeError, PyCharmClient


async def rename_symbol(
    project_path: str,
    file_path: str,
    line: int,
    column: int,
    new_name: str,
    search_in_comments: bool = True,
    search_in_strings: bool = False,
    preview: bool = False,
) -> str:
    """
    Rename a symbol (variable, function, class, etc.) across the entire project.

    Uses PyCharm's semantic rename which understands scope and updates all references.
    Files are auto-saved before refactoring.

    Args:
        project_path: Absolute path to the project (use list_projects to find available projects)
        file_path: Path to the file containing the symbol (relative to project or absolute)
        line: Line number where the symbol is located (1-indexed)
        column: Column number where the symbol is located (1-indexed)
        new_name: The new name for the symbol
        search_in_comments: Also rename occurrences in comments (default: True)
        search_in_strings: Also rename occurrences in string literals (default: False)
        preview: If True, show what would change without applying (default: False)

    Returns:
        Summary of the rename operation including files modified and usages updated.
    """
    client = PyCharmClient()
    try:
        response = await client.rename(
            project=project_path,
            file=file_path,
            line=line,
            column=column,
            new_name=new_name,
            search_in_comments=search_in_comments,
            search_in_strings=search_in_strings,
            preview=preview,
        )

        if preview:
            lines = [f"Preview: Renaming to '{new_name}' would affect:", ""]
        else:
            lines = [f"Successfully renamed to '{new_name}':", ""]

        lines.append(f"  Files modified: {response.files_modified}")
        lines.append(f"  Usages updated: {response.usages_updated}")

        if response.changes:
            lines.append("")
            lines.append("Changes:")
            for change in response.changes[:10]:  # Limit to first 10
                lines.append(f"  • {change.file}:{change.line}")
                lines.append(f"    {change.old_text} → {change.new_text}")

            if len(response.changes) > 10:
                lines.append(f"  ... and {len(response.changes) - 10} more")

        return "\n".join(lines)
    except PyCharmBridgeError as e:
        return f"Error: {e.message}\n{e.details or ''}"
    finally:
        await client.close()
