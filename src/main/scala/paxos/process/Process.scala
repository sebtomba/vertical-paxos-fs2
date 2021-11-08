package paxos.process

trait Process {
  type State
  def behavior: Behavior[State]
  def init(id: String): State
}
