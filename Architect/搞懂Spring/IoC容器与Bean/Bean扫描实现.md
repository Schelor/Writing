# Bean扫描实现

<a name="BKrtm"></a>
## 前言
Spring可以通过两种方式启用Bean的自动扫描<br />一种是:
> <context:component-scan base-package='xxx'/> 

另一种是:
> @ComponentScan('xxx')



<a name="UBVlj"></a>
## context:component-scan实现原理
配置这个标签，spring会开启注解扫描
> By default, the Spring-provided @Component, @Repository, @Service,
> @Controller, @RestController, @ControllerAdvice, and @Configuration stereotypes
> will be detected.
> 隐式开启注解配置：<context:annotation-config/>，并激活@Required,
> @Autowired, @PostConstruct, @PreDestroy, @Resource, @PersistenceContext and @PersistenceUnit这几个注解。


<br />**标签处理器: ComponentScanBeanDefinitionParser**<br />注册Component处理逻辑如下:<br />component-scan处理annotation-config=true, 表示默认会注册AnnotationPostProcessors
```java
// Register annotation config processors, if necessary.

boolean annotationConfig = true;
if (element.hasAttribute(ANNOTATION_CONFIG_ATTRIBUTE)) {
    annotationConfig = Boolean.parseBoolean(element.getAttribute(ANNOTATION_CONFIG_ATTRIBUTE));
}
if (annotationConfig) {
    Set<BeanDefinitionHolder> processorDefinitions =
        AnnotationConfigUtils.registerAnnotationConfigProcessors(readerContext.getRegistry(), source);
    for (BeanDefinitionHolder processorDefinition : processorDefinitions) {
        compositeDef.addNestedComponent(new BeanComponentDefinition(processorDefinition));
    }
}
```
通过AnnotationConfigUtils.registerAnnotationConfigProcessors方法注册多个注解处理器BeanDefinition。
```java
Set<BeanDefinitionHolder> beanDefs = new LinkedHashSet<>(8);

// xml的方式仍然兼容: @Confirguration @ComponentScan @Import 等
if (!registry.containsBeanDefinition(CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)) {
    RootBeanDefinition def = new RootBeanDefinition(ConfigurationClassPostProcessor.class);
    def.setSource(source);
    beanDefs.add(registerPostProcessor(registry, def, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME));
}

// 处理@Autowirded @Value
if (!registry.containsBeanDefinition(AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)) {
    RootBeanDefinition def = new RootBeanDefinition(AutowiredAnnotationBeanPostProcessor.class);
    def.setSource(source);
    beanDefs.add(registerPostProcessor(registry, def, AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME));
}
// 处理@Resource @PostConstruct 等
// Check for JSR-250 support, and if present add the CommonAnnotationBeanPostProcessor.
if (jsr250Present && !registry.containsBeanDefinition(COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)) {
    RootBeanDefinition def = new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class);
    def.setSource(source);
    beanDefs.add(registerPostProcessor(registry, def, COMMON_ANNOTATION_PROCESSOR_BEAN_NAME));
}
```
<a name="WxxHT"></a>
## ComponentScan实现原理
与xml方式类似，都是委托ClassPathBeanDefinitionScanner作Bean扫描。<br />
