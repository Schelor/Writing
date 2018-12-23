## 基础配置
依赖
```xml
<dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>javax.servlet-api</artifactId>
            <version>3.1.0</version>
</dependency>
```
Web.xml配置
```xml
<web-app xmlns="http://java.sun.com/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://java.sun.com/xml/ns/javaee
                      http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd"
         version="3.0">

    <description>The Java Web Examples Conf</description>
    <display-name>Servlet and Filter Examples</display-name>

    <!-- Define example application events listeners -->
    <listener>
        <listener-class>base.listener.ContextListener</listener-class>
    </listener>
    <!-- Define example filters -->
    <filter>
        <filter-name>Filter1</filter-name>
        <filter-class>base.filter.Filter1</filter-class>
        <init-param>
            <param-name>p1</param-name>
            <param-value>v1</param-value>
        </init-param>
        <init-param>
            <param-name>p2</param-name>
            <param-value>100</param-value>
        </init-param>
    </filter>

    <filter-mapping>
        <filter-name>Filter1</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

    <filter>
        <filter-name>Filter2</filter-name>
        <filter-class>base.filter.Filter2</filter-class>
    </filter>

    <filter-mapping>
        <filter-name>Filter2</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

    <!-- Define Servlet -->
    <servlet>
        <servlet-name>Servlet1</servlet-name>
        <servlet-class>base.servlet.Servlet1</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>

    <servlet-mapping>
        <servlet-name>Servlet1</servlet-name>
        <url-pattern>/s1</url-pattern>
    </servlet-mapping>

    <servlet>
        <servlet-name>Servlet2</servlet-name>
        <servlet-class>base.servlet.Servlet2</servlet-class>

    </servlet>

    <servlet-mapping>
        <servlet-name>Servlet2</servlet-name>
        <url-pattern>/s2</url-pattern>
    </servlet-mapping>

    <welcome-file-list>
        <welcome-file>index.jsp</welcome-file>
    </welcome-file-list>


</web-app>

```

### Filter执行顺序
1. init初始化顺序：根据filter-name在web.xml中的配置先后,配置在前则先执行init
2. doFilter执行顺序：根据filter-mapping在web.xml中配置的先后,配置在前则先执行doFilter
