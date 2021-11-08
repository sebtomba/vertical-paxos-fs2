package paxos.process

import paxos.model.Message._
import paxos.model.State.Master

object MasterProcess extends Process {
  override type State = Master

  val behavior: Behavior[Master] = {
    case (state, ActivateProposer(_)) =>
      val nextState = state.copy(nextBallot = state.nextBallot + 1)
      val messages = Seq(NewBallot(state.nextBallot, state.ballot))
      (nextState, messages, Seq.empty)

    case (state, Complete(ballot, prevBallot)) if prevBallot == state.ballot =>
      val nextState = state.copy(ballot = ballot)
      val messages = Seq(Activated(ballot))
      (nextState, messages, Seq.empty)
  }

  def init(id: String): Master = Master(id, 0, 1)
}
