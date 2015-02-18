package be.wegenenverkeer.atomium.server.jdbc

import java.sql.Connection

import be.wegenenverkeer.atomium.server.Context

case class JdbcContext(connection: Connection) extends Context
