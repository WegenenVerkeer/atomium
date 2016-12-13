package be.wegenenverkeer.atomium.store;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by Karel Maesen, Geovise BVBA on 07/12/16.
 */
public class PostgresIndexer implements Indexer {


    private final String indexQuery;

    public PostgresIndexer(JdbcEntryStoreMetadata metadata) {

        this.indexQuery = syncQuery(
                metadata.getTableName(),
                metadata.getPrimaryKeyColumnName(),
                metadata.getSequenceNoColumnName(),
                metadata.getPrimaryKeyColumnName());
    }

    /**
     * Indexes the Entries in a table by running prepared SQL statement
     *
     * Ensure that this operation takes place in its own transaction.
     *
     * @param connection
     * @return
     * @throws SQLException
     */
    public boolean index(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(indexQuery);
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw e;
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    // ignore, nothing we can do
                }
            }
        }
    }

    /**
     * Bouw de SQL query die gebruikt kan worden om een volgnummer veld te zetten voor een tabel met events.
     * Dat volgnummer zorgt voor een voorspelbare en consistente volgorde waarop de events in een atomium feed kopen.
     * Gebruik van deze query voorkomt tussenvoegen van events en zorgt dat de events in verschillende thread kunnen geproduceerd worden.
     * <p>
     * Deze query is gebouwd om atomic te zijn. De query bestaat uit 2 delen:
     * 1) eerst worden de nieuwe atom_entries berekend via een common table expression.
     * In Postgres kan dat via het WITH statement.
     * https://www.postgresql.org/docs/current/static/queries-with.html
     * <p>
     * * Bepaal eerst de huidige hoogste waarde van de bestaande atom entries. Dat wordt opgeslagen als max_atom_entry
     * * Bepaal dan de lijst van items die nog geen atom_entry hebben, en orden die
     * * Vervolgens bereken je voor die lijst de nieuwe atom_entry waarde
     * <p>
     * 2) dan wordt deze table gebruikt om ook effectief de tabel te updaten. In Postgres kan dat handig
     * via UPDATE ... FROM:
     * https://www.postgresql.org/docs/current/static/sql-update.html
     *
     * @param table         naar van de tabel waarin het volgnummer zit, vb "oproep_event"
     * @param sequenceField veld dat gebruikt wordt om de volgorde aan te duiden, vb "atom_entry"
     * @param orderBy       deel van de SQL query om de volgorde van de items te bepalen, vb "oproep_event.creation_time ASC"
     * @return volledige SQL query om de sync uit te voeren
     */
    public String syncQuery(String table, String idField, String sequenceField, String orderBy) {
        return (""
                + "WITH\n"
                + "    max_atom_entry -- max bepalen, basis voor zetten volgnummer, -1 als nog niet gezet zodat teller bij 0 begint\n"
                + "  AS ( SELECT coalesce(max(${table}.${sequence-field}), -1) max_atom_entry\n"
                + "       FROM ${table}),\n"
                + "    to_number -- lijst met aan te passen records, moet dit apart bepalen omdat volgorde anders fout is\n"
                + "  AS (\n"
                + "      SELECT\n"
                + "        ${table}.${idField},\n"
                + "        max.max_atom_entry max_atom_entry\n"
                + "      FROM ${table} CROSS JOIN max_atom_entry max\n"
                + "      WHERE ${table}.${sequence-field} IS NULL\n"
                + "      ORDER BY ${order-by}\n"
                + "  ),\n"
                + "    to_update -- lijst met wijzigingen opbouwen\n"
                + "  AS (\n"
                + "      SELECT\n"
                + "        id,\n"
                + "        (row_number()\n"
                + "        OVER ()) + max_atom_entry new_value\n"
                + "      FROM to_number\n"
                + "      ORDER BY id ASC\n"
                + "  )\n"
                + "-- wijzigingen toepassen\n"
                + "UPDATE ${table}\n"
                + "SET ${sequence-field} = to_update.new_value\n"
                + "FROM to_update\n"
                + "WHERE ${table}.id = to_update.id;")
                .replace("${table}", table)
                .replace("${sequence-field}", sequenceField)
                .replace("${idField}", idField)
                .replace("${order-by}", orderBy);
    }

}
