package com.nerchain.springwebsocketsstomp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.session.Session;
import org.springframework.session.web.socket.config.annotation.AbstractSessionWebSocketMessageBrokerConfigurer;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

@Configuration
@EnableWebSocketMessageBroker
public class SessionSocketsStompConfig extends AbstractSessionWebSocketMessageBrokerConfigurer<Session> {

    /**
     * 设置一个SockJS端点链接
     * /socket
     * setAllowedOrigins 跨域设置
     *
     * @param registry 端点注册器
     */
    public void configureStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/socket").setAllowedOrigins("*").withSockJS();
    }

    /**
     * 消息规则,可以理解成消息 订阅地址前缀的
     * <p>
     * 总要 (red) registry.setPathMatcher(new AntPathMatcher("."));
     * 因为 rabbitmq是不支持URL "/" 分隔符,所以我们要自定义它的分隔符为 ".",如果不定义就会报错.
     * 比如订阅 /amq/queue/delayed.trade 过期订单
     * <p>
     * .setUserDestinationBroadcast("/topic/unresolved.user.destination")
     * .setUserRegistryBroadcast("/topic/registry.broadcast")
     * 多项目设置,比如A 端点给 B端点用户发送,需要设置的
     * <p>
     * 参考
     * <a>https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#websocket-stomp-enable</a>
     *
     * @param registry 端点设置
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        registry.setPathMatcher(new AntPathMatcher("."));

        registry.enableStompBrokerRelay("/topic", "/queue", "/exchange", "/amq/queue")
                // 其他服务访问桥连,一但这里没有找到可以去这里推送给其他用户
                .setUserDestinationBroadcast("/topic/unresolved.user.destination")
                .setUserRegistryBroadcast("/topic/registry.broadcast")
                .setAutoStartup(true);
        registry.setApplicationDestinationPrefixes("/app");
        registry.setPreservePublishOrder(true);
    }
}
