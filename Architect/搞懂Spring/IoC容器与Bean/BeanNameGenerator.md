# BeanNameGenerator

<a name="U0Bn1"></a>
## 概要
Spring提供的BeanNameGenerator接口有3个实现：
> org.springframework.beans.factory.support.BeanNameGenerator
> 无beanName的注解类实现: org.springframework.context.annotation.AnnotationBeanNameGenerator
> 无beanName的XML配置实现: org.springframework.beans.factory.support.DefaultBeanNameGenerator



<a name="svQnC"></a>
## DefaultBeanNameGenerator
> BeanClass+#+0

```java
	public static String generateBeanName(
			BeanDefinition definition, BeanDefinitionRegistry registry, boolean isInnerBean)
			throws BeanDefinitionStoreException {

		String generatedBeanName = definition.getBeanClassName();
		if (generatedBeanName == null) {
			if (definition.getParentName() != null) {
				generatedBeanName = definition.getParentName() + "$child";
			}
			else if (definition.getFactoryBeanName() != null) {
				generatedBeanName = definition.getFactoryBeanName() + "$created";
			}
		}
		if (!StringUtils.hasText(generatedBeanName)) {
			throw new BeanDefinitionStoreException("Unnamed bean definition specifies neither " +
					"'class' nor 'parent' nor 'factory-bean' - can't generate bean name");
		}

		if (isInnerBean) {
			// Inner bean: generate identity hashcode suffix.
			return generatedBeanName + GENERATED_BEAN_NAME_SEPARATOR + ObjectUtils.getIdentityHexString(definition);
		}

		// Top-level bean: use plain class name with unique suffix if necessary.
		return uniqueBeanName(generatedBeanName, registry);
	}

	public static String uniqueBeanName(String beanName, BeanDefinitionRegistry registry) {
		String id = beanName;
		int counter = -1;

		// Increase counter until the id is unique.
		String prefix = beanName + GENERATED_BEAN_NAME_SEPARATOR; // 这个常量表示#
		while (counter == -1 || registry.containsBeanDefinition(id)) {
			counter++;
			id = prefix + counter;
		}
		return id;
	}
```
<a name="fQCQs"></a>
### 举个栗子
> com.example.start.springdemo.spring.HelloWorldBeanPostProcessor#0

<a name="Jlheq"></a>
## AnnotationBeanNameGenerator
> 当前类的短名称首字母小写

```java
	protected String buildDefaultBeanName(BeanDefinition definition) {
		String beanClassName = definition.getBeanClassName();
		Assert.state(beanClassName != null, "No bean class name set");
		String shortClassName = ClassUtils.getShortName(beanClassName);
		return Introspector.decapitalize(shortClassName);
	}
```
