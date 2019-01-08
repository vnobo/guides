##Spring WebSockets STOMP 代理模式集成 RabbitMQ
----

###环境
> [java 11](https://openjdk.java.net/projects/jdk/11/)

> [gradle](https://gradle.org/)

> [spring boot 2.1.1](https://spring.io/projects/spring-boot)

> [RabbitMQ 服务](https://www.rabbitmq.com/#getstarted)
>>需要[STOMP](https://www.rabbitmq.com/stomp.html)插件安装

>>如果你需要定时消息,需要安装 [rabbitmq_delayed_message_exchange](https://www.rabbitmq.com/community-plugins.html#rabbitmq_delayed_message_exchange)
插件
---

###简要说明
1. 依赖包安装:
    ```implementation('org.springframework.boot:spring-boot-starter-web') // web模块
     implementation ('org.springframework.boot:spring-boot-starter-thymeleaf')
     
     implementation('org.springframework.boot:spring-boot-starter-security') // 安全模块
     implementation('org.springframework.security:spring-security-messaging')
     
     implementation('org.springframework.boot:spring-boot-starter-websocket') // websocket
     
     implementation('org.springframework.session:spring-session-data-redis')// spring session data redis 已支持认证多项目共享
     implementation('io.lettuce:lettuce-core')
     ```
2. 代理设置类[SessionSocketsStompConfig](src/main/java/com/nerchain/springwebsocketsstomp/config/SessionSocketsStompConfig.java)
   ```
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
      ```
   AbstractSessionWebSocketMessageBrokerConfigurer启用Session的管理,它很简单就是管理用户认证后Session的生命周期.
    
3. Sockets安全配置类[SocketSecurityConfig](src/main/java/com/nerchain/springwebsocketsstomp/config/SocketSecurityConfig.java)
    ```@Configuration
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
            * sockets 权限消息
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
       ```
### 关于
> 我没有写客户端实现,由于时间问题,暂时没有实现.
这是一个最基本的代理服务链接RabbitMQ的实现,它可以跨域,也集成了安全,重要的知识点都涵盖了.
关于定时这个消息是rabbitmq_delayed_message_exchange这个插件实现的,后面我会详细介绍.
#### 对于不明白的可以加我QQ询问.
### 示例代码 [github](https://github.com/vnobo/guides/tree/master/spring-websockets-stomp) [gitee](https://gitee.com/vno/guides/tree/master/spring-websockets-stomp)
###作者
> 云舒 QQ:5199840 