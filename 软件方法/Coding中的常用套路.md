## 前言
一般而言，服务端开发几乎都是沿用表现层，业务层，集成层这样的三层架构模式. 基于Spring的企业级应用，在框架和容器中内置了很多J2EE模型，典型的SpringMVC几乎整合了表现层的所有模式，例如前端控制器，拦截器，视图等，而集成层更多的是基础设施代码，这类代码通常变化不多。而变化最大，修改最多的多是业务层代码。在这样的框架下，每开发或维护一个项目时，大多时间都在实现或修改业务层逻辑。但是，在这个过程中，发现在每个项目中，存在一些共通的东西. 以此为契机，我查阅了一些资料，并结合自己的一些项目参与体会，对一些常用的开发套路做个总结，

## 引入委托(Delegation)
引入委托，事实上是引入组合。为了更好的降低耦合，Joshua Bloch建议多使用组合，而非继承。委托是指一个对象的行为可以委托给另一个对象来完成，这是一种基本思路，与代理模式有点类似，都是让另一个对象来帮助完成一些行为职责。委托主要强调引入关联对象，把原来的行为交给关联对象来实现，简而言之，委托就像是拿另一种方法替代了原本的方法，交给现在这个替代后的方法使用。代理模式主要强调代理，内部由代理对象增强或减弱原对象的行为一般，一般存在特殊的代理场景中。
举个例子，假如我有件事，自己委托给别人去办，一般是直接办事，不改变我本身的意图和办事的步骤。而代理有点像自作主张，可能人为的加一些他的意图在里面，改变事情本身的步骤。

## 引入业务对象(BO)
实际的复杂的业务中，需要抽离各种模型，包括业务领域模型（业务抽象模型，包含业务领域实体,实体关系以及业务关系), 对象模型(业务领域中的一种具体的对象实现模型，描述了业务领域中所需类和关系)，还有数据模型(通常指的是数据实现模式，通常是ER模型)。
J2EE核心模式中提到，业务对象用于分离业务数据和业务逻辑。我窃以为业务对象本质上是一种值对象，核心职责是存储过程值，即过程数据。在笔者的实际使用中，有点小小的心得：
1. 转换不同层所需要的数据，解耦业务层逻辑。
用业务对象先转换TO(远程调用入参)，然后加工服务所需的业务数据，然后在转换为DO或PO持久化数据，最后再持久化DO或PO. 分离不同层所需的数据，自然可分离逻辑，可以实现很好的解耦。因为后期带来的变化或扩展往往发生在业务层(业务是变化的)。
2. 业务对象的职责需单一，可添加适当的自身行为，封装一些加工数据的逻辑
业务对象的属性或字段需具有相关性，而且不能依赖服务对象(Spring环境中的Bean)，保持依赖单一，一般是依赖其他业务对象，或领域对象。

## 引入传输对象
在SOA类应用中，把领域对象抽象出合理的数据模型，然后封装为数据传输对象，便于作远程调用的传输数据。在命名传输对象时，之前尝试过后缀为DTO或Resp,最后决定后缀叫TO更简洁清晰。

## 引入对象组装器
每层都使用了不同的类型的对象，如TO，BO, DO(PO), 自然需要创建或组装对象，对象组装器可以是基于创建型的Factory，类似于《重构与模式》中提到的Creation Method，常常用静态方法来创建对象，转换对象，在这一概念里，可以做数据加工，或是格式转换，或是同一DO字段映射到不同的业务字段上等。
一般较为常用的是直接用不同对象的getter或setter方法来做值的转换，然后再加工。这类代码较为繁琐，如果是用IDEA，可以用一些插件来自动生成转换代码，可节省一些功夫。之前随便搜索到一款插件叫`GenerateO2O`, 这款插件有便利的地方，也有不便之处，但是已经能满足我的日常使用了。可根据个人所需，先找轮子，最后再自己写个插件都可。

## 引入接口Service Delegate
开发Rpc服务时，根据实际业务场景中，通常的做法的是写一个接口，然后实现这个接口，在这个接口中作相应的业务逻辑，通常是调用Manager层。例如：
```java
interface CustomerQueryHSFService {
  Customer queryById(QueryParam param);
}
```
实现代码：
```java
class CustomerQueryHSFServiceImpl implements  CustomerQueryHSFService{
  Customer queryById(QueryParam param) {
    // 参数校验, BO转换
    // Manager对象事物查询
    ...

  }
}

```
大多这样做已经很好了，但是有些业务场景负责多变时，此时可引入Delegate后，在Delete中业务处理，然后在透出的实际接口中做一些其他操作，如果开关降级，关注特殊异常(切面不能满足的特殊处理逻辑)。看起来像这样：
```java
class CustomerQueryHSFServiceDelegate implements  CustomerQueryHSFService{
  由Delete来完成实际的业务处理
  Customer queryById(QueryParam param) {
    // 参数校验, BO转换
    // Manager对象事物查询
    ...

  }
}
class CustomerQueryHSFServiceImpl implements  CustomerQueryHSFService{
  @Inject
  private CustomerQueryHSFServiceDelegate delete;
  Customer queryById(QueryParam param) {
     前置个性化处理：不属于业务本身，可能属于系统相关的
    delete.queryById(param);
    后置个性化处理：不属于业务本身

    异常特殊处理
    降级特殊处理

  }
}

```

窃以为，这样做可以有效的分离业务逻辑与系统特殊逻辑，提升扩展性，有利于编写可维护的代码。

## 引入方法外观(使用同一逻辑层面的小方法)
我比较喜欢关注和学习如何才能写出灵活性较好同时可读性较好的代码，实践中的个人心得：把逻辑复杂的方法分解成命名良好的小方法，各个方法的细节需要在同一逻辑层面,同一层逻辑不能使用底层或高层的逻辑语义。举个例子：需求是实现早晨上班这件事, 沿用这个思路的话，步骤则是：
```
1. 起床
2. 穿著
3. 洗漱
4. 出门

```
在这个层次中，每个行为都是同一层次，感觉像是实现某个功能所需要的步骤，而每个步骤需要一些子步骤或叫细节步骤。有些功能不需要子步骤，在这个例子中则是：
```
1.起床
  1.1 磨5分钟再起
  1.2 坐在床上发5分钟呆
  1.3 下床
2.穿著(举个例子，您随意)
  2.1 穿一件T恤
  2.2 穿一件外套
  2.3 穿休闲裤
  2.4 穿NB999鞋
3.洗漱
  3.1 洗脸
  3.2 刷牙(欧乐宝电动牙刷不错)
4.出门
```
如果用代码来描述这段逻辑，则可抽象为：
```
 me.getUp();
 me.dressUp();
 me.washUp();
 me.goOut();

 void getUp() {
   waitAndLast5Minutes();
   dazeFor5Minutes();
   getOff();
 }

 void dressUp(){
   wearTShirt();
   wearCoat();
   warePants();
   putOnShowes()
 }

 void washUp(){
   washFace();
   brushTeeth
 }
```
这样只关注同一逻辑层的行为和数据，如果下层需要数据，则通过参数化方式传递，这样的思路融入了外观的思路，整体变得可读，同时也便于修改，每个小方法代码行尽量不超过10行，尽量表达清晰的意图，使用清楚的命名变量，合理的方法名称.（对自己的期望)

## 引入Command尝试替换if-else
有的复杂且多层的if-else代码块，可尝试使用Command来重构，增加扩展性，是的更容易增加新代码。举个例子：
```
if (actionName.equal("Open")) {
  do something
} else if (actionName.equal("Hide")) {
  do something
} else if (actionName.equal("Close")) {
  do something
} else if (actionName.equal("Exit")) {
  do something
}

```
太多if-else后，代码就显得很混乱，引入Command模式后,基于上述假设的示例代码：
```java
abstract class ActionHandler  {
  void execute(Parameter param) {
    是否根据场景，决定是否必要引入前置后置处理
    doExecute(param);
  }
  abstract void doExecute(Parameter param);
  根据实际情况引入模板方法
  void templateMethod() {

  }
}
class OpenHandler extends ActionHandler {
  void doExecute(Parameter param){
    ...
  }
}
class CloseHandler extends ActionHandler {
  void doExecute(Parameter param){
    ...
  }
}
... 其余省略
```
开始重构if-else,把所有的ActionName放入Map中，然后在根据运行时的上下文actionName来查找对应的Handler.
```java
map.put("Open", new OpenHandler())
map.put("Close", new CloseHandler())
map.put("Hide", new HideHandler())
map.put("Exit", new ExitHandler())

ActionHandler handler = lookup(handleName)
handler.execute(parameter);

```

## 根据场景,引入Strategy和State模式
合理就行，不能滥用

## 参考资料
1. J2EE核心模式
2. 重构与模式
