package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Codec;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by Karel Maesen, Geovise BVBA on 10/12/16.
 */
public interface JdbcDialect {


    <T> GetEventsOp<T> mkGetEventsOp(Connection conn, Codec<T, String> codec, JdbcEventStoreMetadata meta);

    CreateEventTableOp mkCreateEventTableOp(Connection conn, JdbcEventStoreMetadata meta);

    TotalSizeOp mkTotalSizeOp(Connection conn, JdbcEventStoreMetadata meta);

    <T> SaveEventOp<T> mkSaveEventOp(Connection conn, Codec<T, String> codec, JdbcEventStoreMetadata meta) throws
            SQLException;

    IndexOp mkIndexOp(Connection conn, JdbcEventStoreMetadata meta);
}
