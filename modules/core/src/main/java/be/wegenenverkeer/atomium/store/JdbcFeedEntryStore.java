package be.wegenenverkeer.atomium.store;

import be.wegenenverkeer.atomium.api.FeedEntryStore;

import java.sql.Connection;

/**
 * Created by Karel Maesen, Geovise BVBA on 07/12/16.
 */
public interface JdbcFeedEntryStore<T> extends FeedEntryStore<T> {

    /**
     * Returns the associated {@code Indexer} for this store
     *
     *
     * The Indexer#index() method should be invoked before every read from this store
     * @return
     */
    public Indexer getIndexer();

    /**
     * Returns the table name
     * @return the table name
     */
    public String getTableName();

    /**
     * Returns the name of the column holding the Entry Id field
     * @return the name of the column holding the Entry Id field
     */
    public String getIdColumnName();

    /**
     * Returns the name of the column holding the "updated" datetimestamp
     * @return the name of the column holding the "updated" datetimestamp
     */
    public String getUpdatedColumnName();

    /**
     * Returns the name of the primary key column.
     *
     * The primary key column needs to be determined by a sequence and will determine the order of entries
     * @return
     */
    public String getPrimaryKeyColumnName();

    /**
     * Returns the name of the column for the entry sequence number
     *
     * The order of entries as determined by primary key must be the same as the order as determined by the sequence number.
     * The sequence number must range from 0..< total number of entries -1 >
     * @return
     */
    public String getSequenceNoColumnName();


    public Connection getConnection();


}
