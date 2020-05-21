> 递归判断字段类型并遍历处理为字符串类型

```Java

递归遍历JSONObject的字段
 public void replace(Object object) {

        if (object instanceof JSONObject) {
            replaceJsonObject((JSONObject)object);

        } else if (object instanceof JSONArray) {
            replaceJsonArray((JSONArray)object);
        }
    }

    public void replaceJsonObject(JSONObject jsonObj) {

        for (Entry<String, Object> entry : jsonObj.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();

            if (value instanceof JSONObject) {

                replaceJsonObject((JSONObject)value);

            } else if (value instanceof JSONArray) {

                replaceJsonArray((JSONArray)value);
            } else {

                jsonObj.replace(key, String.valueOf(value));
            }
        }

    }

    public void replaceJsonArray(JSONArray jsonArray) {
        for (int i = 0; i < jsonArray.size(); i++) {
            Object o = jsonArray.get(i);
            replace(o);
        }

    }
```
