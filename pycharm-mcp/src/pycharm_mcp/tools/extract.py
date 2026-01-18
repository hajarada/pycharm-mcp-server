"""Tools for extracting methods and variables in PyCharm."""

from pycharm_mcp.client import PyCharmBridgeError, PyCharmClient


async def extract_method(
    project_path: str,
    file_path: str,
    start_line: int,
    start_column: int,
    end_line: int,
    end_column: int,
    method_name: str,
    preview: bool = False,
) -> str:
    """
    Extract selected code into a new method.

    PyCharm automatically determines parameters and return values based on the
    selected code's dependencies.

    Args:
        project_path: Absolute path to the project
        file_path: Path to the file containing the code to extract
        start_line: Starting line of the selection (1-indexed)
        start_column: Starting column of the selection (1-indexed)
        end_line: Ending line of the selection (1-indexed)
        end_column: Ending column of the selection (1-indexed)
        method_name: Name for the new method
        preview: If True, show what would change without applying (default: False)

    Returns:
        Summary of the extraction including the new method's signature.
    """
    client = PyCharmClient()
    try:
        response = await client.extract_method(
            project=project_path,
            file=file_path,
            start_line=start_line,
            start_column=start_column,
            end_line=end_line,
            end_column=end_column,
            method_name=method_name,
            preview=preview,
        )

        if preview:
            lines = [f"Preview: Would extract method '{method_name}':", ""]
        else:
            lines = [f"Successfully extracted method '{method_name}':", ""]

        lines.append(f"  File: {response.file}")
        lines.append(f"  Method line: {response.method_line}")

        if response.parameters:
            lines.append(f"  Parameters: {', '.join(response.parameters)}")
        else:
            lines.append("  Parameters: none")

        if response.return_type:
            lines.append(f"  Return type: {response.return_type}")

        return "\n".join(lines)
    except PyCharmBridgeError as e:
        return f"Error: {e.message}\n{e.details or ''}"
    finally:
        await client.close()


async def extract_variable(
    project_path: str,
    file_path: str,
    start_line: int,
    start_column: int,
    end_line: int,
    end_column: int,
    variable_name: str,
    replace_all: bool = True,
    preview: bool = False,
) -> str:
    """
    Extract an expression into a variable.

    The selected expression will be assigned to a new variable, and optionally
    all identical occurrences will be replaced.

    Args:
        project_path: Absolute path to the project
        file_path: Path to the file containing the expression
        start_line: Starting line of the expression (1-indexed)
        start_column: Starting column of the expression (1-indexed)
        end_line: Ending line of the expression (1-indexed)
        end_column: Ending column of the expression (1-indexed)
        variable_name: Name for the new variable
        replace_all: Replace all identical occurrences (default: True)
        preview: If True, show what would change without applying (default: False)

    Returns:
        Summary of the extraction including occurrences replaced.
    """
    client = PyCharmClient()
    try:
        response = await client.extract_variable(
            project=project_path,
            file=file_path,
            start_line=start_line,
            start_column=start_column,
            end_line=end_line,
            end_column=end_column,
            variable_name=variable_name,
            replace_all=replace_all,
            preview=preview,
        )

        if preview:
            lines = [f"Preview: Would extract variable '{variable_name}':", ""]
        else:
            lines = [f"Successfully extracted variable '{variable_name}':", ""]

        lines.append(f"  File: {response.file}")
        lines.append(f"  Variable line: {response.variable_line}")
        lines.append(f"  Occurrences replaced: {response.occurrences_replaced}")

        return "\n".join(lines)
    except PyCharmBridgeError as e:
        return f"Error: {e.message}\n{e.details or ''}"
    finally:
        await client.close()
