package org.acme;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import java.time.LocalDateTime;

public class GridBalancing {

    public Long id;
    public LocalDateTime timestamp;
    public String source_grid_cell;
    public String target_grid_cell;
    public String recommended_action;
    public Float power_kw;
    public String status;

    public GridBalancing() {
    }

    public GridBalancing(Long id, LocalDateTime timestamp, String source_grid_cell, 
                         String target_grid_cell, String recommended_action, 
                         Float power_kw, String status) {
        this.id = id;
        this.timestamp = timestamp;
        this.source_grid_cell = source_grid_cell;
        this.target_grid_cell = target_grid_cell;
        this.recommended_action = recommended_action;
        this.power_kw = power_kw;
        this.status = status;
    }

    @Override
    public String toString() {
        return "GridBalancing [id=" + id + ", timestamp=" + timestamp + ", source_grid_cell=" + source_grid_cell
                + ", target_grid_cell=" + target_grid_cell + ", recommended_action=" + recommended_action
                + ", power_kw=" + power_kw + ", status=" + status + "]";
    }

    private static GridBalancing from(Row row) {
        return new GridBalancing(
            row.getLong("id"),
            row.getLocalDateTime("timestamp"),
            row.getString("source_grid_cell"),
            row.getString("target_grid_cell"),
            row.getString("recommended_action"),
            row.getFloat("power_kw"),
            row.getString("status")
        );
    }

    public static Multi<GridBalancing> findAll(MySQLPool client) {
        return client.query("SELECT * FROM GridBalancing ORDER BY id ASC").execute()
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(GridBalancing::from);
    }

    public static Uni<GridBalancing> findById(MySQLPool client, Long id) {
        return client.preparedQuery("SELECT * FROM GridBalancing WHERE id = ?").execute(Tuple.of(id))
                .onItem().transform(RowSet::iterator)
                .onItem().transform(iterator -> iterator.hasNext() ? from(iterator.next()) : null);
    }
}