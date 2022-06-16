package com.wisdge.cloud.calculate.ots;

import com.alibaba.fastjson.JSON;
import com.wisdge.cloud.configurer.MeteorConfig;
import meteor.api.exception.inner.MeteorStartException;
import meteor.api.exception.service.ConnectionNotReadyException;
import meteor.api.exception.service.ResponseFailureException;
import meteor.api.exception.service.UnexpectedException;
import meteor.client4j.MeteorClient;
import meteor.client4j.handler.ServiceInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("meteorClientProxy")
public class MeteorClientProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeteorClientProxy.class);

    @Autowired
    private MeteorConfig meteorConfig;

    private List<AbstractMeteorService<?, ?>> services = new ArrayList<AbstractMeteorService<?, ?>>();
    private MeteorClient meteorClient;
    private Map<String, AbstractMeteorService<?, ?>> serviceMap;

    @PostConstruct
    public void start() throws MeteorStartException {
        meteorClient = new MeteorClient(meteorConfig.getClientCode(),
                meteorConfig.getClientToken(),
                meteorConfig.getClientHost(),
                meteorConfig.getClientPort(),
                meteorConfig.getServerHost(),
                meteorConfig.getServerPort(),
                this::onServiceCall);
//        meteorClient.start();
//        serviceMap = services.stream()
//                .filter(service -> StringUtils.isNotBlank(service.serviceCode()))
//                .collect(Collectors.toMap(service -> service.serviceCode(), service -> service));
    }

    @PreDestroy
    public void shutdown() {
        meteorClient.shutdown();
    }

    public String callService(ServiceInfo serviceInfo, String param)
            throws ResponseFailureException, ConnectionNotReadyException, UnexpectedException {
        return meteorClient.callService(serviceInfo, param == null ? "" : param);
    }

    private String onServiceCall(ServiceInfo serviceInfo, String param) throws Exception {
        LOGGER.debug("{}", "========================================");
        LOGGER.debug("服务总线调用:{}-{} 参数: {}", serviceInfo.getTargetCode(), serviceInfo.getServiceCode(), param);
        try {
            String serviceCode = serviceInfo.getServiceCode();
            AbstractMeteorService<?, ?> service = serviceMap.get(serviceCode);
            String result = service.process(param);
            LOGGER.debug("服务总线调用成功:{}", serviceInfo.getServiceCode());
            LOGGER.debug("{}", "========================================");
            return result == null ? "" : result;
        } catch (Exception e) {
            LOGGER.error("调用失败!" + e.getMessage() + ". [system:" + serviceInfo.getTargetCode() + ", service:" + serviceInfo.getServiceCode() + ", param:" + param + "]", e);
            LOGGER.debug("{}", "========================================");
            Map<String, Object> ret = new HashMap<>();
            ret.put("code", "9999");
            ret.put("msg", "接口调用异常,错误消息:" + e.getMessage());
            return JSON.toJSONString(ret);
        }
    }
}
