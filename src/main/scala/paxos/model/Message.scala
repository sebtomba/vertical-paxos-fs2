package paxos.model

trait Message
object Message {
  case class ActivateProposer(proposer: String) extends Message // Oracle message
  case class ActivateAcceptor(acceptor: String) extends Message // Oracle message
  case class NewBallot(ballot: Long, prevBallot: Long) extends Message
  case class Prepare1a(ballot: Long, prevBallot: Long) extends Message
  case class Promise1b(acceptor: String, ballot: Long, voteBallot: Long, promised: Option[Any]) extends Message
  case class Propose2a(ballot: Long, value: Any) extends Message
  case class Accept2b(acceptor: String, ballot: Long) extends Message
  case class Complete(ballot: Long, prevBallot: Long) extends Message
  case class Activated(ballot: Long) extends Message
  case class ClientRequest(value: Any) extends Message
}
