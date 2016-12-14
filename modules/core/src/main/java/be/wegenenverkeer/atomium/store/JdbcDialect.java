package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.Codec;
import be.wegenenverkeer.atomium.api.Entry;
import be.wegenenverkeer.atomium.format.AtomEntry;
import be.wegenenverkeer.atomium.format.Content;

import java.sql.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Karel Maesen, Geovise BVBA on 10/12/16.
 */
public interface JdbcDialect {


    <T> GetEntriesOp<T> createGetEntriesOp(Connection conn, Codec<T, String> codec, JdbcEntryStoreMetadata meta);

    CreateTablesOp createEntryTable(Connection conn, JdbcEntryStoreMetadata meta);

    TotalSizeOp createTotalSizeOp(Connection conn, JdbcEntryStoreMetadata meta);

    <T> SaveEntryOp<T> createSaveEntryOp(Connection conn, Codec<T, String> codec, JdbcEntryStoreMetadata meta) throws
            SQLException;

    IndexOp createIndexOp(Connection conn, JdbcEntryStoreMetadata meta);
}
