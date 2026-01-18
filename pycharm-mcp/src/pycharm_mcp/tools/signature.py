"""Tool for changing function signatures in PyCharm."""

from pycharm_mcp.client import PyCharmBridgeError, PyCharmClient
from pycharm_mcp.models import ParameterInfo


async def change_signature(
    project_path: str,
    file_path: str,
    line: int,
    column: int,
    new_name: str | None = None,
    parameters: list[dict[str, str | None]] | None = None,
    return_type: str | None = None,
    preview: bool = False,
) -> str:
    """
    Change a function's signature (name, parameters, return type).

    Updates all call sites automatically. You can change the function name,
    add/remove/reorder parameters, and change the return type annotation.

    Args:
        project_path: Absolute path to the project
        file_path: Path to the file containing the function
        line: Line number where the function is defined (1-indexed)
        column: Column number (1-indexed)
        new_name: New name for the function (optional)
        parameters: New parameter list, each with 'name', optional 'type', and optional 'defaultValue'
        return_type: New return type annotation (optional)
        preview: If True, show what would change without applying (default: False)

    Returns:
        Summary of the signature change including call sites updated.
    """
    client = PyCharmClient()
    try:
        # Convert dict parameters to ParameterInfo
        param_infos: list[ParameterInfo] | None = None
        if parameters is not None:
            param_infos = [
                ParameterInfo(
                    name=p["name"],  # type: ignore[arg-type]
                    type=p.get("type"),
                    default_value=p.get("defaultValue"),
                )
                for p in parameters
            ]

        response = await client.change_signature(
            project=project_path,
            file=file_path,
            line=line,
            column=column,
            new_name=new_name,
            parameters=param_infos,
            return_type=return_type,
            preview=preview,
        )

        if preview:
            lines = ["Preview: Signature change would affect:", ""]
        else:
            lines = ["Successfully changed signature:", ""]

        lines.append(f"  Call sites updated: {response.call_sites_updated}")

        if response.changes:
            lines.append("")
            lines.append("Changes:")
            for change in response.changes[:10]:
                lines.append(f"  • {change.file}:{change.line}")
                lines.append(f"    {change.old_text}")
                lines.append(f"    → {change.new_text}")

            if len(response.changes) > 10:
                lines.append(f"  ... and {len(response.changes) - 10} more")

        return "\n".join(lines)
    except PyCharmBridgeError as e:
        return f"Error: {e.message}\n{e.details or ''}"
    finally:
        await client.close()
