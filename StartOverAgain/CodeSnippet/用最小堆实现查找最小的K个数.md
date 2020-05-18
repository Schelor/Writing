**最小堆**
最小堆为一颗排序的完全二叉树,根节点为最小值，每个节点都小于其作子节点或右子节点。

```java

final int[] arr = {1, 3, 5, 7, 2, 4, 6, 8};
System.out.println(Arrays.toString(smallestKWithMinHead(arr, 4)));

public static int[] smallestKWithMinHead(int[] arr, int k) {

    // 默认为最小堆
    PriorityQueue<Integer> queue = new PriorityQueue<>(k + 1);
    for (int i = 0; i < arr.length; i++) {
        queue.offer(arr[i]);
    }
    int[] data = new int[k];
    for (int i = 0; i < k; i++) {
        data[i] = queue.poll();
    }
    return data;
}


```
