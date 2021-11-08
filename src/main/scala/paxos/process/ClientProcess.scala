package paxos.process

import cats.effect.kernel.{Concurrent, Resource}
import cats.effect.syntax.spawn._
import cats.syntax.applicative._
import cats.syntax.functor._
import cats.Applicative

import paxos.model.Event
import paxos.model.Event.BallotActivated
import paxos.model.Message.ClientRequest

object ClientProcess {

  def process[F[_]: Applicative: PaxosNetwork](value: Any): Event => F[Unit] = {
    case BallotActivated(_, _, false) => PaxosNetwork[F].broadcast(ClientRequest(value)).void
    case _ => ().pure[F]
  }

  def start[F[_]: Concurrent: PaxosNetwork: PaxosEventLog](value: Any): Resource[F, Unit] =
    for {
      sub <- PaxosEventLog[F].subscribe(100)
      _ <- sub.evalMap(process[F](value)).compile.drain.background
    } yield ()
}
