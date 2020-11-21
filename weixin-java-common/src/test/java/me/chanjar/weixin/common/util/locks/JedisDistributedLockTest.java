package me.chanjar.weixin.common.util.locks;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.CountDownLatch;

/**
 * @author wangtonghe
 * @date 2020/11/20 4:25 下午
 */
@Slf4j
public class JedisDistributedLockTest {

  private JedisDistributedLock jedisDistributedLock;


  volatile String accessToken;


  @BeforeTest
  public void init() {

    String lockKey = "accessTokenLock";

    JedisPool jedisPool = new JedisPool(new GenericObjectPoolConfig(), "116.196.112.11", 21551, 2000, "super0!@#456");
    jedisDistributedLock = new JedisDistributedLock(jedisPool, lockKey);

  }

  @Test(description = "模拟多个线程并发获取accessToken")
  public void testLock() {

    int threadCount = 3;

    CountDownLatch countDownLatch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {

      try {
        new Thread(() -> {

          log.info("线程{}开始", Thread.currentThread().getName());

          if (StringUtils.isNotEmpty(accessToken)) {
            log.info("直接获取:{}", accessToken);
            return;
          }

          jedisDistributedLock.lock();

          try {

            log.info("线程{}获取到锁", Thread.currentThread().getName());


            if (StringUtils.isNotEmpty(accessToken)) {
              log.info("直接获取2:{}", accessToken);
            } else {

              // 模拟获取accessToken
              accessToken = Thread.currentThread().getName();

            }



          } catch (Exception e) {
            log.error("获取锁失败", e);
          } finally {
            jedisDistributedLock.unlock();
            countDownLatch.countDown();

          }

        }).start();

      } catch (Exception e) {
        log.error("外部报错", e);
      }
    }

    try {
      countDownLatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }


}
