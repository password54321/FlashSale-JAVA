package com.project.seckill.service;

import com.alibaba.fastjson.JSON;
import com.project.seckill.db.dao.OrderDao;
import com.project.seckill.db.dao.SeckillActivityDao;
import com.project.seckill.db.dao.SeckillCommodityDao;
import com.project.seckill.db.po.Order;
import com.project.seckill.db.po.SeckillActivity;
import com.project.seckill.db.po.SeckillCommodity;
import com.project.seckill.mq.RocketMQService;
import com.project.seckill.util.RedisService;
import com.project.seckill.util.SnowFlake;
import com.sun.org.apache.xpath.internal.operations.Or;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class SeckillActivityService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Autowired
    private RocketMQService rocketMQService;

    @Autowired
    SeckillCommodityDao seckillCommodityDao;

    @Autowired
    OrderDao orderDao;

    /**
     * datacenterId;  
     * machineId;     
     * 
     * snowFlake generate IDs
     */
    private SnowFlake snowFlake = new SnowFlake(1, 1);

    /**
     * create order
     *
     * @param seckillActivityId
     * @param userId
     * @return
     * @throws Exception
     */
    public Order createOrder(long seckillActivityId, long userId) throws Exception {
        /*
         * 1.create order
         */
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        Order order = new Order();
        // snowflake
        order.setOrderNo(String.valueOf(snowFlake.nextId()));
        order.setSeckillActivityId(seckillActivity.getId());
        order.setUserId(userId);
        order.setOrderAmount(seckillActivity.getSeckillPrice().longValue());
        /*
         *2.send order
         */
        rocketMQService.sendMessage("seckill_order", JSON.toJSONString(order));

        /*
         * 3.checkpayment
         * Free version of RocketMQ, time delay options：
         * messageDelayLevel=1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
         */
        rocketMQService.sendDelayMessage("pay_check", JSON.toJSONString(order), 3);

        return order;
    }

    /**
     * Check the stock
     *
     * @param activityId 
     * @return
     */
    public boolean seckillStockValidator(long activityId) {
        String key = "stock:" + activityId;
        return redisService.stockDeductValidator(key);
    }

    /**
     * order created
     *
     * @param orderNo
     */
    public void payOrderProcess(String orderNo) throws Exception {
        log.info("Order Payment Received! Order No：" + orderNo);
        Order order = orderDao.queryOrder(orderNo);
        /*
         * 1.check the order existence
         * 2.check payment 
         */
        if (order == null) {
            log.error("Order can not found：" + orderNo);
            return;
        } else if(order.getOrderStatus() != 1 ) {
            log.error("Invaild Order：" + orderNo);
            return;
        }
        /*
         * 2.Payment Check
         */
        order.setPayTime(new Date());
        // Set 0 if no stock, 1, waiting on payment 2, payment received
        order.setOrderStatus(2);
        orderDao.updateOrder(order);
        /*
         *3.send the payment result
         */
        rocketMQService.sendMessage("pay_done", JSON.toJSONString(order));
    }
}
