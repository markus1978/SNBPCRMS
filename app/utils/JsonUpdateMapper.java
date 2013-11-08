package utils;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonUpdateMapper {
	
	private static Object javaValue(JsonNode json, Class<?> javaType) {
		if (javaType.isEnum()) {
			try {
				return javaType.getMethod("valueOf", String.class).invoke(null, json.asText());
			} catch (Exception e) {
				throw new RuntimeException("Bad enum value: " + e.getMessage());
			}
		} else if (javaType == String.class) {
			return json.asText();
		} else if (javaType == Long.TYPE || javaType == Long.class) {
			return json.asLong();
		} else if (javaType == Boolean.TYPE || javaType == Boolean.class) {
			return json.asBoolean();
		} else if (javaType == Date.class) {
			return new Date(json.asLong());
		} else {
			throw new RuntimeException("Unsupported Json type.");
		}
	
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void update(JsonNode json, Object pojo) {
		Iterator<Entry<String, JsonNode>> fields = json.fields();
    	while (fields.hasNext()) {
    		try {
	    		Entry<String, JsonNode> next = fields.next();
				String key = next.getKey();
				if (key.trim().equals("")) {
					continue;
				} else if (key.contains("[")) {
					key = key.substring(0, key.indexOf("["));
				}
				if (next.getValue().asText().equals("")) {
					continue;
				}
				PropertyDescriptor propertyDescriptor = new PropertyDescriptor(key, pojo.getClass());
				
				Class<?> propertyType = propertyDescriptor.getPropertyType();			
				if (propertyType.isAssignableFrom(List.class)) {
					List list = (List)propertyDescriptor.getReadMethod().invoke(pojo);
					Object javaValue = javaValue(next.getValue(), String.class);
					if (!list.contains(javaValue)) { // TODO this should also be configuration via anotations
						list.add(javaValue); // TODO this only works for string lists. one could enhance this by passing the generic type via an annotation on the property
					}
				} else {
					propertyDescriptor.getWriteMethod().invoke(pojo, javaValue(next.getValue(), propertyType));
				}			
			} catch (NullPointerException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | IntrospectionException e) {
				throw new RuntimeException("Unexpected getter/setter problem: " + e.getMessage());	
			}
			
		}
	}
}
