package com.xuecheng.manage_cms_client.mq;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.manage_cms_client.dao.CmsPageRepository;
import com.xuecheng.manage_cms_client.service.PageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * 接收rabbitmq消息的类
 */

@Component
public class ConsumerPostPage {

    private static final Logger LOGGER = LoggerFactory.getLogger(Component.class);

    @Autowired
    private PageService pageService;

    @Autowired
    private CmsPageRepository cmsPageRepository;


    /**
     * 监听rabbitmq消息的方法
     * @param msg
     * @param message
     * @param channel
     */
    @RabbitListener(queues = {"${xuecheng.mq.queue}"})
    public void postPage(String msg, Message message, Channel channel) {
        Map map = JSON.parseObject(msg, Map.class);
        LOGGER.info("receive cms post page:{}",msg.toString());
        String pageId = (String)map.get("pageId");
        Optional<CmsPage> optional = cmsPageRepository.findById(pageId);
        if (!optional.isPresent()){
            LOGGER.error("receive cms post page,cmsPage is null:{}",msg.toString());
            return;
        }
        //保存页面
        pageService.savePageToServerPath(pageId);
    }
}
