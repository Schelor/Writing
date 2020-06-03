## Spring AOP在项目中的套路

### 前言

正如大家知道的，AOP意为面向切面编程，是对OOP的一种补充，是一种水平方向的编程模式。本文是对`Spring AOP`的一些使用总结和学习心得，如有错误的地方，欢迎指出。 By Xie Le

### 看看别人是怎么写的切面

偶然间看到`邱老师`在订单中心项目中的[b2b-common](git@git.jd.com:cdrd-nbd/b2b-common.git)模块中的代码：

```
/**
 * @author qiulong
 */
public abstract class AbstractAspect {

    public void before(JoinPoint jp) {};
    public void afterReturning(JoinPoint jp, Object retVal) {};
    public void afterThrowing(JoinPoint jp, Throwable e) throws Throwable {};
    public void after(JoinPoint jp) {};
    public Object around(ProceedingJoinPoint jp) throws Throwable {return null;};
}
```
还有一些实现类，例如`RPCConsumerAspect`,主要代码如下：
```
@Aspect
public class RPCConsumerAspect extends AspectForLogger {

    private static final String POINTCUT = "execution(public * com.jd.b2b.mid.rpc.consumer..*.*(..))";

    @Override
    @Before(POINTCUT)
    public void before(JoinPoint jp) {
        if (isEnabled()) {
            log("{}#{},入参:{}", getClassName(jp), getMethodName(jp), Json.toJson(jp.getArgs()));
        }
    }

    @Override
    @AfterReturning(pointcut = POINTCUT, returning = "retVal")
    public void afterReturning(JoinPoint jp, Object retVal) {
        if (isEnabled()) {
            log("{}#{},出参:{}", getClassName(jp), getMethodName(jp), Json.toJson(retVal));
        }
    }
}
```

代码逻辑很容易读懂，凡是涉及到RPC调用的接口，我们都希望记录入参和调用结果。如果在每个方法都作单独的日志，那将是不小的工作量。

再看看UMP使用注解的方式来添加监控的示例, 调用方式为：
```
@JProfiler(jAppName = APP_NAME, jKey = SERVICESOAPADAPTER_ISCANCELED,
mState = { JProEnum.TP, JProEnum.FunctionError })
```
其解析的切面为`com.jd.ump.annotation.JAnnotation`, 打开其源码就可以知道, 这个切面主要是检测方法上是否有`@JProfiler`注解，然后校验参数。最后则是添加UMP的监控，其中很熟悉的模式为：
```
    CallerInfo callerInfo ...
    try {
        callerInfo = Profiler.registerInfo(...)
    } catch(Excpetion e) {
        //如果配置了functionError
        Profiler.functionError(callerInfo);
    } finally {
        Profiler.registerInfoEnd(callerInfo);
    }

```
`@JProfiler`或许带来了一点便利，但是处处都是`@JProfiler`，还要指定APP_NAME, 编写很多Key, 会不会很累？

### 项目中重复的碎片代码

以商品池为例吧。Service层调用，RPC层调用有很多UMP监控的代码，也有很多记录缓存的代码、捕获异常，抛出异常、Service层散落着事物处理，Manager层也有事物处理。这些有很多都是重复的碎片，也许可以试试使用切面，试试AOP。

其他的项目或系统中或许也有类似的问题吧。

### 减少碎片

- [ ] 假如把重要方法或RPC调用的方法中记录入参和出参的代码用切面来完成(学习邱老师)，会不会减少一部分工作量。代码示例见文章开始部分。
- [ ] 假如把记录UMP监控的代码也借用切面来完成。代码就会类似如下：
    ```
     /**
     * Service层切面
     * 用于添加UMP监控，事务处理等
     * @author Xie le
     * @date 2016/7/9
     */
    @Aspect
    public class ServiceAspectBean implements InitializingBean {

        private String appName;
        private String systemKey;
        private String jvmKey;
        protected TransactionTemplate transactionTemplate;

        static final Logger logger = LoggerFactory.getLogger(ServiceAspectBean.class);

        @Pointcut("execution(* com.jd.ka.nest.service..*.*(..))")
        public void enableServicePoint() {}

        @Around("enableServicePoint()")
        public Object executeInTransation(ProceedingJoinPoint pjp) throws Throwable {
              final Method method = this.getMethod(pjp);
              return doProceed(pjp, methodName);
        }

        private Object doProceed(ProceedingJoinPoint pjp, String methodName) throws Throwable {
            String className = pjp.getTarget().getClass().getName();
            Object result = null;
            CallerInfo callerInfo = null;
            String key = appName + "." + className + "." + methodName;
            try {
                callerInfo = Profiler.registerInfo(key, appName, false, true);
                result = pjp.proceed();
            } finally {
                if (callerInfo != null) {
                    Profiler.registerInfoEnd(callerInfo);
                }
            }
            return result;
    }

    ```
- [ ] 一般还有很多其他可应用切面的场景，例如：
   > Authentication 权限  Caching 缓存 Context passing 内容传递 Error handling 错误处理 Lazy loading　懒加载 Debugging　　调试 logging, tracing, profiling and monitoring　记录跟踪　优化　校准 Performance optimization　性能优化 Persistence　　持久化 Resource pooling　资源池 Synchronization　同步 Transactions 事务

    `注：上述几个场景来自于网络`
- [ ] 来自Spring官方文档中的一个切面

    ```
    @Aspect
    public class SystemArchitecture {

      @Pointcut("within(com.xyz.someapp.web..*)")
      public void inWebLayer() {} //Web层横切点

      @Pointcut("within(com.xyz.someapp.service..*)")
      public void inServiceLayer() {} // 业务层横切点

      @Pointcut("within(com.xyz.someapp.dao..*)")
      public void inDataAccessLayer() {} //数据访问层横切点

      @Pointcut("execution(* com.xyz.someapp.service.*.*(..))")
      public void businessService() {} //业务逻辑横切执行点

      @Pointcut("execution(* com.xyz.someapp.dao.*.*(..))")
      public void dataAccessOperation() {}

   }

    ```
### 如果不知道怎么用

AOP的常用概念实体如下(注：这些内容在Spring官方文档中说明得更详细.[去看文档](http://docs.spring.io/spring/docs/3.2.17.RELEASE/spring-framework-reference/htmlsingle/#aop))

- [x] `Aspect(切面)`：一个模块化的关注点，并可用于横切多个类，例如日志记录，事物处理，异常捕获等。
- [x] `JoinPoint(连接点)`:执行程序的切入点，表示一次方法执行。
- [x] `Pointcut(横切点)`: 表示匹配连接点的前置条件，并关联到相应的Advice上。
- [x] `Advice(通知)`: 英文直意为通知，建议等，我理解为在调用目标方法时，需要做某些相关的操作，可以是前置操作，后置操作，这些操作需要告知目标对象。有的翻译为增强处理，也可行，重在理解即可。

举几个切面示例
- 前置处理切面`@Before`
```
@Aspect
public class BeforeAdvice {

    private Resource recource;

    @Pointcut("execution(boolean *.create(..))")
    public void before(){}

    @Before("before()")
    public void setUpResourceBefore(JoinPoint joinPoint) throws Throwable {
        if (recource != null && !recource.exists()) {

        }
    }

    //也可以这么写
    @Before("execution(boolean *.create(..))")
    public void setUpResourceBefore2(JoinPoint joinPoint) throws Throwable {

    }
}
```
- 后置处理`@Finally`
```
@Aspect
public class AfterFinallyAdvice {

    @After("execution(boolean *.clean(..))")
    public void cleanUp(JoinPoint jp) {
        //do Something
    }
}
```
- 环绕型处理`@Around`

```
/**
 * @author Xie le
 * @date 2016/8/9
 */
@Aspect
public class RoundAdvice {

    @Pointcut("execution(* *..*(..))")
    public void trace(){}

    @Around("trace()")
    public void failover(ProceedingJoinPoint pjp) throws Throwable {
        StopWatch watch = new StopWatch();
        try {
            watch.start("trace");
            pjp.proceed();
        } finally {
            watch.stop();
            watch.prettyPrint();
        }
    }

}
```

Spring 1.x 只能使用Spring中的AOP封装对象来编写切面代码。Spring 2.x及以上引入了@AspectJ, 这样我们可以用更简洁的代码来编写切面，其中还有基于Schema的XML模式来使用AOP。个人觉得，这种方式可读性维护都很繁琐，就不建议去研究了。因此只需要了解@AspectJ风格的AOP就行(也就是上述示例中的注解方式)。


### Spring AOP实现原理

AOP其实很早就出现了，AOP编程概念并不只有Java世界里才有。由于Java平台支持动态代理以及其他动态字节码增强技术，使得AOP在Java世界里最流行而且发展的很好。
Spring 自1.x版本就开始集成AOP, 也就是我们常说的Spring AOP。在`Spring AOP`的底层，其本质即是`动态代理`。Spring AOP使用JDK动态代理和CGLIB动态字节码增强来实现动态代理生成。通过策略模式来完成实际场景中的代理策略选择。按既有的约定，JDK代理是基于接口的，而CGLIB则无此约束。默认情况下，Spring AOP会选择JDK代理，如果为`ProxyConfig`的属性`proxyTargetClass`配置为`true`, 则会强制使用CGLIB代理。下面对Spring AOP 实现原理作详细说明。

#### 自己来写动态代理
无论是JDK的动态代理，还是CGLIB动态代理，都会有必需的编程步骤。自己来写和Spring框架集成，其最终的步骤和本质都大同小异，只要自己会写，那么就能理解框架中的原理，只不过需要剥丝抽茧，去掉抽象，剥去封装，才能接近本质。

**InvocationHandler**

> JDK的动态代理采用InvocationHandler来实现对目标对象的方法拦截。生成代理类实现了目标类的所有接口，并动态生成目标类的所有方法。在代理类的方法中，调用了InvocationHandler的invoke方法，来完成对目标方法的调用拦截.(可阅读源码以知晓更多)

定义自己的`InvocationHandler`

```
private static class MyInvokationHandler implements InvocationHandler {
    private Object target;
    MyInvokationHandler(Object target) {
        this.target = target;
    }
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("JDK生成的代码类： " + proxy.getClass().getName());
        System.out.println("调用方法： " + method.getName());
        System.out.println("调用参数： " + ArrayUtils.toString(args));
        Object invoked = method.invoke(target, args);
        System.out.println("后置处理...");
        return invoked;
    }

    public Object getProxy() {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                target.getClass().getInterfaces(), new MyInvokationHandler(target));
    }
}

```
客户端调用代码：
```
public static void main(String[] args) throws Throwable {
        ServiceBeanImpl impl = new ServiceBeanImpl(); //目标类
        MyInvokationHandler handler = new MyInvokationHandler(impl);
        ServiceBean bean = (ServiceBean)handler.getProxy(); //获取代理类
        System.out.println(bean.getClass().getName());
        String returnVal = bean.doService("JDK Proxy"); //在代理对象上调用目标方法
        System.out.println(returnVal);
}    
```

**CGLIB**

> CGLIB,英文全称为Code Generation Library，意为字节码生成库。通过为目标对象进行继承扩展，为其生成相应的子类，而子类可以通过覆写来扩展父类的行为，然后将横切逻辑放到子类中，最后系统使用扩展后的目标对象的子类。

Spring2.x及其以上的版本已经继承了CGLIB库，因此在Spring环境中可以自由使用，使用CGLIB来实现代理的一般步骤为：
- 实现`MethodInterceptor`接口，自定义拦截逻辑
- 通过`Enhancer`来生成CGLIB代理
- 调用目标方法

一个小示例代码如下：
```
public class CglibProxyCase {
    public static void main(String[] args) {
        useCglib();
    }
    public static void useCglib() {
        Enhancer en = new Enhancer();
        // 设置要代理的目标类
        en.setSuperclass(Requestable.class);
        // 设置要代理的拦截器
        en.setCallback(new MethodAdvisor());
        // 生成代理类的实例
        Requestable proxy = (Requestable) en.create();
        String response = proxy.request("Hello Cglib");
        System.out.println(response);
    }
}
public class MethodAdvisor implements MethodInterceptor {

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        System.out.println("Before Execution, check something ...");
        // 执行目标方法，并保存目标方法执行后的返回值
        Object returnVal = methodProxy.invokeSuper(o, objects);
        System.out.println("After Execution,  finalize something ...");
        return "Intercepted " + returnVal;
    }

}
```
代码很容易看懂，不是吗？

#### Spring AOP实现

`Spring Ioc` 统一把预初始化、创建Bean的职责都委托给了BeanFactory，默认是由`DefaultListableBeanFactory`来实现。而Bean工厂创建Bean的流程简述如下：

```
getBean -> doGetBean-> 从缓存中取已创建好的Bean -> 没有取到则创建
-> 依赖检查 -> 递归创建依赖Bean -> 创建Bean本身 -> 依赖注入
-> 初始化Bean ->  应用后处理器(BeanPostProcessor)

```
上述步骤只有大概的步骤，我忽略的很多细节。有了上述流程后，再来分析AOP的原理。

在Spring 2.x以上，我们使用AOP时，往往会配置一行代码：
```
<aop:aspectj-autoproxy/>
```
表示要启用AspectJ AOP,Spring启动时会注册一个`AnnotationAwareAspectJAutoProxyCreator`, 而这个类则由父类间接的实现了`BeanPostProsessor`。
因此Spring创建所有的Bean时，都会经由AnnotationAwareAspectJAutoProxyCreator作后置处理, 如果有必要，则会为Bean创建AOP代理。而是否有必要的条件是，获取到系统中所有切面并检查是否目标Bean满足切面的应有范围。我们看看`AnnotationAwareAspectJAutoProxyCreator`的`postProcessAfterInitialization`方法。
```
/*
 * 如果Bean满足被代理的条件，则需要为其创建代理对象，并设置相应的拦截器
 */
public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
	if (bean != null) {
		Object cacheKey = getCacheKey(bean.getClass(), beanName);
		if (!this.earlyProxyReferences.containsKey(cacheKey)) {
		    //如果有必要，则需要Wrap
			return wrapIfNecessary(bean, beanName, cacheKey);
		}
	}
	return bean;
}
```
而在`wrapIfNecessary`方法中，可以看到Spring 为Bean创建的代理的行为：
```
// Create proxy if we have advice. 这个注释很好理解吧
Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
 if (specificInterceptors != DO_NOT_PROXY) {
	this.advisedBeans.put(cacheKey, Boolean.TRUE);
	Object proxy = createProxy(bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
	this.proxyTypes.put(cacheKey, proxy.getClass());
	return proxy;
}
```
**再看看创建代理的过程**

createProxy方法最后会调用`DefaultAopProxyFactory`工厂的`createAopProxy`来创建代理.
```
public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
    //配置了使用代理属性：proxyTargetClass = true, 则使用创建CGLIB代理
    if (config.isOptimize() || config.isProxyTargetClass() ||                        hasNoUserSuppliedProxyInterfaces(config)) {
	Class targetClass = config.getTargetClass();
	if (targetClass.isInterface()) { //目标类有接口
		return new JdkDynamicAopProxy(config); //创建JDK代理
	}
	return CglibProxyFactory.createCglibProxy(config); //创建CGLIB代理
    }
    else { //默认使用JDK 代理
	return new JdkDynamicAopProxy(config);
    }
}
```
构建好代理策略后，Spring直接通过具体的代理策略getProxy即可。有兴趣的童鞋可以自行去看看这部分的代码。

**去掉层层包装**
直接看看Spring AOP为目标Bean创建代理的简化过程吧。
```
    public static void createAopProxy() {
        //基于目标对象构建代理工厂
        ProxyFactory factory = new ProxyFactory(new Foo());
        factory.setProxyTargetClass(false); //表示不强制使用CGLIB
        //暴露当前代理，此处不再屠版，有兴趣的童鞋可以去看看这个有什么用
        factory.setExposeProxy(true);
        //添加一个切面
        factory.addAdvice(new PerformanceMonitorInterceptor());
        //获取代理
        BusinessInterfacer bean = (BusinessInterfacer) factory.getProxy();
        System.out.println(bean.getClass());
        //在代理对象上调用目标方法
        String s = bean.doSomething();
        System.out.println("Return value: " + s);
        //再次调用其他方法
        bean.method1();
    }
```

### 最后
行文至此，已近黄昏。就这样吧。
