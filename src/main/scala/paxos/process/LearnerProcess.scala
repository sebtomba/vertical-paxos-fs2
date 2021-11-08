package paxos.process

import cats.effect.kernel.{Concurrent, Resource}
import cats.effect.syntax.spawn._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._

import fs2.concurrent.SignallingRef
import paxos.model.Event
import paxos.model.Event.{LearnerAdoptedValue, ValueAccepted}

object LearnerProcess {

  def process[F[_]: Concurrent: PaxosNetwork: PaxosEventLog](
      id: String,
      confirmations: Int,
      signal: SignallingRef[F, Boolean]
  )(accepted: Map[Long, Set[String]], event: Event): F[(Map[Long, Set[String]], Unit)] =
    event match {
      case ValueAccepted(acceptor, ballot, value) if accepted.keys.isEmpty || accepted.keys.max <= ballot =>
        signal.get.ifM(
          (accepted -> ()).pure[F],
          Concurrent[F].unit.flatMap { _ =>
            val acceptors = accepted.getOrElse(ballot, Set.empty[String]) + acceptor
            val nextAcc = accepted + (ballot -> acceptors)
            if (acceptors.size >= confirmations)
              PaxosEventLog[F].publish(LearnerAdoptedValue(s"$id", ballot, value)) >> signal.set(true).as(nextAcc -> ())
            else (nextAcc -> ()).pure[F]
          }
        )

      case _ => (accepted -> ()).pure[F]
    }

  def start[F[_]: Concurrent: PaxosNetwork: PaxosEventLog](
      id: String,
      confirmations: Int,
      signal: SignallingRef[F, Boolean]
  ): Resource[F, Unit] =
    for {
      sub <- PaxosEventLog[F].subscribe(100)
      _ <- sub
        .evalMapAccumulate(Map.empty[Long, Set[String]])(process(id, confirmations, signal))
        .compile
        .drain
        .background
    } yield ()
}
