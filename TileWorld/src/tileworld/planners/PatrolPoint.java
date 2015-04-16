package tileworld.planners;

public class PatrolPoint {
	private int x, y;
//	private PatrolPoint next;
	public PatrolPoint(int x, int y) {
		this.x = x;
		this.y = y;
//		this.next = null;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
//	public PatrolPoint getNext() {
//		return next;
//	}
//	public void setNext(PatrolPoint next) {
//		this.next = next;
//	}
}
