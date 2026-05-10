package org.acme;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;

public class GridCell {
	
	public Long id;
	public Long operator_id;
	public String grid_cell_id;
	public String location;
	public Float max_load_mw;

	public GridCell() {
	}

	public GridCell(Long id, Long operator_id, String grid_cell_id, String location, Float max_load_mw) {
		this.id = id;
		this.operator_id = operator_id;
		this.grid_cell_id = grid_cell_id;
		this.location = location;
		this.max_load_mw = max_load_mw;
	}

	@Override
	public String toString() {
		return "{id:" + id + ", operator_id:" + operator_id + ", grid_cell_id:" + grid_cell_id 
				+ ", location:" + location + ", max_load_mw:" + max_load_mw + "}\n";
	}

	private static GridCell from(Row row) {
		return new GridCell(row.getLong("id"), row.getLong("operator_id"), row.getString("grid_cell_id"),
				row.getString("location"), row.getFloat("max_load_mw"));
	}

	public static Multi<GridCell> findAll(MySQLPool client) {
		return client.query("SELECT id, operator_id, grid_cell_id, location, max_load_mw FROM GridCell ORDER BY id ASC")
				.execute()
				.onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
				.onItem().transform(GridCell::from);
	}

	public static Uni<GridCell> findById(MySQLPool client, Long id) {
		return client.preparedQuery("SELECT id, operator_id, grid_cell_id, location, max_load_mw FROM GridCell WHERE id = ?")
				.execute(Tuple.of(id))
				.onItem().transform(RowSet::iterator)
				.onItem().transform(iterator -> iterator.hasNext() ? from(iterator.next()) : null);
	}

	public static Multi<GridCell> findByOperatorId(MySQLPool client, Long operator_id) {
		return client.preparedQuery("SELECT id, operator_id, grid_cell_id, location, max_load_mw FROM GridCell WHERE operator_id = ? ORDER BY id ASC")
				.execute(Tuple.of(operator_id))
				.onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
				.onItem().transform(GridCell::from);
	}

	public Uni<Long> save(MySQLPool client, Long operator_id, String grid_cell_id, String location, Float max_load_mw) {
		return client.preparedQuery("INSERT INTO GridCell(operator_id, grid_cell_id, location, max_load_mw) VALUES (?,?,?,?)")
				.execute(Tuple.of(operator_id, grid_cell_id, location, max_load_mw))
				.onItem().transform(pgRowSet -> pgRowSet.property(io.vertx.mutiny.mysqlclient.MySQLClient.LAST_INSERTED_ID));
	}

	public static Uni<Boolean> delete(MySQLPool client, Long id) {
		return client.preparedQuery("DELETE FROM GridCell WHERE id = ?").execute(Tuple.of(id))
				.onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
	}

	public static Uni<Boolean> updateCapacity(MySQLPool client, Long id, Float max_load_mw) {
		return client.preparedQuery("UPDATE GridCell SET max_load_mw = ? WHERE id = ?")
				.execute(Tuple.of(max_load_mw, id))
				.onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
	}
}
