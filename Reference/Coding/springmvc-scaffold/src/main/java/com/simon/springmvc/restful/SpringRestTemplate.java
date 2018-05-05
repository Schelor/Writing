package com.simon.springmvc.restful;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.http.*;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.SSLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xiele
 * @date 2018/03/30
 */
public class SpringRestTemplate {

    public static void main(String[] args) throws SSLException {

        RestTemplate template = new RestTemplate();
        Netty4ClientHttpRequestFactory factory = new Netty4ClientHttpRequestFactory();
        SslContext ssl = SslContextBuilder.forClient().build();
        factory.setSslContext(ssl);
        template.setRequestFactory(factory);

        String url = "https://www.baidu.com";

        // 添加请求头
        HttpHeaders requestHeaders = new HttpHeaders();
        List<MediaType> medias = new ArrayList<>();
        medias.add(MediaType.TEXT_PLAIN);
        medias.add(MediaType.ALL);
        requestHeaders.setAccept(medias);
        requestHeaders.set("Cache-Control", "no-cache");
        requestHeaders.setConnection("keep-alive");

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("ie", "utf-8")
                .queryParam("wd", "RestTemplate")
                ;

        // 请求实体：header + body
        HttpEntity<String> requestEntity = new HttpEntity(requestHeaders);

        ResponseEntity<String> response = template.exchange(builder.toUriString(), HttpMethod.GET, requestEntity, String.class);
        template.exchange(builder.toUriString(), HttpMethod.GET, requestEntity, String.class);
        template.exchange(builder.toUriString(), HttpMethod.GET, requestEntity, String.class);
        //ResponseEntity<String> response = template.getForEntity(builder.toUriString(), String.class);
        System.out.println(response);
    }
}
