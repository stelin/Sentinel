package com.alibaba.csp.sentinel.dashboard.repository.metric;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.entity.InfluxDbMetricEntity;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.InfluxDBResultMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author stelin <stelin@swoft.org>
 * @date 2021-07-01 14:24:16
 */
@Component("inInfluxDbMetricsRepository")
public class InInfluxDbMetricsRepository implements MetricsRepository<MetricEntity> {
    private final InfluxDB influxDb;

    @Autowired()
    public InInfluxDbMetricsRepository(InfluxDB influxDb) {
        this.influxDb = influxDb;
    }

    @Override
    public synchronized void save(MetricEntity metric) {
        Point point = Point
                .measurement("sentinelInfo")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                //tag 数据走索引
                .tag("app", metric.getApp())
                //tag 数据走索引
                .tag("resource", metric.getResource())
                .addField("gmtCreate", metric.getGmtCreate().getTime())
                .addField("gmtModified", metric.getGmtModified().getTime())
                .addField("timestamp", metric.getTimestamp().getTime())
                .addField("passQps", metric.getPassQps())
                .addField("successQps", metric.getSuccessQps())
                .addField("blockQps", metric.getBlockQps())
                .addField("exceptionQps", metric.getExceptionQps())
                .addField("rt", metric.getRt())
                .addField("count", metric.getCount())
                .addField("resourceCode", metric.getResourceCode())
                .build();
        influxDb.write(point);
    }

    @Override
    public synchronized void saveAll(Iterable<MetricEntity> metrics) {
        if (metrics == null) {
            return;
        }
        BatchPoints batchPoints = BatchPoints.builder()
                .tag("async", "true")
                .consistency(InfluxDB.ConsistencyLevel.ALL)
                .build();
        metrics.forEach(metric -> {
            Point point = Point
                    .measurement("sentinelInfo")
                    /**
                     * 这里使用毫秒
                     * 但是从客户端过来的请求可能是多个(部分静态资源没有被过滤掉)
                     * 导致数据唯一标识重复，后面资源的把前面的覆盖点
                     * 如果还有覆盖数据就使用微妙，保证 time 和 tag 唯一就可以
                     * 但是有个问题，使用微妙会导致查询时间有问题，建议这里过滤掉静态请求
                     * 或者客户端屏蔽
                     */
                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .tag("app", metric.getApp())//tag 数据走索引
                    .tag("resource", metric.getResource())//tag 数据走索引
                    .addField("gmtCreate", metric.getGmtCreate().getTime())
                    .addField("gmtModified", metric.getGmtModified().getTime())
                    .addField("timestamp", metric.getTimestamp().getTime())
                    .addField("passQps", metric.getPassQps())
                    .addField("successQps", metric.getSuccessQps())
                    .addField("blockQps", metric.getBlockQps())
                    .addField("exceptionQps", metric.getExceptionQps())
                    .addField("rt", metric.getRt())
                    .addField("count", metric.getCount())
                    .addField("resourceCode", metric.getResourceCode())
                    .build();
            batchPoints.point(point);
        });
        influxDb.write(batchPoints);
    }

    @Override
    public synchronized List<MetricEntity> queryByAppAndResourceBetween(String app, String resource, long startTime, long endTime) {
        List<MetricEntity> results = new ArrayList<>();
        if (StringUtil.isBlank(app)) {
            return results;
        }
        String command = "SELECT * FROM sentinelInfo WHERE app='" + app + "' AND resource = '" + resource + "' AND gmtCreate>" + startTime + " AND gmtCreate<" + endTime;
        Query query = new Query(command);
        QueryResult queryResult = influxDb.query(query);
        // thread-safe - can be reused
        InfluxDBResultMapper resultMapper = new InfluxDBResultMapper();
        List<InfluxDbMetricEntity> influxResults = resultMapper.toPOJO(queryResult, InfluxDbMetricEntity.class);
        try {
            influxResults.forEach(entity -> {
                MetricEntity metric = new MetricEntity();
                BeanUtils.copyProperties(entity, metric);
                metric.setTimestamp(new Date(entity.getTimestamp()));
                metric.setGmtCreate(new Date(entity.getGmtCreate()));
                metric.setGmtModified(new Date(entity.getGmtModified()));
                results.add(metric);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    @Override
    public List<String> listResourcesOfApp(String app) {
        List<String> results = new ArrayList<>();
        if (StringUtil.isBlank(app)) {
            return results;
        }
        //最近一分钟的指标(实时数据)
        final long minTimeMs = System.currentTimeMillis() - 1000 * 60;
        String command = "SELECT * FROM sentinelInfo WHERE app='" + app + "' AND gmtCreate>" + minTimeMs;
        Query query = new Query(command);
        QueryResult queryResult = influxDb.query(query);
        // thread-safe - can be reused
        InfluxDBResultMapper resultMapper = new InfluxDBResultMapper();
        List<InfluxDbMetricEntity> influxResults = resultMapper.toPOJO(queryResult, InfluxDbMetricEntity.class);
        try {
            if (CollectionUtils.isEmpty(influxResults)) {
                return results;
            }
            Map<String, InfluxDbMetricEntity> resourceCount = new HashMap<>(32);
            for (InfluxDbMetricEntity metricEntity : influxResults) {
                String resource = metricEntity.getResource();
                if (resourceCount.containsKey(resource)) {
                    InfluxDbMetricEntity oldEntity = resourceCount.get(resource);
                    oldEntity.setPassQps(metricEntity.getPassQps());
                    oldEntity.setRt(metricEntity.getRt());
                    oldEntity.setSuccessQps(metricEntity.getSuccessQps());
                    oldEntity.setBlockQps(metricEntity.getBlockQps());
                    oldEntity.setExceptionQps(metricEntity.getExceptionQps());
                    oldEntity.setCount(1);
                } else {
                    resourceCount.put(resource, InfluxDbMetricEntity.copyOf(metricEntity));
                }
            }
            //排序
            results = resourceCount.entrySet()
                    .stream()
                    .sorted((o1, o2) -> {
                        InfluxDbMetricEntity e1 = o1.getValue();
                        InfluxDbMetricEntity e2 = o2.getValue();
                        int t = e2.getBlockQps().compareTo(e1.getBlockQps());
                        if (t != 0) {
                            return t;
                        }
                        return e2.getPassQps().compareTo(e1.getPassQps());
                    })
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }
}
