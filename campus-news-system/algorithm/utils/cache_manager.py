"""缓存管理器 - 提供多级缓存支持

核心功能：
1. 内存缓存 - 基于字典的快速缓存
2. TTL管理 - 支持过期时间设置
3. 缓存统计 - 命中率、大小等统计
4. LRU淘汰 - 基于访问时间的缓存淘汰
5. 装饰器支持 - 便捷的函数缓存

答辩要点：
Q: 为什么需要缓存？
A: 减少重复计算，提高响应速度，降低数据库压力

Q: TTL是什么？
A: Time To Live，缓存的生存时间，过期后自动失效

Q: 什么是LRU？
A: Least Recently Used，最近最少使用，淘汰最久未访问的缓存
"""
import json
import time
import hashlib
from typing import Any, Optional, Dict, Callable
from functools import wraps
import logging
import threading

logger = logging.getLogger(__name__)

class CacheManager:
    """内存缓存管理器
    
    提供线程安全的内存缓存功能，支持：
    - TTL过期管理
    - 缓存命中率统计
    - 自动清理过期缓存
    - LRU淘汰策略
    """
    
    def __init__(self, max_size: int = 1000):
        """初始化缓存管理器
        
        Args:
            max_size: 最大缓存条目数
        """
        self._cache: Dict[str, Dict] = {}
        self._access_times: Dict[str, float] = {}
        self._hit_count = 0
        self._miss_count = 0
        self._max_size = max_size
        self._lock = threading.RLock()
        self._creation_time = time.time()
        logger.info(f"缓存管理器初始化完成，最大容量: {max_size}")
    
    def _generate_key(self, prefix: str, *args, **kwargs) -> str:
        """生成缓存键
        
        Args:
            prefix: 键前缀
            *args: 位置参数
            **kwargs: 关键字参数
            
        Returns:
            MD5哈希后的缓存键
        """
        key_data = f"{prefix}:{args}:{sorted(kwargs.items())}"
        return hashlib.md5(key_data.encode()).hexdigest()
    
    def get(self, key: str) -> Optional[Any]:
        """获取缓存值
        
        Args:
            key: 缓存键
            
        Returns:
            缓存值，不存在或过期返回None
        """
        with self._lock:
            if key in self._cache:
                cache_item = self._cache[key]
                if time.time() - cache_item['timestamp'] < cache_item['ttl']:
                    self._access_times[key] = time.time()
                    self._hit_count += 1
                    logger.debug(f"缓存命中: {key[:16]}...")
                    return cache_item['data']
                else:
                    # 缓存过期，删除
                    del self._cache[key]
                    if key in self._access_times:
                        del self._access_times[key]
                    logger.debug(f"缓存过期: {key[:16]}...")
            
            self._miss_count += 1
            logger.debug(f"缓存未命中: {key[:16]}...")
            return None
    
    def set(self, key: str, value: Any, ttl: int = 3600):
        """设置缓存值
        
        Args:
            key: 缓存键
            value: 缓存值
            ttl: 生存时间（秒）
        """
        with self._lock:
            # 检查是否需要淘汰
            if len(self._cache) >= self._max_size:
                self._evict_lru()
            
            self._cache[key] = {
                'data': value,
                'timestamp': time.time(),
                'ttl': ttl
            }
            self._access_times[key] = time.time()
            logger.debug(f"缓存设置: {key[:16]}..., TTL: {ttl}s")
    
    def _evict_lru(self):
        """淘汰最近最少使用的缓存"""
        if not self._access_times:
            return
        
        # 找到最久未访问的键
        oldest_key = min(self._access_times.items(), key=lambda x: x[1])[0]
        self.delete(oldest_key)
        logger.debug(f"LRU淘汰: {oldest_key[:16]}...")
    
    def delete(self, key: str):
        """删除缓存
        
        Args:
            key: 缓存键
        """
        with self._lock:
            if key in self._cache:
                del self._cache[key]
            if key in self._access_times:
                del self._access_times[key]
            logger.debug(f"缓存删除: {key[:16]}...")
    
    def clear(self):
        """清空所有缓存"""
        with self._lock:
            self._cache.clear()
            self._access_times.clear()
            self._hit_count = 0
            self._miss_count = 0
            logger.info("缓存已清空")
    
    def get_stats(self) -> Dict:
        """获取缓存统计信息
        
        Returns:
            包含缓存统计的字典
        """
        with self._lock:
            total_requests = self._hit_count + self._miss_count
            hit_rate = (self._hit_count / total_requests * 100) if total_requests > 0 else 0
            
            return {
                'total_keys': len(self._cache),
                'max_size': self._max_size,
                'hit_count': self._hit_count,
                'miss_count': self._miss_count,
                'hit_rate': round(hit_rate, 2),
                'cache_size_mb': self._estimate_size(),
                'uptime_seconds': round(time.time() - self._creation_time, 2)
            }
    
    def _estimate_size(self) -> float:
        """估算缓存大小（MB）
        
        Returns:
            缓存大小（MB）
        """
        try:
            size_bytes = len(json.dumps(self._cache, default=str).encode('utf-8'))
            return round(size_bytes / 1024 / 1024, 2)
        except:
            return 0.0
    
    def cleanup_expired(self) -> int:
        """清理过期缓存
        
        Returns:
            清理的缓存数量
        """
        with self._lock:
            current_time = time.time()
            expired_keys = []
            
            for key, cache_item in self._cache.items():
                if current_time - cache_item['timestamp'] >= cache_item['ttl']:
                    expired_keys.append(key)
            
            for key in expired_keys:
                self.delete(key)
            
            if expired_keys:
                logger.info(f"清理过期缓存: {len(expired_keys)} 个")
            
            return len(expired_keys)
    
    def get_keys_by_prefix(self, prefix: str) -> list:
        """获取指定前缀的所有键
        
        Args:
            prefix: 键前缀
            
        Returns:
            匹配的键列表
        """
        with self._lock:
            return [k for k in self._cache.keys() if k.startswith(prefix)]
    
    def delete_by_prefix(self, prefix: str) -> int:
        """删除指定前缀的所有缓存
        
        Args:
            prefix: 键前缀
            
        Returns:
            删除的缓存数量
        """
        keys = self.get_keys_by_prefix(prefix)
        for key in keys:
            self.delete(key)
        logger.info(f"按前缀删除缓存: {prefix}, 数量: {len(keys)}")
        return len(keys)

# 全局缓存实例
cache_manager = CacheManager()

def cached(ttl: int = 3600, prefix: str = "default"):
    """缓存装饰器"""
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            # 生成缓存键
            cache_key = cache_manager._generate_key(f"{prefix}:{func.__name__}", *args, **kwargs)
            
            # 尝试从缓存获取
            cached_result = cache_manager.get(cache_key)
            if cached_result is not None:
                return cached_result
            
            # 执行函数并缓存结果
            result = func(*args, **kwargs)
            cache_manager.set(cache_key, result, ttl)
            return result
        
        return wrapper
    return decorator