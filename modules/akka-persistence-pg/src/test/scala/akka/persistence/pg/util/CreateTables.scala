package akka.persistence.pg.util

import akka.persistence.pg.PgConfig

trait CreateTables {
  self: PgConfig =>

  import driver.api._

  lazy val createJournal = sqlu"""create table #${pluginConfig.fullJournalTableName} (
                           "id" BIGSERIAL NOT NULL PRIMARY KEY,
                           "persistenceid" VARCHAR(254) NOT NULL,
                           "sequencenr" INT NOT NULL,
                           "rowid" BIGINT DEFAULT NULL,
                           "deleted" BOOLEAN DEFAULT false,
                           "payload" BYTEA,
                           "manifest" VARCHAR(512),
                           "uuid" VARCHAR(36) NOT NULL,
                           "writeruuid" VARCHAR(36) NOT NULL,
                           "created" timestamptz NOT NULL,
                           "tags" HSTORE,
                           "event" #${pluginConfig.jsonType},
                           constraint "cc_journal_payload_event" check (payload IS NOT NULL OR event IS NOT NULL))"""

  lazy val createSnapshot = sqlu"""create table #${pluginConfig.fullSnapshotTableName} (
                            "persistenceid" VARCHAR(254) NOT NULL,
                            "sequencenr" INT NOT NULL,
                            "timestamp" bigint NOT NULL,
                            "snapshot" BYTEA,
                            PRIMARY KEY (persistenceid, sequencenr))"""

  lazy val createUniqueIndex = sqlu"""CREATE unique INDEX journal_pidseq_idx ON #${pluginConfig.fullJournalTableName} (persistenceid, sequencenr)"""
  lazy val createEventIndex = sqlu"""CREATE INDEX journal_event_idx ON #${pluginConfig.fullJournalTableName} USING gin (event)"""
  lazy val createRowIdIndex = sqlu"""CREATE unique INDEX journal_rowid_idx ON #${pluginConfig.fullJournalTableName} (rowid)"""

  lazy val createTables = createJournal
      .andThen(createUniqueIndex)
      .andThen(createRowIdIndex)
      .andThen(createSnapshot)

  def countEvents                = sql"""select count(*) from #${pluginConfig.fullJournalTableName}""".as[Long].head
  def countEvents(id: String)    = sql"""select count(*) from #${pluginConfig.fullJournalTableName} where persistenceid = $id""".as[Long].head
  def countSnapshots(id: String) = sql"""select count(*) from #${pluginConfig.fullSnapshotTableName} where persistenceid = $id""".as[Long].head
  def countSnapshots             = sql"""select count(*) from #${pluginConfig.fullSnapshotTableName}""".as[Long].head


}
