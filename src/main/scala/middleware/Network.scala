package middleware

import cats.effect.{Concurrent, Resource}
import cats.Functor
import cats.syntax.functor._

import fs2.Stream
import fs2.concurrent.Topic

trait Network[F[_], M] {
  def broadcast(message: M): F[Boolean]
  def deliver(maxQueued: Int): Resource[F, Stream[F, M]]
}

object Network {
  def apply[F[_]: Concurrent: Functor, M]: F[Network[F, M]] = {
    Topic[F, M].map { topic =>
      new Network[F, M] {
        def broadcast(message: M): F[Boolean] = topic.publish1(message).map(_.isRight)
        def deliver(maxQueued: Int): Resource[F, Stream[F, M]] = topic.subscribeAwait(maxQueued)
      }
    }
  }
}
