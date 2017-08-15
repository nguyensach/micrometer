/**
 * Copyright 2017 Pivotal Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.core.instrument.composite;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.noop.NoopDistributionSummary;
import io.micrometer.core.instrument.stats.hist.Histogram;
import io.micrometer.core.instrument.stats.quantile.Quantiles;
import io.micrometer.core.instrument.util.MeterId;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class CompositeDistributionSummary implements DistributionSummary, CompositeMeter {
    private final MeterId id;
    private final Quantiles quantiles;
    private final Histogram histogram;

    private final Map<MeterRegistry, DistributionSummary> distributionSummaries =
        Collections.synchronizedMap(new LinkedHashMap<>());

    CompositeDistributionSummary(MeterId id, Quantiles quantiles, Histogram histogram) {
        this.id = id;
        this.quantiles = quantiles;
        this.histogram = histogram;
    }

    @Override
    public void record(double amount) {
        synchronized (distributionSummaries) {
            distributionSummaries.values().forEach(ds -> ds.record(amount));
        }
    }

    @Override
    public long count() {
        synchronized (distributionSummaries) {
            return distributionSummaries.values().stream().findFirst().orElse(NoopDistributionSummary.INSTANCE).count();
        }
    }

    @Override
    public double totalAmount() {
        synchronized (distributionSummaries) {
            return distributionSummaries.values().stream().findFirst().orElse(NoopDistributionSummary.INSTANCE).totalAmount();
        }
    }

    @Override
    public void add(MeterRegistry registry) {
        synchronized (distributionSummaries) {
            distributionSummaries.put(registry,
                registry.summaryBuilder(id.getName()).tags(id.getTags()).quantiles(quantiles).histogram(histogram).create());
        }
    }

    @Override
    public void remove(MeterRegistry registry) {
        synchronized (distributionSummaries) {
            distributionSummaries.remove(registry);
        }
    }

    @Override
    public String getName() {
        return id.getName();
    }

    @Override
    public Iterable<Tag> getTags() {
        return id.getTags();
    }

    @Override
    public List<Measurement> measure() {
        synchronized (distributionSummaries) {
            return distributionSummaries.values().stream().flatMap(c -> c.measure().stream()).collect(toList());
        }
    }
}
