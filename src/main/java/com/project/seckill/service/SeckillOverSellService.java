package com.project.seckill.services;

import com.project.seckill.db.dao.SeckillActivityDao;
import com.project.seckill.db.po.SeckillActivity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SeckillOverSellService {
    @Autowired
    private SeckillActivityDao seckillActivityDao;

    public String  processSeckill(long activityId) {
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(activityId);
        long availableStock = seckillActivity.getAvailableStock();
        String result;
        if (availableStock > 0) {
            result = "Congradulations! Order successfully placed";
            System.out.println(result);
            availableStock = availableStock - 1;
            seckillActivity.setAvailableStock(new Integer("" + availableStock));
            seckillActivityDao.updateSeckillActivity(seckillActivity);
        } else {
            result = "Sorry, out of stock!";
            System.out.println(result);
        }
        return result;
    }
}
