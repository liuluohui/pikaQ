/*
 * Copyright (C) 2015 knightliao, Inc. All Rights Reserved.
 */
package com.baidu.pikaq.client.test.tests.simpletest.error;

import java.math.BigDecimal;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.BeforeTransaction;

import com.baidu.pikaq.client.test.common.BaseTestCase;
import com.baidu.pikaq.client.test.mock.PikaQGatewayWithExceptionMockImpl;
import com.baidu.pikaq.client.test.service.campaign.bo.Campaign;
import com.baidu.pikaq.client.test.service.campaign.service.CampaignMgr;
import com.baidu.pikaq.client.test.service.pikadata.bo.PikaData;
import com.baidu.pikaq.client.test.service.pikadata.dao.PikaDataDao;

import junit.framework.Assert;

/**
 * Created by knightliao on 15/7/27.
 * <p/>
 * 使用 RabbitMq，校验在 Q事务异常时，是否有保存有 pikadata
 */
public class RabbitQCreateQErrorTestCase extends BaseTestCase {

    protected static final Logger LOGGER = LoggerFactory.getLogger(RabbitQCreateQErrorTestCase.class);

    @Autowired
    private CampaignMgr campaignMgr;

    @Autowired
    private PikaDataDao pikaDataDao;

    private static long RANDOM_DATA = RandomUtils.nextInt(10000000);

    private static String campaignName = "campaign" + String.valueOf(RANDOM_DATA);

    /**
     * 错误处理业务：订单生成，发送消息，（强一致性）
     * <p/>
     * 测试：数据库无此条数据，消息也不存在
     *
     * @throws Exception
     */
    @Test
    public void test() {
    }

    @BeforeTransaction
    public void test_() {
        try {
            campaignMgr.createWithQErrorRabbitQ(campaignName, BigDecimal.valueOf(RANDOM_DATA));
        } catch (Throwable e) {
            LOGGER.info("");
        }
    }

    /**
     * Q 有异常，则本地事务会提交成功，但是 消息失败了，status data会有数据
     */
    @After
    public void check() {

        checkDbData(campaignName);
        checkQData(campaignName);
    }

    /**
     * check Q 无数据
     *
     * @param campaignName
     */
    public void checkQData(String campaignName) {

        Object data = PikaQGatewayWithExceptionMockImpl.consumeOne();

        LOGGER.info("checking Q data.......");
        if (data != null) {
            Assert.assertTrue(false);

        } else {
            Assert.assertTrue(true);

            // 必须要有 status data
            checkPikaData();
        }
    }

    /**
     * 没有 Pikadata
     */
    private void checkPikaData() {

        String correlation = PikaQGatewayWithExceptionMockImpl.getPreviousCorrelation();

        PikaData pikaData = pikaDataDao.getByCorrelation(correlation);
        Assert.assertNull(pikaData);
    }

    /**
     * check 数据库没有数据
     */
    private void checkDbData(String campaignName) {

        LOGGER.info("checking Db data.......");
        //
        // check 数据库有数据
        //
        Campaign campaign = campaignMgr.getByName(campaignName);
        Assert.assertNull(campaign);
    }
}
