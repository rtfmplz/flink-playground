package org.apache.flink.playgrounds.ops.clickcount.functions;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEvent;

public class MyMetricCounter extends RichMapFunction<ClickEvent, ClickEvent> {
    private transient Counter counter;

    @Override
    public void open(Configuration config) {
        this.counter = getRuntimeContext()
                .getMetricGroup()
                .counter("MyMetricCounter");
    }

    @Override
    public ClickEvent map(ClickEvent value) throws Exception {
        this.counter.inc();
        return value;
    }
}

