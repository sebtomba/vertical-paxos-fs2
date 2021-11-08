package middleware

import cats.effect.{Concurrent, Resource}
import cats.Functor
import cats.syntax.functor._

import fs2.Stream
import fs2.concurrent.Topic

trait EventLog[F[_], E] {
  def publish(event: E): F[Boolean]
  def subscribe(maxQueued: Int): Resource[F, Stream[F, E]]
}

object EventLog {
  def apply[F[_]: Concurrent: Functor, E]: F[EventLog[F, E]] =
    Topic[F, E].map { topic =>
      new EventLog[F, E] {
        def publish(event: E): F[Boolean] = topic.publish1(event).map(_.isRight)
        def subscribe(maxQueued: Int): Resource[F, Stream[F, E]] = topic.subscribeAwait(maxQueued)
      }
    }
}
