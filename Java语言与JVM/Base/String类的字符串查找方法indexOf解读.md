# Java String类的字符串查找方法indexOf
## 前言
也是偶然间想看看这方面的内容，顺便读读源码。

## 从源码开始

```java

索引待查找字符串在当前字符串中第一次出现的位置，返回该位置的字符下标
public int indexOf(String str) {
        return indexOf(str, 0);
}

public int indexOf(String str, int fromIndex) {
        return indexOf(value, 0, value.length,
                str.value, 0, str.value.length, fromIndex);
}
 /**

   * @param   source       源字符串
   * @param   sourceOffset 源字符串开始查找位置，一般从0开始
   * @param   sourceCount  源字符串的长度
   * @param   target       待查找的目标字符串
   * @param   targetOffset 目标字符串的开始查找位置，一般从0开始，意为查找整个目标字符串
   * @param   targetCount  目标字符串的长度
   * @param   fromIndex    从源字符串第几个字符开始查找
   */   
static int indexOf(char[] source, int sourceOffset, int sourceCount,
            char[] target, int targetOffset, int targetCount,
            int fromIndex) {
        如果是逆向查找，除非目标字符串是空串(""), 否则查不到     
        if (fromIndex >= sourceCount) {
            return (targetCount == 0 ? sourceCount : -1);
        }
        自我校验，始终做正向查找
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        空串查找始终都能命中
        if (targetCount == 0) {
            return fromIndex;
        }

        取目标字符串的第一个字符
        char first = target[targetOffset];
        最大索引位置，最坏的情况目标字符串在源源字符串的尾部，所以需要最大的索引位置
        例如：source: I am source text
             target:             text
        上述查找需要从I逐渐匹配到source后
        int max = sourceOffset + (sourceCount - targetCount);

        for (int i = sourceOffset + fromIndex; i <= max; i++) {

            先从源中查找第一个字符是否匹配，然后不断递增索引，如果到最大位置时，第一个字符都不匹配，则查找失败，返回-1
            if (source[i] != first) {
                while (++i <= max && source[i] != first);
            }

            只有当i<=max，才表示已找到第一个字符，找到后开始匹配剩余字符
            if (i <= max) {
                int j = i + 1; 取源字符中的下一个位置
                int end = j + targetCount - 1; 目标字符的结束位置，实际上是目标字符后一个字符

                思路为：用目标字符逐个去匹配源字符，然后对比下标
                for (int k = targetOffset + 1; j < end && source[j]
                        == target[k]; j++, k++);

                if (j == end) {
                    /* Found whole string. */
                    return i - sourceOffset;
                }
            }

        }
        return -1;
    }

```

## 用一个复杂的例子验证
```
source = {"appName":"60032158","appVersion":"android_1.0.1","deviceName":"Huawei(PE-TL10)"}
target = "deviceName":" (双引号也是查找内容)
```
关键逻辑为：
1. 目标字符的第一个字符为：`"`, 从source中开始查找`"`, 第一个`{`,不匹配，i自增为1, 此处i < max
2. 从source中appName开始，逐个匹配`d e v i c e ...`, 在前段部分, 由于都不匹配， 所以j不会增加
3. 外层循环，递增i
4. 知道i递增到`"deviceName..."`后，从该处的位置开始，逐个匹配target，此时 j自增，最后递增到end处
5. 验证j end, 返回实际下标位置


## 总结
字符串查找的核心思路：
1. 先找第一个字符，然后逐个在源字符串中匹配剩余字符
2. 关注下标位置的递增情况
