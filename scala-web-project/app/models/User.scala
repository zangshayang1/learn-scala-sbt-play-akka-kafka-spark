package models

import java.util.UUID
import scalikejdbc.WrappedResultSet

case class User(userId: UUID, userCode: String, password: String)

// a helper function that we can use to map the DB query result to User object
object User {
  def fromRS(rs: WrappedResultSet) : User = {
    User(
      UUID.fromString(rs.string("user_id")),
      rs.string("user_code"),
      rs.string("password")
        )
  }
}

/** On WrappedResultSet

There are methods that return a value of a particular type,
  for example string returns String, short returns Short and so on.

There are also methods whose names end with Opt,
  for example stringOpt. These return Option values,
  i.e None when the database value is NULL and Some when it’s not.

Since we have only non-nullable columns in our table,
  we don’t need to use Opt-methods.
*/
