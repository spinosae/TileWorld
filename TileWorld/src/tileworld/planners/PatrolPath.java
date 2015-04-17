package tileworld.planners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class PatrolPath {
	private List<PatrolPoint> pps = new LinkedList<PatrolPoint>();
	// private PatrolPoint[] = new Array<PatrolPoint>()
	private int pn;

	public PatrolPath(PatrolPoint p1) {
		pps.add(p1);
		pn = 0;
	}
	public PatrolPath(PatrolPoint[] newps) {
		List<PatrolPoint> newpsl = Arrays.asList(newps);
		pps.addAll(newpsl);
		pn = 0;
	}

	public void addPoint(PatrolPoint newp) {
		pps.add(newp);
	}
	public void addPoints(PatrolPoint[] newps){
		List<PatrolPoint> newpsl = Arrays.asList(newps);
		pps.addAll(newpsl);
	}
	public PatrolPoint nextPoint(){
		int nextp = pn;
		pn = (nextp+1) % pps.size();
		PatrolPoint np = pps.get(nextp);
		return np;
	}
}
