package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Codec;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by Karel Maesen, Geovise BVBA on 10/12/16.
 */
public interface JdbcDialect {


    <T> GetEventsOp<T> createGetEntriesOp(Connection conn, Codec<T, String> codec, JdbcEntryStoreMetadata meta);

    CreateTablesOp createEntryTable(Connection conn, JdbcEntryStoreMetadata meta);

    TotalSizeOp createTotalSizeOp(Connection conn, JdbcEntryStoreMetadata meta);

    <T> SaveEventOp<T> createSaveEntryOp(Connection conn, Codec<T, String> codec, JdbcEntryStoreMetadata meta) throws
            SQLException;

    IndexOp createIndexOp(Connection conn, JdbcEntryStoreMetadata meta);
}
