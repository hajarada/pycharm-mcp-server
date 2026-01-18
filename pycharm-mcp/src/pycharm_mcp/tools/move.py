"""Tool for moving elements between modules in PyCharm."""

from pycharm_mcp.client import PyCharmBridgeError, PyCharmClient


async def move_element(
    project_path: str,
    file_path: str,
    line: int,
    column: int,
    target_file: str,
    preview: bool = False,
) -> str:
    """
    Move a class, function, or variable to a different module.

    Automatically updates all imports across the project. The element at the
    specified position will be moved to the target file.

    Args:
        project_path: Absolute path to the project
        file_path: Path to the source file containing the element
        line: Line number where the element is defined (1-indexed)
        column: Column number (1-indexed)
        target_file: Path to the destination file
        preview: If True, show what would change without applying (default: False)

    Returns:
        Summary of the move operation including import updates.
    """
    client = PyCharmClient()
    try:
        response = await client.move(
            project=project_path,
            file=file_path,
            line=line,
            column=column,
            target_file=target_file,
            preview=preview,
        )

        if preview:
            lines = [f"Preview: Moving to '{target_file}' would affect:", ""]
        else:
            lines = [f"Successfully moved to '{target_file}':", ""]

        lines.append(f"  Files modified: {response.files_modified}")
        lines.append(f"  Imports updated: {response.imports_updated}")

        if response.changes:
            lines.append("")
            lines.append("Changes:")
            for change in response.changes[:10]:
                lines.append(f"  • {change.file}:{change.line}")
                lines.append(f"    {change.old_text} → {change.new_text}")

            if len(response.changes) > 10:
                lines.append(f"  ... and {len(response.changes) - 10} more")

        return "\n".join(lines)
    except PyCharmBridgeError as e:
        return f"Error: {e.message}\n{e.details or ''}"
    finally:
        await client.close()
