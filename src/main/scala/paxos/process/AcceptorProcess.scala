package paxos.process

import paxos.model.Event.{AcceptorActivated, BallotPrepared, ValueAccepted}
import paxos.model.Message._
import paxos.model.State.Acceptor

object AcceptorProcess extends Process {
  override type State = Acceptor

  val behavior: Behavior[Acceptor] = {
    case (state, ActivateAcceptor(acceptor)) if !state.active && state.id == acceptor =>
      val nextState = state.copy(active = true)
      val events = Seq(AcceptorActivated(state.id))
      (nextState, Seq.empty, events)

    case (state, Prepare1a(ballot, prevBallot)) if state.active && ballot >= state.maxBallot =>
      val nextState = state.copy(maxBallot = ballot)
      val messages = Seq(Promise1b(state.id, ballot, prevBallot, state.votes.get(prevBallot)))
      val events = Seq(BallotPrepared(state.id, ballot, prevBallot, state.votes.get(prevBallot)))
      (nextState, messages, events)

    case (state, Propose2a(ballot, value)) if state.active && ballot >= state.maxBallot =>
      val nextState = state.copy(maxBallot = ballot, votes = state.votes + (ballot -> value))
      val messages = Seq(Accept2b(state.id, ballot))
      val events = Seq(ValueAccepted(state.id, ballot, value))
      (nextState, messages, events)
  }

  def init(id: String): Acceptor = Acceptor(id, active = false, Map.empty, 0)
}
