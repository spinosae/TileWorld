package tileworld.planners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class PatrolPath {
	private List<PatrolPoint> pps = new LinkedList<PatrolPoint>();
	public List<PatrolPoint> getPps() {
		return pps;
	}
	// private PatrolPoint[] = new Array<PatrolPoint>()
	private PatrolPoint line1p1, line1p2, line2p1, line2p2;
	private int pn;
	private boolean line = false;
	public enum Shape {ZIGZAG, STAIR};

	public PatrolPath(PatrolPoint p1) {
		pps.add(p1);
		pn = 0;
	}

	public PatrolPath(PatrolPoint[] newps) {
		List<PatrolPoint> newpsl = Arrays.asList(newps);
		pps.addAll(newpsl);
		pn = 0;
	}

	public PatrolPath(PatrolPoint l1p1, PatrolPoint l1p2, PatrolPoint l2p1,
			PatrolPoint l2p2) {
		this.line1p1 = l1p1;
		this.line1p2 = l1p2;
		this.line2p1 = l2p1;
		this.line2p2 = l2p2;
		this.line = true;
	}

	public void addPoint(PatrolPoint newp) {
		pps.add(newp);
	}

	public void addPoints(PatrolPoint[] newps) {
		List<PatrolPoint> newpsl = Arrays.asList(newps);
		pps.addAll(newpsl);
	}

	public PatrolPoint nextPoint() {
		int nextp = pn;
		pn = (nextp + 1) % pps.size();
		return pps.get(nextp);
	}

	public void autoPath(int precision, Shape shape) {
		if (line) {
//			PatrolPoint[] line1 = new PatrolPoint[precision];
//			PatrolPoint[] line2 = new PatrolPoint[precision];
//			line1[0] = this.line1p1;
//			line1[precision - 1] = this.line1p2;
//			line2[0] = this.line2p1;
//			line2[precision - 1] = this.line2p2;
			pps.clear();
			pps.add(this.line1p1);
			for (int i = 1; i <= precision; i++) {
				PatrolPoint p1 = new PatrolPoint(
						(int) (this.line1p1.getX() + i / (double)precision* (this.line1p2.getX() - this.line1p1.getX())),
						(int) (this.line1p1.getY() + i / (double)precision* (this.line1p2.getY() - this.line1p1.getY())));
				PatrolPoint p2 = new PatrolPoint(
						(int) (this.line2p1.getX() + i/ (double)precision* (this.line2p2.getX() - this.line2p1.getX())),
						(int) (this.line2p1.getY() + i / (double)precision* (this.line2p2.getY() - this.line2p1.getY())));
				switch(shape){
				case ZIGZAG :
					pps.add(p1);
					pps.add(p2);
					break;
				case STAIR:
					pps.add(i%2==0?p1:p2);
					pps.add(i%2==0?p2:p1);
					break;
				}
			}
		}
	}
//	public void stair(int precision) {
//		if (line) {
////			PatrolPoint[] line1 = new PatrolPoint[precision];
////			PatrolPoint[] line2 = new PatrolPoint[precision];
////			line1[0] = this.line1p1;
////			line1[precision - 1] = this.line1p2;
////			line2[0] = this.line2p1;
////			line2[precision - 1] = this.line2p2;
//			pps.clear();
//			for (int i = 0; i <= precision; i++) {
//				PatrolPoint p1 = new PatrolPoint(
//						(int) (this.line1p1.getX() + i / (double)precision* (this.line1p2.getX() - this.line1p1.getX())),
//						(int) (this.line1p1.getY() + i / (double)precision* (this.line1p2.getY() - this.line1p1.getY())));
//				PatrolPoint p2 = new PatrolPoint(
//						(int) (this.line2p1.getX() + i/ (double)precision* (this.line2p2.getX() - this.line2p1.getX())),
//						(int) (this.line2p1.getY() + i / (double)precision* (this.line2p2.getY() - this.line2p1.getY())));
//				pps.add(i%2==0?p1:p2);
//				pps.add(i%2==0?p2:p1);
//			}
//		}
//	}
	public void reset() {
		pps.clear();
	}
}
