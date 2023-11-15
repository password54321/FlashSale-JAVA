package com.project.seckill.db.dao;

import com.project.seckill.db.po.Order;

public interface OrderDao {

    void insertOrder(Order order);

    Order queryOrder(String orderNo);

    void updateOrder(Order order);
}
