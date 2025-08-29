package com.calendar.app.config;

import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        
        tomcat.addConnectorCustomizers(connector -> {
            Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
            
            // SSL/TLS 요청 처리 설정
            protocol.setSSLEnabled(false);
            
            // 연결 타임아웃 설정
            protocol.setConnectionTimeout(60000);
            
            // 최대 연결 수 설정
            protocol.setMaxConnections(8192);
            
            // 요청 처리 설정
            protocol.setMaxHttpHeaderSize(8192);
            protocol.setMaxSwallowSize(2097152); // 2MB
            
            // SSL/TLS 요청 무시 설정
            protocol.setRelaxedPathChars("|,{,},[,],\\");
            protocol.setRelaxedQueryChars("|,{,},[,],\\");
        });
        
        return tomcat;
    }
}
