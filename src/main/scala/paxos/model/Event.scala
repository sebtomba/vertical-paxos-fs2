package paxos.model

trait Event
object Event {
  case class NewProposerSelected(proposer: String) extends Event
  case class AcceptorActivated(acceptor: String) extends Event
  case class NewBallotCreated(proposer: String, ballot: Long, prevBallot: Long) extends Event
  case class BallotPrepared(acceptor: String, ballot: Long, voteBallot: Long, promised: Option[Any]) extends Event
  case class ValueProposed(proposer: String, ballot: Long, value: Any) extends Event
  case class ValueAccepted(acceptor: String, ballot: Long, value: Any) extends Event
  case class ReplicationCompleted(proposer: String, ballot: Long, prevBallot: Long) extends Event
  case class BallotActivated(proposer: String, ballot: Long, voted: Boolean) extends Event
  case class ProposerFinished(proposer: String, ballot: Long) extends Event
  case class ClientRequestedValue(proposer: String, value: Any) extends Event
  case class LearnerAdoptedValue(learner: String, ballot: Long, value: Any) extends Event
}
