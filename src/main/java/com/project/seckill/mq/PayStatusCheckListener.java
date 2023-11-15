
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

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RocketMQMessageListener(topic = "pay_check", consumerGroup = "pay_check_group")
public class PayStatusCheckListener implements RocketMQListener<MessageExt> {
    @Autowired
    private OrderDao orderDao;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Resource
    private RedisService redisService;

    @Override
    @Transactional
    public void onMessage(MessageExt messageExt) {
        String message = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        log.info("Received the Request for checking payment:" + message);
        Order order = JSON.parseObject(message, Order.class);
        //1.Check the order
        Order orderInfo = orderDao.queryOrder(order.getOrderNo());
        //2.us paid?
        if (orderInfo.getOrderStatus() != 2) {
            //3.if no, close the order
            log.info("Payment Failed, Order No：" + orderInfo.getOrderNo());
            orderInfo.setOrderStatus(99);
            orderDao.updateOrder(orderInfo);
            //4.revert the db stock
            seckillActivityDao.revertStock(order.getSeckillActivityId());
            // revert the redis stock
            redisService.revertStock("stock:" + order.getSeckillActivityId());
            //5.remove the user from the buying limit list
            redisService.removeLimitMember(order.getSeckillActivityId(), order.getUserId());
        }
    }
}

