package be.wegenenverkeer.atomium.server.spring;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PostgreSqlSyncQueryProviderTest {

    @Test
    public void syncQuery() throws Exception {
        assertThat(PostgreSqlSyncQueryProvider.syncQuery("oproep_event", "atom_entry", "oproep_event.creation_time ASC")).isEqualTo(""
                + "WITH\n"
                + "    max_atom_entry -- max bepalen, basis voor zetten volgnummer, -1 als nog niet gezet zodat teller bij 0 begint\n"
                + "  AS ( SELECT coalesce(max(oproep_event.atom_entry), -1) max_atom_entry\n"
                + "       FROM oproep_event),\n"
                + "    to_number -- lijst met aan te passen records, moet dit apart bepalen omdat volgorde anders fout is\n"
                + "  AS (\n"
                + "      SELECT\n"
                + "        oproep_event.id,\n"
                //+ "        oproep_event.creation_time,\n"
                + "        max.max_atom_entry max_atom_entry\n"
                + "      FROM oproep_event CROSS JOIN max_atom_entry max\n"
                + "      WHERE oproep_event.atom_entry IS NULL\n"
                + "      ORDER BY oproep_event.creation_time ASC\n"
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
                + "UPDATE oproep_event\n"
                + "SET atom_entry = to_update.new_value\n"
                + "FROM to_update\n"
                + "WHERE oproep_event.id = to_update.id;");
    }

}