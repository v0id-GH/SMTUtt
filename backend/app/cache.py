import time
from typing import Any, Optional, Dict

class SimpleMemoryCache:
    def __init__(self, default_ttl: int = 1800):
        self._cache: Dict[str, Dict[str, Any]] = {}
        self.default_ttl = default_ttl

    def get(self, key: str) -> Optional[Any]:
        entry = self._cache.get(key)
        if not entry:
            return None
        if time.time() > entry["expires_at"]:
            del self._cache[key]
            return None
        return entry["data"]

    def set(self, key: str, value: Any, ttl: Optional[int] = None) -> None:
        duration = ttl if ttl is not None else self.default_ttl
        self._cache[key] = {
            "data": value,
            "expires_at": time.time() + duration,
            "cached_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
        }

    def clear(self) -> None:
        self._cache.clear()

cache = SimpleMemoryCache()
