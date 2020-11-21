package me.chanjar.weixin.open.api.impl;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.TimeUnit;

/**
 * @author wangtonghe
 * @date 2020/11/19 2:39 下午
 */
@Slf4j
public class WxOpenLockTest {

  private WxOpenServiceImpl wxOpenService;

  private JedisPool pool;


  @BeforeClass
  public void initOpenService() {


    wxOpenService = new WxOpenServiceImpl();

//
//    WxOpenInRedisConfigStorage inRedisConfigStorage = new WxOpenInRedisConfigStorage(pool);
//    inRedisConfigStorage.setWxOpenInfo("wxefddc5767b8857f1", "a87c926660cc311f4377aee9e3c2c186",
//      "LbDBC0x1nVC", "0ZR6gCH16Hhm4EKWwfARtCHz4pigQGvsU6MTpKU3Hpj");
//    inRedisConfigStorage.setComponentVerifyTicket("ticket@@@FsrSMA3eTDbfKGRqXHc6hh9CYzFZobr26D1mKCagtvH9gI3eg8oKegmXZKxKlJ-NBHWpWlsNeVHeRAFFi9_PRA");

    Config config = new Config();
    config.useSingleServer().setAddress("redis://116.196.112.11:21551").setPassword("super0!@#456");
    RedissonClient redissonClient = Redisson.create(config);

    WxOpenInRedissonConfigStorage inRedisConfigStorage = new WxOpenInRedissonConfigStorage(redissonClient);
    inRedisConfigStorage.setWxOpenInfo("wxefddc5767b8857f1", "a87c926660cc311f4377aee9e3c2c186",
      "LbDBC0x1nVC", "0ZR6gCH16Hhm4EKWwfARtCHz4pigQGvsU6MTpKU3Hpj");
    inRedisConfigStorage.setComponentVerifyTicket("ticket@@@FsrSMA3eTDbfKGRqXHc6hh9CYzFZobr26D1mKCagtvH9gI3eg8oKegmXZKxKlJ-NBHWpWlsNeVHeRAFFi9_PRA");


    wxOpenService.setWxOpenConfigStorage(inRedisConfigStorage);

  }



}
