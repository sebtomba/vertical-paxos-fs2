package paxos.model

object State {
  case class Master(id: String, ballot: Long, nextBallot: Long)
  case class Acceptor(id: String, active: Boolean, votes: Map[Long, Any], maxBallot: Long)
  case class Learner(id: String, accepted: Set[Message.Accept2b])

  trait ProposerPhase
  object ProposerPhase {
    case object Suspended extends ProposerPhase
    case object Initializing extends ProposerPhase
    case object Preparing extends ProposerPhase
    case object Proposing extends ProposerPhase
    case object Activating extends ProposerPhase
    case object AwaitingRequest extends ProposerPhase
    case object Finished extends ProposerPhase
  }

  case class Proposer(
      id: String,
      phase: ProposerPhase,
      ballot: Long,
      prevBallot: Long,
      safeVal: Option[Any],
      promises: Set[String],
      accepted: Set[String]
  )
}
