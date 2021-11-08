package paxos

import middleware.{EventLog, Network}
import paxos.model.{Event, Message}

package object process {
  type Behavior[S] = PartialFunction[(S, Message), (S, Seq[Message], Seq[Event])]
  object Behavior {
    def none[S](state: S): Behavior[S] = { case _ => (state, Seq.empty, Seq.empty) }
  }

  type PaxosNetwork[F[_]] = Network[F, Message]
  object PaxosNetwork {
    def apply[F[_]](implicit N: PaxosNetwork[F]): PaxosNetwork[F] = N
  }

  type PaxosEventLog[F[_]] = EventLog[F, Event]
  object PaxosEventLog {
    def apply[F[_]](implicit E: PaxosEventLog[F]): PaxosEventLog[F] = E
  }
}
