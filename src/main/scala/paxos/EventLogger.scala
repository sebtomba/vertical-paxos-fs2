package paxos

import cats.Show
import cats.effect.kernel.Sync
import cats.syntax.show._

import paxos.model.Event
import paxos.model.Event._

object EventLogger {
  implicit val showNewProposerSelected: Show[NewProposerSelected] =
    Show.show(e => s"@${e.proposer}: Proposer activated")
  implicit val showAcceptorActivated: Show[AcceptorActivated] =
    Show.show(e => s"#${e.acceptor}: Acceptor activated")
  implicit val showNewBallotCreated: Show[NewBallotCreated] =
    Show.show(e => s"@${e.proposer}: New ballot ${e.ballot} created, previous ballot is ${e.prevBallot}")
  implicit val showBallotPrepared: Show[BallotPrepared] =
    Show.show { e =>
      val value = e.promised.map(v => s"[$v]").getOrElse("[_]")
      s"#${e.acceptor}: Prepared ballot ${e.ballot}, last ballot ${e.voteBallot} has value $value}"
    }
  implicit val showValueProposed: Show[ValueProposed] =
    Show.show(e => s"#${e.proposer}: Proposed value ${e.value} for ballot ${e.ballot}")
  implicit val showValueAccepted: Show[ValueAccepted] =
    Show.show(e => s"#${e.acceptor}: Accepted value ${e.value} for ballot ${e.ballot}")
  implicit val showReplicationCompleted: Show[ReplicationCompleted] =
    Show.show(e => s"@${e.proposer}: Replication from ballot ${e.prevBallot} to ballot ${e.ballot} completed")
  implicit val showBallotActivated: Show[BallotActivated] =
    Show.show { e =>
      val state = if (e.voted) "closed" else "open"
      s"@${e.proposer}: Activated ballot ${e.ballot} is $state for votes."
    }
  implicit val showProposerFinished: Show[ProposerFinished] =
    Show.show(e => s"@${e.proposer}: Finished")
  implicit val showClientRequestedValue: Show[ClientRequestedValue] =
    Show.show(e => s"#${e.proposer}: Client requested value ${e.value}")
  implicit val showLearnerAdoptedValue: Show[LearnerAdoptedValue] =
    Show.show(e => s"!${e.learner}: Adopted value ${e.value} for ballot ${e.ballot}")

  implicit val showEvent: Show[Event] =
    Show.show {
      case e: NewProposerSelected => e.show
      case e: AcceptorActivated => e.show
      case e: NewBallotCreated => e.show
      case e: BallotPrepared => e.show
      case e: ValueProposed => e.show
      case e: ValueAccepted => e.show
      case e: ReplicationCompleted => e.show
      case e: BallotActivated => e.show
      case e: ProposerFinished => e.show
      case e: ClientRequestedValue => e.show
      case e: LearnerAdoptedValue => e.show
      case e => s"Unknown event $e"
    }

  def log[F[_]: Sync](event: Event): F[Unit] =
    Sync[F].delay(println(event.show))
}
