package tileworld.planners;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class PatrolPath {
	private List<PatrolPoint> pps = new LinkedList<PatrolPoint>();
	public List<PatrolPoint> getPps() {
		return pps;
	}

	private PatrolPoint line1p1, line1p2, line2p1, line2p2;
	private int pn;
	private boolean line = false;

	public enum Shape {
		ZIGZAG, STAIR, CUSTOM
	};

	/**
	 * build patrol path with 1 point
	 * @param p1 the initial point
	 */
	public PatrolPath(PatrolPoint p1) {
		pps.add(p1);
		pn = 0;
	}
	
	/**
	 * build patrol path with many points
	 * @param newps array of patrol points
	 */
	public PatrolPath(PatrolPoint[] newps) {
		List<PatrolPoint> newpsl = Arrays.asList(newps);
		pps.addAll(newpsl);
		pn = 0;
	}

	/**
	 * build patrol path with two lines
	 * @param l1p1 start point of first line
	 * @param l1p2 end point of first line
	 * @param l2p1start point of second line
	 * @param l2p2 end point of second line
	 */
	public PatrolPath(PatrolPoint l1p1, PatrolPoint l1p2, PatrolPoint l2p1,
			PatrolPoint l2p2) {
		this.line1p1 = l1p1;
		this.line1p2 = l1p2;
		this.line2p1 = l2p1;
		this.line2p2 = l2p2;
		this.line = true;
	}

	/**
	 * add point to end of patrol path
	 * @param newp one point to be added
	 */
	public void addPoint(PatrolPoint newp) {
		pps.add(newp);
	}

	/**
	 * add points to end of patrol path
	 * @param newps points to be added
	 */
	public void addPoints(PatrolPoint[] newps) {
		List<PatrolPoint> newpsl = Arrays.asList(newps);
		pps.addAll(newpsl);
	}

	/**
	 * get next point of the patrol path
	 * @return next point of the patrol path
	 */
	public PatrolPoint nextPoint() {
		int nextp = pn;
		pn = (nextp + 1) % pps.size();
		return pps.get(nextp);
	}

	/**
	 * generate patrol points automatically and add to the path
	 * @param precision layers for the ZigZag and Stair shape
	 * @param shape shape of the path
	 */
	public void autoPath(int precision, Shape shape) {
		if (line) {
			pps.clear();
			pps.add(this.line1p1);
			for (int i = 1; i <= precision; i++) {
				PatrolPoint p1 = new PatrolPoint((int) (this.line1p1.getX() + i
						/ (double) precision
						* (this.line1p2.getX() - this.line1p1.getX())),
						(int) (this.line1p1.getY() + i / (double) precision
								* (this.line1p2.getY() - this.line1p1.getY())));
				PatrolPoint p2 = new PatrolPoint((int) (this.line2p1.getX() + i
						/ (double) precision
						* (this.line2p2.getX() - this.line2p1.getX())),
						(int) (this.line2p1.getY() + i / (double) precision
								* (this.line2p2.getY() - this.line2p1.getY())));
				switch (shape) {
				case ZIGZAG:
					pps.add(p1);
					pps.add(p2);
					break;
				case STAIR:
					pps.add(i % 2 == 0 ? p1 : p2);
					pps.add(i % 2 == 0 ? p2 : p1);
					break;
				case CUSTOM:
					PatrolPoint p4 = PatrolPoint.midPoint(this.line1p1,
							this.line1p2);
					PatrolPoint p5 = PatrolPoint.midPoint(this.line2p2,
							this.line1p1);
					PatrolPoint p6 = PatrolPoint.midPoint(this.line1p2,
							this.line2p2);

					PatrolPoint[] pp = { p4, p5, this.line1p2, p4, p6,
							this.line2p2, p5, p6, this.line1p2, p5,
							this.line1p1 };
					this.addPoints(pp);
				}
			}
		}
	}

	/**
	 * reset path
	 */
	public void reset() {
		pps.clear();
	}
}
