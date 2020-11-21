package me.chanjar.weixin.common.util.locks;

import me.chanjar.weixin.common.error.WxRuntimeException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.util.Pool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * JedisPool 分布式锁
 *
 * @author <a href="https://github.com/007gzs">007</a>
 */
public class JedisDistributedLock implements Lock {

  private final Pool<Jedis> jedisPool;

  private final InnerJedisLock lock;

  public JedisDistributedLock(Pool<Jedis> jedisPool, String key) {
    this.jedisPool = jedisPool;
    this.lock = new InnerJedisLock(key);
  }

  @Override
  public void lock() {

    try (Jedis jedis = jedisPool.getResource()) {
      if (!lock.acquire(jedis)) {
        throw new WxRuntimeException("acquire timeout");
      }
    } catch (InterruptedException e) {
      throw new WxRuntimeException("lock failed", e);
    }
  }

  @Override
  public void lockInterruptibly() throws InterruptedException {
    try (Jedis jedis = jedisPool.getResource()) {
      if (!lock.acquire(jedis)) {
        throw new WxRuntimeException("acquire timeouted");
      }
    }
  }

  @Override
  public boolean tryLock() {
    try (Jedis jedis = jedisPool.getResource()) {
      return lock.acquire(jedis, 0);
    } catch (InterruptedException e) {
      throw new WxRuntimeException("lock failed", e);
    }
  }

  @Override
  public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
    try (Jedis jedis = jedisPool.getResource()) {
      return lock.acquire(jedis, unit.toNanos(time));
    }
  }

  @Override
  public void unlock() {
    try (Jedis jedis = jedisPool.getResource()) {
      lock.release(jedis);
    }
  }

  @Override
  public Condition newCondition() {
    throw new WxRuntimeException("unsupported method");
  }


  static class InnerJedisLock {

    String lockKey;

    // 是否获取锁
    private volatile boolean locked;

    // 默认锁超时时间(1分钟)
    private final int defaultExpireTimes = 60 * 1000;

    // 默认锁等待时间（6秒）
    private final int defaultTimeoutTimes = 6 * 1000;


    public InnerJedisLock(String lockKey) {
      this.lockKey = lockKey;
    }

    public boolean acquire(Jedis jedis) throws InterruptedException {

      return acquire(jedis, defaultTimeoutTimes);
    }

    /**
     * 获取锁，成功返回true,未获取锁返回false
     *
     * @param jedis   jedis
     * @param timeout 超时时间
     * @return bool, 是否获取锁
     */
    public boolean acquire(Jedis jedis, long timeout) throws InterruptedException {

      timeout = timeout < 0 ? defaultTimeoutTimes : timeout;

      while (timeout >= 0) {

        String expiresValue = String.valueOf(System.currentTimeMillis() + defaultExpireTimes + 1);

        //若设值成功，表明获取锁
        if (jedis.setnx(lockKey, expiresValue) == 1) {
          locked = true;
          return true;
        }

        //否则查看锁是否过期
        String currentValueStr = jedis.get(lockKey);
        if (currentValueStr != null && Long.parseLong(currentValueStr) < System.currentTimeMillis()) {

          //锁已过期，尝试设置锁
          String oldValueStr = jedis.getSet(lockKey, expiresValue);
          // 若锁原值与所设值相同，表明已获取锁
          if (currentValueStr.equals(oldValueStr)) {
            locked = true;
            return true;
          }
        }
        timeout -= 100;
        if (timeout >= 0) {
          Thread.sleep(100);
        }
      }
      return false;
    }


    /**
     * 释放锁
     * 没有严格判断该锁是否属于释放者，在超时时间外仍未释放的锁被认为该持有者已奔溃或被杀死
     *
     * @param jedis jedis
     */
    public void release(Jedis jedis) {

      if (locked) {

        String oldValueStr = jedis.get(lockKey);
        //说明锁还没有过期，可以正常释放
        if (oldValueStr != null && Long.parseLong(oldValueStr) > System.currentTimeMillis()) {
          jedis.del(lockKey);
        }
        locked = false;
      }
    }
  }


}
