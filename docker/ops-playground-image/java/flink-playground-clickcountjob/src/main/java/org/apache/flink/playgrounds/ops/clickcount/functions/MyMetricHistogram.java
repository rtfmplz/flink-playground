package org.apache.flink.playgrounds.ops.clickcount.functions;

import com.codahale.metrics.SlidingWindowReservoir;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.dropwizard.metrics.DropwizardHistogramWrapper;
import org.apache.flink.metrics.Histogram;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEvent;

public class MyMetricHistogram extends RichMapFunction<ClickEvent, ClickEvent> {
    private transient Histogram histogram;

    @Override
    public void open(Configuration config) {
        com.codahale.metrics.Histogram dropwizardHistogram =
                new com.codahale.metrics.Histogram(new SlidingWindowReservoir(500));

        this.histogram = getRuntimeContext()
                .getMetricGroup()
                .histogram("MyMetricHistogram", new DropwizardHistogramWrapper(dropwizardHistogram));
    }

    @Override
    public ClickEvent map(ClickEvent value) throws Exception {
        this.histogram.update(value.getId());
        return value;
    }
}