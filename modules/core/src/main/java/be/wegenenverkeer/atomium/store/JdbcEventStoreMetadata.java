package be.wegenenverkeer.atomium.store;

/**
 * An JdbcFeedEntryStore
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 07/12/16.
 */
public class JdbcEventStoreMetadata {

    public static final String DEFAULT_ENTRY_VAL_COLUMN_TYPE = "TEXT";

    private final String tableName;
    private final String idColumnName;
    private final String updatedColumnName;
    private final String primaryKeyColumnName;
    private final String sequenceNoColumnName;
    private final String entryValColumnName;
    private final String entryValColumnType;

    public JdbcEventStoreMetadata(String tableName, String idColumnName, String updatedColumnName, String primaryKeyColumnName, String sequenceNoColumnName, String entryValColumnName, String entryValColumnType) {
        this.tableName = tableName;
        this.idColumnName = idColumnName;
        this.updatedColumnName = updatedColumnName;
        this.primaryKeyColumnName = primaryKeyColumnName;
        this.sequenceNoColumnName = sequenceNoColumnName;
        this.entryValColumnName = entryValColumnName;
        this.entryValColumnType = entryValColumnType;
    }

    public JdbcEventStoreMetadata(String tableName, String idColumnName, String updatedColumnName, String primaryKeyColumnName, String sequenceNoColumnName, String entryValColumnName) {
        this(
                tableName,
                idColumnName,
                updatedColumnName,
                primaryKeyColumnName,
                sequenceNoColumnName,
                entryValColumnName,
                DEFAULT_ENTRY_VAL_COLUMN_TYPE
        );
    }

    /**
     * Returns the table name
     *
     * @return the table name
     */
    public String getTableName() {
        return this.tableName;
    }

    /**
     * Returns the name of the column holding the Entry Id field
     *
     * @return the name of the column holding the Entry Id field
     */
    public String getIdColumnName() {
        return this.idColumnName;
    }

    /**
     * Returns the name of the column holding the "updated" datetimestamp
     *
     * @return the name of the column holding the "updated" datetimestamp
     */
    public String getUpdatedColumnName() {
        return this.updatedColumnName;
    }

    /**
     * Returns the name of the primary key column.
     * <p>
     * The primary key column needs to be determined by a sequence and will determine the order of entries
     *
     * @return the name of the primary key column
     */
    public String getPrimaryKeyColumnName() {
        return this.primaryKeyColumnName;
    }

    /**
     * Returns the name of the column for the entry sequence number
     * <p>
     * The order of entries as determined by primary key must be the same as the order as determined by the sequence number.
     * The sequence number must range from 0 to the total number of entries -1
     *
     * @return the name of the column for the entry sequence number
     */
    public String getSequenceNoColumnName() {
        return this.sequenceNoColumnName;
    }

    /**
     * Returns the name of the column for the entity payload
     *
     * @return the name of the column for the entity payload
     */
    public String getEntryValueColumnName() {
        return this.entryValColumnName;
    }


    /**
     * Returns the sql type of the column for the entity payload
     *
     * @return the sql type of the column for the entity payload
     */
    public String getEntryValColumnType() {
        return entryValColumnType;
    }
}
