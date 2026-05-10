package org.acme;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;

public class Asset {
	
	public Long id;
	public Long prosumer_id;
	public String asset_id;
	public String asset_type;
	public Float max_capacity_kw;

	public Asset() {
	}

	public Asset(Long id, Long prosumer_id, String asset_id, String asset_type, Float max_capacity_kw) {
		this.id = id;
		this.prosumer_id = prosumer_id;
		this.asset_id = asset_id;
		this.asset_type = asset_type;
		this.max_capacity_kw = max_capacity_kw;
	}

	@Override
	public String toString() {
		return "{id:" + id + ", prosumer_id:" + prosumer_id + ", asset_id:" + asset_id 
				+ ", asset_type:" + asset_type + ", max_capacity_kw:" + max_capacity_kw + "}\n";
	}

	private static Asset from(Row row) {
		return new Asset(row.getLong("id"), row.getLong("prosumer_id"), row.getString("asset_id"),
				row.getString("asset_type"), row.getFloat("max_capacity_kw"));
	}

	public static Multi<Asset> findAll(MySQLPool client) {
		return client.query("SELECT id, prosumer_id, asset_id, asset_type, max_capacity_kw FROM Asset ORDER BY id ASC")
				.execute()
				.onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
				.onItem().transform(Asset::from);
	}

	public static Uni<Asset> findById(MySQLPool client, Long id) {
		return client.preparedQuery("SELECT id, prosumer_id, asset_id, asset_type, max_capacity_kw FROM Asset WHERE id = ?")
				.execute(Tuple.of(id))
				.onItem().transform(RowSet::iterator)
				.onItem().transform(iterator -> iterator.hasNext() ? from(iterator.next()) : null);
	}

	public static Multi<Asset> findByProsumerId(MySQLPool client, Long prosumer_id) {
		return client.preparedQuery("SELECT id, prosumer_id, asset_id, asset_type, max_capacity_kw FROM Asset WHERE prosumer_id = ? ORDER BY id ASC")
				.execute(Tuple.of(prosumer_id))
				.onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
				.onItem().transform(Asset::from);
	}

	public Uni<Boolean> save(MySQLPool client, Long prosumer_id, String asset_id, String asset_type, Float max_capacity_kw) {
		return client.preparedQuery("INSERT INTO Asset(prosumer_id, asset_id, asset_type, max_capacity_kw) VALUES (?,?,?,?)")
				.execute(Tuple.of(prosumer_id, asset_id, asset_type, max_capacity_kw))
				.onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 ); 
	}

	public static Uni<Boolean> delete(MySQLPool client, Long id) {
		return client.preparedQuery("DELETE FROM Asset WHERE id = ?").execute(Tuple.of(id))
				.onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
	}

	public static Uni<Boolean> update(MySQLPool client, Long id, Long prosumer_id, String asset_id, String asset_type, Float max_capacity_kw) {
		return client.preparedQuery("UPDATE Asset SET prosumer_id = ?, asset_id = ?, asset_type = ?, max_capacity_kw = ? WHERE id = ?")
				.execute(Tuple.of(prosumer_id, asset_id, asset_type, max_capacity_kw, id))
				.onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
	}
}
