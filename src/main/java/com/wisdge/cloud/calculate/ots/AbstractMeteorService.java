package com.wisdge.cloud.calculate.ots;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;

public abstract class AbstractMeteorService<P, R> {
	
	private Class<P> paramType;
	@Autowired
	private ObjectMapper objectMapper;

	public abstract String serviceCode();

	protected abstract R onServiceCall(P param);

	@SuppressWarnings("unchecked")
	public AbstractMeteorService() {
		super();
		ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
		paramType = (Class<P>) parameterizedType.getActualTypeArguments()[0];
	}

	public String process(String paramJson)
			throws JsonParseException, JsonMappingException, IOException {
		P param = readParam(paramJson);
		R result = onServiceCall(param);
		return writeResult(result);
	}

	private P readParam(String json) throws IOException, JsonParseException, JsonMappingException {
		if (StringUtils.isBlank(json)) {
			return null;
		} else {
			return objectMapper.readValue(json, paramType);
		}
	}

	private String writeResult(R result) throws JsonProcessingException {
		if (result == null) {
			return "";
		} else {
			return objectMapper.writeValueAsString(result);
		}
	}
}
