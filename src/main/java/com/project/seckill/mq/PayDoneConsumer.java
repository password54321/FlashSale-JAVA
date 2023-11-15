package com.project.seckill.mq;

import com.alibaba.fastjson.JSON;
import com.project.seckill.db.dao.SeckillActivityDao;
import com.project.seckill.db.po.Order;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

/**
 * After the payment is processed
 * Deduct the stock
 */
@Slf4j
@Component
@Transactional
@RocketMQMessageListener(topic = "pay_done", consumerGroup = "pay_done_group")
public class PayDoneConsumer implements RocketMQListener<MessageExt> {

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Override
    public void onMessage(MessageExt messageExt) {
      
        String message = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        log.info("Recived the request for creating the order：" + message);
        Order order = JSON.parseObject(message, Order.class);
     
        seckillActivityDao.deductStock(order.getSeckillActivityId());
    }
}
