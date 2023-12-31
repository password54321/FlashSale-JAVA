package com.project.seckill;

import com.project.seckill.util.RedisService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class RedisDemoTest {

    @Resource
    private RedisService redisService;

    @Test
    public void stockTest() {
        redisService.setValue("stock:19", 10L);
    }

    @Test
    public void getStockTest() {
        String stock = redisService.getValue("stock:19");
        System.out.println(stock);
    }

    @Test
    public void stockDeductValidatorTest() {
        boolean result = redisService.stockDeductValidator("stock:19");
        System.out.println("result:" + result);
        String stock = redisService.getValue("stock:19");
        System.out.println("stock:" + stock);
    }


    @Test
    public void revertStock() {
        String stock = redisService.getValue("stock:19");
        System.out.println("回滚库存之前的库存：" + stock);

        redisService.revertStock("stock:19");

        stock = redisService.getValue("stock:19");
        System.out.println("回滚库存之后的库存：" + stock);
    }

    @Test
    public void removeLimitMember() {
        redisService.removeLimitMember(19, 1234);
    }
}
