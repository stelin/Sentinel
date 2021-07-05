package com.alibaba.csp.sentinel.dashboard.rule.nacos;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.GatewayFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author stelin <stelin@swoft.org>
 * @date 2021-07-05 16:16:14
 */
@Component("gateWayFlowRulesNacosPunlisher")
public class GateWayFlowRulesNacosPunlisher implements DynamicRulePublisher<List<GatewayFlowRuleEntity>> {
    private final ConfigService configService;
    private final Converter<List<GatewayFlowRuleEntity>, String> converter;

    @Autowired
    public GateWayFlowRulesNacosPunlisher(ConfigService configService, Converter<List<GatewayFlowRuleEntity>, String> converter) {
        this.configService = configService;
        this.converter = converter;
    }

    @Override
    public void publish(String app, List<GatewayFlowRuleEntity> rules) throws Exception {
        AssertUtil.notEmpty(app, "app name cannot be empty");
        if (rules == null) {
            return;
        }
        configService.publishConfig(app + NacosConfigUtil.GATEWAY_FLOW_DATA_ID_POSTFIX,
                NacosConfigUtil.GROUP_ID, converter.convert(rules));
    }
}
