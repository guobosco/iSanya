import asyncio
import json
import os
import uuid
from collections import defaultdict
from typing import Dict, List, Optional, Set

from fastapi import WebSocket

try:
    import redis.asyncio as redis
except ImportError:  # pragma: no cover - optional dependency during local dev
    redis = None


class ChatRealtimeHub:
    def __init__(self, redis_url: Optional[str] = None):
        self.redis_url = redis_url or os.getenv("LULU_REDIS_URL", "").strip()
        self.instance_id = str(uuid.uuid4())
        self._connections: Dict[str, Set[WebSocket]] = defaultdict(set)
        self._redis = None
        self._pubsub = None
        self._listener_task: Optional[asyncio.Task] = None

    async def startup(self):
        if not self.redis_url or redis is None:
            return
        try:
            self._redis = redis.from_url(self.redis_url, decode_responses=True)
            await self._redis.ping()
            self._pubsub = self._redis.pubsub()
            await self._pubsub.subscribe("chat_events")
            self._listener_task = asyncio.create_task(self._listen())
        except Exception:
            self._redis = None
            self._pubsub = None
            self._listener_task = None

    async def shutdown(self):
        if self._listener_task:
            self._listener_task.cancel()
            try:
                await self._listener_task
            except asyncio.CancelledError:
                pass
            self._listener_task = None
        if self._pubsub is not None:
            try:
                await self._pubsub.close()
            except Exception:
                pass
            self._pubsub = None
        if self._redis is not None:
            try:
                await self._redis.close()
            except Exception:
                pass
            self._redis = None

    async def register(self, user_id: str, websocket: WebSocket):
        self._connections[user_id].add(websocket)

    async def unregister(self, user_id: str, websocket: WebSocket):
        sockets = self._connections.get(user_id)
        if not sockets:
            return
        sockets.discard(websocket)
        if not sockets:
            self._connections.pop(user_id, None)

    async def publish_to_users(self, user_ids: List[str], event: dict):
        await self._dispatch_local(user_ids, event)
        if self._redis is None:
            return
        payload = json.dumps(
            {
                "origin": self.instance_id,
                "user_ids": user_ids,
                "event": event,
            },
            ensure_ascii=False,
        )
        try:
            await self._redis.publish("chat_events", payload)
        except Exception:
            pass

    async def _listen(self):
        if self._pubsub is None:
            return
        async for raw_message in self._pubsub.listen():
            if not raw_message or raw_message.get("type") != "message":
                continue
            try:
                payload = json.loads(raw_message.get("data") or "{}")
                if payload.get("origin") == self.instance_id:
                    continue
                user_ids = payload.get("user_ids") or []
                event = payload.get("event") or {}
                await self._dispatch_local(user_ids, event)
            except Exception:
                continue

    async def _dispatch_local(self, user_ids: List[str], event: dict):
        for user_id in set(user_ids):
            sockets = list(self._connections.get(user_id, set()))
            stale: List[WebSocket] = []
            for websocket in sockets:
                try:
                    await websocket.send_json(event)
                except Exception:
                    stale.append(websocket)
            for websocket in stale:
                await self.unregister(user_id, websocket)
