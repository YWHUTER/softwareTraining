"""性能监控器 - 监控算法执行性能

核心功能：
1. 执行时间记录 - 记录函数执行耗时
2. 成功率统计 - 统计函数调用成功率
3. 错误追踪 - 记录最近的错误信息
4. 性能报告 - 生成性能分析报告
5. 装饰器支持 - 便捷的性能监控

答辩要点：
Q: 为什么需要性能监控？
A: 及时发现性能瓶颈，优化系统响应速度

Q: 监控哪些指标？
A: 执行时间、成功率、错误率、调用次数等

Q: 如何处理慢查询？
A: 记录超过阈值的慢查询，便于后续优化
"""
import time
import logging
from typing import Dict, List, Optional, Callable
from functools import wraps
from collections import defaultdict, deque
import threading
from datetime import datetime

logger = logging.getLogger(__name__)

class PerformanceMonitor:
    """性能监控器
    
    提供线程安全的性能监控功能，支持：
    - 函数执行时间记录
    - 成功率和错误率统计
    - 慢查询检测和告警
    - 性能报告生成
    """
    
    def __init__(self, max_records: int = 1000, slow_threshold: float = 1.0):
        """初始化性能监控器
        
        Args:
            max_records: 每个函数最大记录数
            slow_threshold: 慢查询阈值（秒）
        """
        self.max_records = max_records
        self.slow_threshold = slow_threshold
        self._metrics: Dict[str, deque] = defaultdict(lambda: deque(maxlen=max_records))
        self._slow_queries: deque = deque(maxlen=100)
        self._lock = threading.Lock()
        self._start_time = time.time()
        logger.info(f"性能监控器初始化完成，慢查询阈值: {slow_threshold}s")
    
    def record_execution(self, function_name: str, execution_time: float, 
                        success: bool = True, error: Optional[str] = None):
        """记录函数执行性能
        
        Args:
            function_name: 函数名称
            execution_time: 执行时间（秒）
            success: 是否成功
            error: 错误信息
        """
        with self._lock:
            record = {
                'timestamp': time.time(),
                'datetime': datetime.now().isoformat(),
                'execution_time': execution_time,
                'success': success,
                'error': error
            }
            self._metrics[function_name].append(record)
            
            # 记录慢查询
            if execution_time > self.slow_threshold:
                self._slow_queries.append({
                    'function_name': function_name,
                    'execution_time': execution_time,
                    'timestamp': time.time(),
                    'datetime': datetime.now().isoformat()
                })
                logger.warning(f"慢查询检测: {function_name} 耗时 {execution_time:.3f}s")
    
    def get_function_stats(self, function_name: str) -> Dict:
        """获取特定函数的统计信息"""
        with self._lock:
            records = list(self._metrics[function_name])
        
        if not records:
            return {
                'function_name': function_name,
                'total_calls': 0,
                'success_rate': 0,
                'avg_execution_time': 0,
                'min_execution_time': 0,
                'max_execution_time': 0,
                'recent_errors': []
            }
        
        execution_times = [r['execution_time'] for r in records]
        successful_calls = sum(1 for r in records if r['success'])
        recent_errors = [r['error'] for r in records[-10:] if r['error']]
        
        return {
            'function_name': function_name,
            'total_calls': len(records),
            'success_rate': round(successful_calls / len(records) * 100, 2),
            'avg_execution_time': round(sum(execution_times) / len(execution_times), 3),
            'min_execution_time': round(min(execution_times), 3),
            'max_execution_time': round(max(execution_times), 3),
            'recent_errors': recent_errors[-5:]  # 最近5个错误
        }
    
    def get_overall_stats(self) -> Dict:
        """获取整体统计信息"""
        with self._lock:
            all_functions = list(self._metrics.keys())
        
        total_calls = 0
        total_errors = 0
        all_execution_times = []
        
        function_stats = []
        for func_name in all_functions:
            stats = self.get_function_stats(func_name)
            function_stats.append(stats)
            total_calls += stats['total_calls']
            total_errors += stats['total_calls'] - int(stats['total_calls'] * stats['success_rate'] / 100)
            
            # 收集所有执行时间
            records = list(self._metrics[func_name])
            all_execution_times.extend([r['execution_time'] for r in records])
        
        uptime = time.time() - self._start_time
        
        return {
            'uptime_seconds': round(uptime, 2),
            'uptime_hours': round(uptime / 3600, 2),
            'total_functions': len(all_functions),
            'total_calls': total_calls,
            'total_errors': total_errors,
            'overall_success_rate': round((total_calls - total_errors) / total_calls * 100, 2) if total_calls > 0 else 100,
            'avg_response_time': round(sum(all_execution_times) / len(all_execution_times), 3) if all_execution_times else 0,
            'functions': function_stats
        }
    
    def get_recent_activity(self, minutes: int = 10) -> List[Dict]:
        """获取最近活动
        
        Args:
            minutes: 时间范围（分钟）
            
        Returns:
            最近活动列表
        """
        cutoff_time = time.time() - (minutes * 60)
        recent_activity = []
        
        with self._lock:
            for func_name, records in self._metrics.items():
                for record in records:
                    if record['timestamp'] >= cutoff_time:
                        recent_activity.append({
                            'function_name': func_name,
                            'timestamp': record['timestamp'],
                            'datetime': record.get('datetime', ''),
                            'execution_time': record['execution_time'],
                            'success': record['success'],
                            'error': record['error']
                        })
        
        # 按时间排序
        recent_activity.sort(key=lambda x: x['timestamp'], reverse=True)
        return recent_activity[:50]
    
    def get_slow_queries(self, limit: int = 20) -> List[Dict]:
        """获取慢查询列表
        
        Args:
            limit: 返回数量限制
            
        Returns:
            慢查询列表
        """
        with self._lock:
            queries = list(self._slow_queries)
        
        queries.sort(key=lambda x: x['execution_time'], reverse=True)
        return queries[:limit]
    
    def clear_metrics(self):
        """清空所有监控数据"""
        with self._lock:
            self._metrics.clear()
            self._slow_queries.clear()
            logger.info("性能监控数据已清空")
    
    def get_function_percentiles(self, function_name: str) -> Dict:
        """获取函数执行时间的百分位数
        
        Args:
            function_name: 函数名称
            
        Returns:
            百分位数统计
        """
        with self._lock:
            records = list(self._metrics[function_name])
        
        if not records:
            return {'p50': 0, 'p90': 0, 'p95': 0, 'p99': 0}
        
        import numpy as np
        execution_times = [r['execution_time'] for r in records]
        
        return {
            'p50': round(np.percentile(execution_times, 50), 3),
            'p90': round(np.percentile(execution_times, 90), 3),
            'p95': round(np.percentile(execution_times, 95), 3),
            'p99': round(np.percentile(execution_times, 99), 3)
        }


# 全局性能监控实例
performance_monitor = PerformanceMonitor()

def monitor_performance(function_name: Optional[str] = None):
    """性能监控装饰器"""
    def decorator(func):
        name = function_name or f"{func.__module__}.{func.__name__}"
        
        @wraps(func)
        def wrapper(*args, **kwargs):
            start_time = time.time()
            error = None
            success = True
            
            try:
                result = func(*args, **kwargs)
                return result
            except Exception as e:
                success = False
                error = str(e)
                logger.error(f"函数 {name} 执行失败: {error}")
                raise
            finally:
                execution_time = time.time() - start_time
                performance_monitor.record_execution(name, execution_time, success, error)
                
                if execution_time > 1.0:  # 执行时间超过1秒的记录警告
                    logger.warning(f"函数 {name} 执行时间较长: {execution_time:.3f}s")
        
        return wrapper
    return decorator