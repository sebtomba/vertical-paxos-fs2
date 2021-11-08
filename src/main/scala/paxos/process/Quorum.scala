package paxos.process

case class Quorum(members: Set[String], required: Int) {
  assert(members.size >= required)

  def apply(acceptors: Set[String]): Boolean =
    acceptors.intersect(members).size >= required
}

object Quorum {
  def apply(required: Int, members: String*): Quorum = new Quorum(members.toSet, required)
}
