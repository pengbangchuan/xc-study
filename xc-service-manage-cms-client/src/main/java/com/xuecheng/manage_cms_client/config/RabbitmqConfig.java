package com.xuecheng.manage_cms_client.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitmqConfig {

    //配置队列bean的名称
    private static final String QUEUE_CMS_POSTPAGE= "queue_cms_postpage";
    //交换机的名称
    private static final String EX_ROUTING_CMS_POSTPAGE="ex_routing_cms_postpage";
    //路由key，routingKey 站点id
    @Value("${xuecheng.mq.routingKey}")
    private String routingKey;
    //队列名称
    @Value("${xuecheng.mq.queue}")
    private String queue_cms_postpage_name;

    //声明交换机添加到spring容器
    @Bean(EX_ROUTING_CMS_POSTPAGE)
    public Exchange EXCHANGE_TOPICS_INFORM(){
        //durable(true) 持久化，mq重启之后交换机还在
        return ExchangeBuilder.topicExchange(EX_ROUTING_CMS_POSTPAGE).durable(true).build();
    }

    //声明队列添加到spring容器
    @Bean(QUEUE_CMS_POSTPAGE)
    public Queue QUEUE_CMS_POSTPAGE(){
        return new Queue(queue_cms_postpage_name);
    }

    // 队列和交换机绑定 还有routingKey
    @Bean
    public Binding BINDING_QUEUE_INFORM_EMAIL(@Qualifier(QUEUE_CMS_POSTPAGE) Queue queue,
                                              @Qualifier(EX_ROUTING_CMS_POSTPAGE) Exchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with(routingKey).noargs();
    }

}
