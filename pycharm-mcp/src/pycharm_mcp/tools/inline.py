"""Tool for inlining elements in PyCharm."""

from pycharm_mcp.client import PyCharmBridgeError, PyCharmClient


async def inline_element(
    project_path: str,
    file_path: str,
    line: int,
    column: int,
    preview: bool = False,
) -> str:
    """
    Inline a variable or method (replace usages with the definition).

    For variables, each usage is replaced with the assigned value.
    For methods, each call is replaced with the method body.

    Args:
        project_path: Absolute path to the project
        file_path: Path to the file containing the element
        line: Line number where the element is defined (1-indexed)
        column: Column number (1-indexed)
        preview: If True, show what would change without applying (default: False)

    Returns:
        Summary of the inline operation including usages replaced.
    """
    client = PyCharmClient()
    try:
        response = await client.inline(
            project=project_path,
            file=file_path,
            line=line,
            column=column,
            preview=preview,
        )

        if preview:
            lines = ["Preview: Inlining would affect:", ""]
        else:
            lines = ["Successfully inlined:", ""]

        lines.append(f"  Usages inlined: {response.usages_inlined}")

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
