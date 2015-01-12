package be.wegenenverkeer.atom

case class EntryNotFoundException(entryRef:EntryRef)
  extends RuntimeException(s"feed ${entryRef.url} does not contain an entry with id = ${entryRef.entryId}")
