package paxos.process

import cats.effect.{Ref, Resource}
import cats.effect.kernel.Concurrent
import cats.effect.syntax.spawn._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._

import paxos.model.Message

class ProcessEngine[F[_]: Concurrent: PaxosNetwork: PaxosEventLog, S](
    state: Ref[F, S],
    process: Behavior[S]
) {
  def consume(message: Message): F[Unit] = {
    for {
      s0 <- state.get
      (s1, ms, es) = process.orElse(Behavior.none(s0))((s0, message))
      _ <- state.set(s1)
      _ <- es.traverse(e => PaxosEventLog[F].publish(e))
      _ <- ms.traverse(m => PaxosNetwork[F].broadcast(m))
    } yield ()
  }
}

object ProcessEngine {
  def start[F[_]: Concurrent: PaxosNetwork: PaxosEventLog](
      id: String,
      process: Process,
      maxQueued: Int = 10
  ): Resource[F, Unit] =
    for {
      ref <- Resource.eval(Ref[F].of(process.init(id)))
      eng <- Resource.pure(new ProcessEngine(ref, process.behavior))
      sub <- PaxosNetwork[F].deliver(maxQueued)
      _ <- sub.evalMap(eng.consume).compile.drain.background
    } yield ()
}
