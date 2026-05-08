package org.acme;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import java.time.LocalDateTime;

public class EnergyAnalytics {

    public Long id;
    public LocalDateTime timestamp;
    public String metric_type;
    public String reference_id;
    public Float metric_value;
    public String unit;
    public String status;

    public EnergyAnalytics() {}

    public EnergyAnalytics(Long id, LocalDateTime timestamp, String metric_type, String reference_id, Float metric_value, String unit, String status) {
        this.id = id;
        this.timestamp = timestamp;
        this.metric_type = metric_type;
        this.reference_id = reference_id;
        this.metric_value = metric_value;
        this.unit = unit;
        this.status = status;
    }

    @Override
    public String toString() {
        return "EnergyAnalytics [id=" + id + ", timestamp=" + timestamp + ", metric_type=" + metric_type
                + ", reference_id=" + reference_id + ", metric_value=" + metric_value
                + ", unit=" + unit + ", status=" + status + "]";
    }

    private static EnergyAnalytics from(Row row) {
        return new EnergyAnalytics(
            row.getLong("id"),
            row.getLocalDateTime("timestamp"),
            row.getString("metric_type"),
            row.getString("reference_id"),
            row.getFloat("metric_value"),
            row.getString("unit"),
            row.getString("status")
        );
    }

    public static Multi<EnergyAnalytics> findAll(MySQLPool client) {
        return client.query("SELECT * FROM EnergyAnalytics ORDER BY id ASC").execute()
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(EnergyAnalytics::from);
    }

    public static Uni<EnergyAnalytics> findById(MySQLPool client, Long id) {
        return client.preparedQuery("SELECT * FROM EnergyAnalytics WHERE id = ?").execute(Tuple.of(id))
                .onItem().transform(RowSet::iterator)
                .onItem().transform(iterator -> iterator.hasNext() ? from(iterator.next()) : null);
    }

    public static Multi<EnergyAnalytics> findByMetricType(MySQLPool client, String metricType) {
        return client.preparedQuery("SELECT * FROM EnergyAnalytics WHERE metric_type = ? ORDER BY id ASC").execute(Tuple.of(metricType))
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(EnergyAnalytics::from);
    }
}