package com.nerchain.springwebsocketsstomp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

import java.util.List;
import java.util.Map;

@Configuration
public class SocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {


    @Autowired
    private FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    /**
     * 因为我们采用spring-session 头x-auth-token 认证
     * 那么传统的web认证就无法,获取到消息认证信息了.
     * 所以我们要从它的session获取认证信息手动增加到消息上.
     * 这样消息就具有认证信息了.
     * 我们可以参考spring
     * https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#websocket-stomp-authentication-token-based
     *
     * @param registration 消息请求体
     */
    @Override
    protected void customizeClientInboundChannel(ChannelRegistration registration) {

        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {

                // 获取消息头
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // 获取 x-auth-token session id
                List<String> sessions = accessor != null ? accessor.getNativeHeader("x-auth-token") : null;

                if (sessions == null) return message;

                //根据 session id 获取它的认证信息
                Session session = sessionRepository.findById(sessions.get(0));
                if (session == null) return message;

                //根据 session id 获取它的认证信息 SPRING_SECURITY_CONTEXT
                SecurityContext securityContext = session.getAttribute("SPRING_SECURITY_CONTEXT");

                if (securityContext == null) return message;

                //根据 SPRING_SECURITY_CONTEXT  获取用户的认证信息 Authentication
                Authentication user = securityContext.getAuthentication();

                //判断用户是否已认证,一般获取到的是已认证的用户,也就是只要存在,用户肯定被认证了.
                //但是 如果你设置了用户过期,没有设置清理session 用户还是存在的.这时候就必须去判断用户的认证状态,不然会报无权限错误
                if (user.isAuthenticated()) {

                    // 重要这里 我们获取的认证信息,手动赋值给它.
                    accessor.setUser(user);

                    /*根据spring session sockets API 将x-auth-token 放入sockets 会话
                       让spring sockets 代理托管我们的session 会话生命周期.若果用户没有断开链接
                       session 是不会过期的具体查看 @SessionSocketsStompConfig 代码*/
                    Map<String, Object> attributes = accessor.getSessionAttributes();
                    if (attributes != null) {
                        attributes.put("SPRING.SESSION.ID", sessions.get(0));
                        accessor.setSessionAttributes(attributes);
                    }
                } else {
                    throw new BadCredentialsException("Your authentication information has expired please login again.");
                }
                return message;
            }
        });
    }

    /**
     * sockets 消息权限规则声明
     *
     * @param messages 消息体
     */
    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
                .simpSubscribeDestMatchers("/user/queue/errors", "").permitAll()
                .simpDestMatchers("/app/**").hasRole("USER")
                .simpSubscribeDestMatchers("/user/**", "/queue/**").hasRole("USER");
    }

    /**
     * 关闭Sockets 的CSRF 保护
     *
     * @return true
     */
    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }

}
