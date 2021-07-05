package com.alibaba.csp.sentinel.dashboard.rule.nacos;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.GatewayFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author stelin <stelin@swoft.org>
 * @date 2021-07-05 16:16:14
 */
@Component("gateWayFlowRulesNacosProvider")
public class GateWayFlowRulesNacosProvider implements DynamicRuleProvider<List<GatewayFlowRuleEntity>> {
    private final ConfigService configService;
    private final Converter<String, List<GatewayFlowRuleEntity>> converter;

    @Autowired
    public GateWayFlowRulesNacosProvider(ConfigService configService, Converter<String, List<GatewayFlowRuleEntity>> converter) {
        this.configService = configService;
        this.converter = converter;
    }

    @Override
    public List<GatewayFlowRuleEntity> getRules(String appName) throws Exception {
        String rules = configService.getConfig(appName + NacosConfigUtil.GATEWAY_FLOW_DATA_ID_POSTFIX,
                NacosConfigUtil.GROUP_ID, 3000);
        if (StringUtil.isEmpty(rules)) {
            return new ArrayList<>();
        }
        return converter.convert(rules);
    }
}
