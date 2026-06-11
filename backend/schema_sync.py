import json

from sqlalchemy import inspect, text
from sqlalchemy.exc import OperationalError


JSON_LIST_DEFAULTS = {
    "users": {"tags", "favorite_service_ids"},
    "services": {"image_urls", "participant_ids", "service_declarations_extra"},
}

DROP_COLUMNS = {
    "users": {
        "wechat_id",
        "ringtone_uri",
        "role",
        "status",
        "last_login_at",
        "is_deleted",
    },
}

DROP_TABLES = {
    "asset_items",
    "asset_records",
    "countdown_items",
    "database_schema_docs",
    "feedbacks",
    "friend_requests",
    "friendships",
    "focus_configs",
    "groups",
    "focus_records",
    "habit_records",
    "habits",
    "life_progress_configs",
    "memo_notes",
    "menstrual_configs",
    "menstrual_records",
    "quick_reminders",
    "recurring_reminders",
    "reminders",
    "salary_configs",
    "sports_records",
    "task_histories",
    "todo_items",
    "wish_tree_records",
}


def _quoted(name: str) -> str:
    return f'"{name}"'


def _default_sql_literal(table_name: str, column_name: str):
    if column_name in JSON_LIST_DEFAULTS.get(table_name, set()):
        return "'[]'"
    return None


def _rebuild_table_without_columns(connection, inspector, table_name: str, remove_columns: set[str]):
    columns = inspector.get_columns(table_name)
    keep_columns = [column for column in columns if column["name"] not in remove_columns]
    if len(keep_columns) == len(columns):
        return []

    column_defs = []
    insert_columns = []
    pk_columns = []
    for column in keep_columns:
        name = _quoted(column["name"])
        col_type = column["type"].compile(dialect=connection.dialect)
        parts = [name, col_type]
        if column.get("primary_key"):
            pk_columns.append(name)
        if not column.get("nullable", True):
            parts.append("NOT NULL")
        default = column.get("default")
        if default not in (None, ""):
            parts.append(f"DEFAULT {default}")
        column_defs.append(" ".join(parts))
        insert_columns.append(name)

    if pk_columns:
        column_defs.append(f"PRIMARY KEY ({', '.join(pk_columns)})")

    temp_table = f"{table_name}__schema_sync_new"
    statements = [
        f"CREATE TABLE {_quoted(temp_table)} ({', '.join(column_defs)});",
        (
            f"INSERT INTO {_quoted(temp_table)} ({', '.join(insert_columns)}) "
            f"SELECT {', '.join(insert_columns)} FROM {_quoted(table_name)};"
        ),
        f"DROP TABLE {_quoted(table_name)};",
        f"ALTER TABLE {_quoted(temp_table)} RENAME TO {_quoted(table_name)};",
    ]

    for statement in statements:
        connection.execute(text(statement))
    return statements


def sync_sqlite_schema(engine, metadata):
    if engine.url.get_backend_name() != "sqlite":
        return []

    inspector = inspect(engine)
    executed_statements = []
    existing_columns_by_table = {}

    with engine.begin() as connection:
        for table_name in sorted(DROP_TABLES):
            if not inspector.has_table(table_name):
                continue
            statement = f"DROP TABLE {_quoted(table_name)};"
            connection.execute(text(statement))
            executed_statements.append(statement)

        inspector = inspect(engine)
        for table_name, table in metadata.tables.items():
            if not inspector.has_table(table_name):
                continue

            db_columns = {column["name"] for column in inspector.get_columns(table_name)}
            existing_columns_by_table[table_name] = db_columns

            for column in table.columns:
                if column.name in db_columns:
                    continue

                sql_type = engine.dialect.type_compiler.process(column.type)
                statement = (
                    f"ALTER TABLE {_quoted(table_name)} "
                    f"ADD COLUMN {_quoted(column.name)} {sql_type}"
                )
                default_sql = _default_sql_literal(table_name, column.name)
                if default_sql is not None:
                    statement = f"{statement} DEFAULT {default_sql}"
                statement = f"{statement};"

                try:
                    connection.execute(text(statement))
                except OperationalError as exc:
                    if "duplicate column name" not in str(exc).lower():
                        raise
                    db_columns.add(column.name)
                else:
                    executed_statements.append(statement)
                    db_columns.add(column.name)

        inspector = inspect(engine)
        for table_name, removable_columns in DROP_COLUMNS.items():
            if not inspector.has_table(table_name):
                continue

            db_columns = {column["name"] for column in inspector.get_columns(table_name)}
            stale_columns = db_columns.intersection(removable_columns)
            if not stale_columns:
                continue

            executed_statements.extend(
                _rebuild_table_without_columns(connection, inspector, table_name, stale_columns)
            )
            inspector = inspect(engine)

        inspector = inspect(engine)
        for table_name in DROP_TABLES:
            if table_name in metadata.tables:
                continue
            if not inspector.has_table(table_name):
                continue
            statement = f"DROP TABLE {_quoted(table_name)};"
            connection.execute(text(statement))
            executed_statements.append(statement)
            inspector = inspect(engine)

        for table_name, column_names in JSON_LIST_DEFAULTS.items():
            if not inspector.has_table(table_name):
                continue

            for column_name in column_names:
                if column_name not in existing_columns_by_table.get(table_name, set()):
                    continue

                connection.execute(
                    text(
                        f'UPDATE {_quoted(table_name)} '
                        f'SET {_quoted(column_name)} = :value '
                        f'WHERE {_quoted(column_name)} IS NULL'
                    ),
                    {"value": json.dumps([])},
                )

    return executed_statements
