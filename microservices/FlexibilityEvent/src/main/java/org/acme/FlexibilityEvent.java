package org.acme;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import java.time.LocalDateTime;

public class FlexibilityEvent {

    public Long id;
    public LocalDateTime timestamp;
    public String asset_id;
    public Long prosumer_id;
    public String grid_cell_id;
    public String logic_type;
    public String proposed_action;
    public Float incentive_value;
    public Float target_value_kw;
    public Long telemetry_reference_id;
    public String status;

    public FlexibilityEvent() {
    }

    public FlexibilityEvent(Long id, LocalDateTime timestamp, String asset_id, Long prosumer_id, String grid_cell_id, 
                            String logic_type, String proposed_action, Float incentive_value, 
                            Float target_value_kw, Long telemetry_reference_id, String status) {
        this.id = id;
        this.timestamp = timestamp;
        this.asset_id = asset_id;
        this.prosumer_id = prosumer_id;
        this.grid_cell_id = grid_cell_id;
        this.logic_type = logic_type;
        this.proposed_action = proposed_action;
        this.incentive_value = incentive_value;
        this.target_value_kw = target_value_kw;
        this.telemetry_reference_id = telemetry_reference_id;
        this.status = status;
    }

    @Override
    public String toString() {
        return "FlexibilityEvent [id=" + id + ", timestamp=" + timestamp + ", asset_id=" + asset_id 
                + ", prosumer_id=" + prosumer_id + ", grid_cell_id=" + grid_cell_id + ", logic_type=" + logic_type 
                + ", proposed_action=" + proposed_action + ", incentive_value=" + incentive_value 
                + ", target_value_kw=" + target_value_kw + ", telemetry_reference_id=" + telemetry_reference_id 
                + ", status=" + status + "]";
    }

    private static FlexibilityEvent from(Row row) {
        return new FlexibilityEvent(
                row.getLong("id"),
                row.getLocalDateTime("timestamp"),
                row.getString("asset_id"),
                row.getLong("prosumer_id"),
                row.getString("grid_cell_id"),
                row.getString("logic_type"),
                row.getString("proposed_action"),
                row.getFloat("incentive_value"),
                row.getFloat("target_value_kw"),
                row.getLong("telemetry_reference_id"),
                row.getString("status")
        );
    }

    public static Multi<FlexibilityEvent> findAll(MySQLPool client) {
        return client.query("SELECT * FROM FlexibilityEvent ORDER BY id ASC").execute()
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(FlexibilityEvent::from);
    }

    public static Uni<FlexibilityEvent> findById(MySQLPool client, Long id) {
        return client.preparedQuery("SELECT * FROM FlexibilityEvent WHERE id = ?").execute(Tuple.of(id))
                .onItem().transform(RowSet::iterator)
                .onItem().transform(iterator -> iterator.hasNext() ? from(iterator.next()) : null);
    }

    public static Multi<FlexibilityEvent> findByGridCellId(MySQLPool client, String gridCellId) {
        return client.preparedQuery("SELECT * FROM FlexibilityEvent WHERE grid_cell_id = ? ORDER BY timestamp DESC").execute(Tuple.of(gridCellId))
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(FlexibilityEvent::from);
    }

    public static Multi<FlexibilityEvent> findByStatus(MySQLPool client, String status) {
        return client.preparedQuery("SELECT * FROM FlexibilityEvent WHERE status = ? ORDER BY timestamp DESC").execute(Tuple.of(status))
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(FlexibilityEvent::from);
    }
}