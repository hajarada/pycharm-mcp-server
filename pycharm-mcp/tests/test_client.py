"""Tests for the PyCharm Bridge client."""

import pytest
import respx
from httpx import Response

from pycharm_mcp.client import PyCharmBridgeError, PyCharmClient


@pytest.fixture
def client() -> PyCharmClient:
    """Create a test client."""
    return PyCharmClient(base_url="http://localhost:9876")


@respx.mock
@pytest.mark.asyncio
async def test_health_success(client: PyCharmClient) -> None:
    """Test successful health check."""
    respx.get("http://localhost:9876/health").mock(
        return_value=Response(
            200,
            json={
                "status": "ok",
                "version": "0.1.0",
                "projectsOpen": 2,
            },
        )
    )

    response = await client.health()
    assert response.status == "ok"
    assert response.version == "0.1.0"
    assert response.projects_open == 2

    await client.close()


@respx.mock
@pytest.mark.asyncio
async def test_list_projects_success(client: PyCharmClient) -> None:
    """Test successful project listing."""
    respx.get("http://localhost:9876/projects").mock(
        return_value=Response(
            200,
            json={
                "success": True,
                "projects": [
                    {
                        "name": "myproject",
                        "path": "/home/user/myproject",
                        "isOpen": True,
                        "isDefault": False,
                    }
                ],
            },
        )
    )

    response = await client.list_projects()
    assert response.success is True
    assert len(response.projects) == 1
    assert response.projects[0].name == "myproject"
    assert response.projects[0].path == "/home/user/myproject"

    await client.close()


@respx.mock
@pytest.mark.asyncio
async def test_rename_success(client: PyCharmClient) -> None:
    """Test successful rename operation."""
    respx.post("http://localhost:9876/refactor/rename").mock(
        return_value=Response(
            200,
            json={
                "success": True,
                "changes": [
                    {
                        "file": "/project/src/main.py",
                        "line": 10,
                        "oldText": "old_name",
                        "newText": "new_name",
                    }
                ],
                "filesModified": 1,
                "usagesUpdated": 1,
            },
        )
    )

    response = await client.rename(
        project="/project",
        file="src/main.py",
        line=10,
        column=5,
        new_name="new_name",
    )

    assert response.success is True
    assert response.files_modified == 1
    assert response.usages_updated == 1
    assert len(response.changes) == 1

    await client.close()


@respx.mock
@pytest.mark.asyncio
async def test_connection_error(client: PyCharmClient) -> None:
    """Test handling of connection errors."""
    respx.get("http://localhost:9876/health").mock(side_effect=Exception("Connection refused"))

    with pytest.raises(Exception):
        await client.health()

    await client.close()


@respx.mock
@pytest.mark.asyncio
async def test_error_response(client: PyCharmClient) -> None:
    """Test handling of error responses."""
    respx.post("http://localhost:9876/refactor/rename").mock(
        return_value=Response(
            200,
            json={
                "success": False,
                "error": "Element not found",
                "details": "No symbol at specified position",
            },
        )
    )

    with pytest.raises(PyCharmBridgeError) as exc_info:
        await client.rename(
            project="/project",
            file="src/main.py",
            line=10,
            column=5,
            new_name="new_name",
        )

    assert "Element not found" in str(exc_info.value)

    await client.close()
