package com.project.seckill.mq;

import com.alibaba.fastjson.JSON;
import com.project.seckill.db.dao.OrderDao;
import com.project.seckill.db.dao.SeckillActivityDao;
import com.project.seckill.db.po.Order;
import com.project.seckill.util.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
@RocketMQMessageListener(topic = "seckill_order", consumerGroup = "seckill_order_group")
public class OrderConsumer implements RocketMQListener<MessageExt> {
    @Autowired
    private OrderDao orderDao;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Autowired
    RedisService redisService;

    @Override
    @Transactional
    public void onMessage (MessageExt messageExt) {
        //1.Process the MQ message
        String message = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        log.info("Received the reqeust for creating a order：" + message);
        Order order = JSON.parseObject(message, Order.class);
        order.setCreateTime(new Date());
        //2.Deduct the stock
        boolean lockStockResult = seckillActivityDao.lockStock(order.getSeckillActivityId());
        if (lockStockResult) {
            //if order status 0: no stock 1:waiting on the payment
            order.setOrderStatus(1);
            // add the user to the buying limit list
            redisService.addLimitMember(order.getSeckillActivityId(), order.getUserId());
        } else {
            order.setOrderStatus(0);
        }
        //3.insert the order
        orderDao.insertOrder(order);
    }
}
