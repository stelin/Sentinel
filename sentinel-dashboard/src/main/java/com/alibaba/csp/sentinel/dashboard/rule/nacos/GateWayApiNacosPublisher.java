package com.alibaba.csp.sentinel.dashboard.rule.nacos;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.ApiDefinitionEntity;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author stelin <stelin@swoft.org>
 * @date 2021-07-05 16:16:14
 */
@Component("gateWayApiNacosPublisher")
public class GateWayApiNacosPublisher implements DynamicRulePublisher<List<ApiDefinitionEntity>> {
    private final ConfigService configService;
    private final Converter<List<ApiDefinitionEntity>, String> converter;

    public GateWayApiNacosPublisher(ConfigService configService, Converter<List<ApiDefinitionEntity>, String> converter) {
        this.configService = configService;
        this.converter = converter;
    }

    @Override
    public void publish(String app, List<ApiDefinitionEntity> rules) throws Exception {
        AssertUtil.notEmpty(app, "app name cannot be empty");
        if (rules == null) {
            return;
        }
        configService.publishConfig(app+ NacosConfigUtil.GATEWAY_API_DATA_ID_POSTFIX,
                NacosConfigUtil.GROUP_ID,converter.convert(rules));
    }
}
