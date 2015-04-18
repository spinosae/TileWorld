package tileworld.agent;

import sim.field.grid.ObjectGrid2D;
import sim.util.Int2D;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEntity;
import tileworld.environment.TWEnvironment;
import tileworld.environment.TWFuelStation;
import tileworld.environment.TWHole;
import tileworld.environment.TWTile;
import tileworld.exceptions.CellBlockedException;
import tileworld.exceptions.InsufficientFuelException;
import tileworld.planners.AstarPathGenerator;
import tileworld.planners.PatrolPath;
import tileworld.planners.PatrolPath.Shape;
import tileworld.planners.PatrolPoint;
import tileworld.planners.TWPath;
import tileworld.planners.TWPathGenerator;
import tileworld.planners.TWPathStep;

import java.lang.Math;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReactiveAgent extends TWAgent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final int id;
	private TWPath path;
	private PathFor pathFor = PathFor.NONE;
	private PatrolPath pp;
	private PatrolPoint nextp;
	private Random randomGenerator = new Random();
	private TWPathGenerator pathGenerator = new AstarPathGenerator(
			this.getEnvironment(), this, 100);
	private static final Logger LOGGER = Logger
			.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private long startTime;
	private int FUEL_LOW = 50;
	private int SEARCH_DEPTH = 20;
	private int FUEL_CRITICAL = 15;
	private int TIME_THRES = 20;
	private int DIST_THRES = 3;
	private int TILE_CAP = 3;

	public int getTileNum() {
		return this.carriedTiles.size();
	}

	public boolean canCarry() {
		return this.carriedTiles.size() < TILE_CAP;
	}

	public enum PathFor {
		FUEL, TILE_HOLE, NONE, RANDOM, PATROL
	}

	public ReactiveAgent(int xpos, int ypos, int id, TWEnvironment env,
			double fuelLevel) {
		super(xpos, ypos, env, fuelLevel);
		this.id = id;
		this.setPatrol();
		this.FUEL_LOW = (env.getxDimension() + env.getyDimension()) / 4;
		this.SEARCH_DEPTH = env.getxDimension() + env.getyDimension();
		this.DIST_THRES = this.sensor.sensorRange;
		startTime = System.nanoTime();
		LOGGER.setLevel(Level.INFO);
	}

	private void setPatrol() {
		TWEnvironment env = this.getEnvironment();
		int dev = this.sensor.sensorRange, xmax = env.getxDimension(), ymax = env
				.getyDimension();
		// PatrolPoint p1 = new PatrolPoint(dev, ymax / 2);
		// PatrolPoint p3 = new PatrolPoint(xmax / 2, ymax - dev);
		// PatrolPoint p5 = new PatrolPoint(xmax / 2 - 2 * dev, ymax / 2 + 2 *
		// dev);
		PatrolPoint p1 = new PatrolPoint(dev, ymax - dev);
		PatrolPoint p2 = new PatrolPoint(xmax - 3 * dev, ymax - dev);
		PatrolPoint p3 = new PatrolPoint(dev, dev);
		// PatrolPoint p4 = PatrolPoint.midPoint(p3, p1);
		// PatrolPoint p5 = PatrolPoint.midPoint(p2, p3);
		// PatrolPoint p6 = PatrolPoint.midPoint(p1, p2);

		PatrolPoint pp1 = new PatrolPoint(xmax - dev, dev);
		PatrolPoint pp2 = new PatrolPoint(xmax - dev, ymax - dev);
		PatrolPoint pp3 = new PatrolPoint(3 * dev, dev);
//		PatrolPoint pp4 = PatrolPoint.midPoint(pp3, pp1);
//		PatrolPoint pp5 = PatrolPoint.midPoint(pp2, pp3);
//		PatrolPoint pp6 = PatrolPoint.midPoint(pp1, pp2);

		// PatrolPoint[] p1p = { p4, p5, p1, p4, p6, p2, p5, p6, p1, p5, p3 };
//		PatrolPoint[] p2p = { pp4, pp5, pp1, pp4, pp6, pp2, pp5, pp6, pp1, pp5,
//				pp3 };
//		PatrolPoint[] p1p = { p1, p2, p3 };
//		 PatrolPoint[] p2p = { pp1, pp2,pp3 };
		if (id % 2 == 1) {
//			pp = new PatrolPath(p1p);// 1
			 pp = new PatrolPath(p3,p1,p3,p2);// 1
			 pp.autoPath(3, Shape.ZIGZAG);
		} else {
//			pp = new PatrolPath(p2p);// 1
			 pp = new PatrolPath(pp3,pp1,pp3,pp2);// 1
			 pp.autoPath(3, Shape.STAIR);
		}
		for (PatrolPoint p : pp.getPps()) {
			LOGGER.warning(p.getX() + " " + p.getY());
		}
		this.nextp = pp.nextPoint();
	}

	@Override
	protected TWThought think() {
		if (this.getX() > this.getEnvironment().getxDimension()
				|| this.getY() > this.getEnvironment().getyDimension()) {
			LOGGER.warning("out of border");
		}
		LOGGER.info("agent " + id + " score: " + this.score + " at " + this.getX() + " " + this.getY());
		LOGGER.info("runtime: " + (int)((System.nanoTime() - startTime)/1000000));
		return this.react();
	}

	@Override
	protected void act(TWThought thought) {
		TWEntity e = (TWEntity) this.getMemory().getMemoryGrid()
				.get(this.getX(), this.getY());
		switch (thought.getAction()) {
		case PICKUP:
			try {
				this.pickUpTile((TWTile) e);
			} catch (Exception ex) {
				LOGGER.info("no tile on the ground");
			}
			this.getMemory().removeAgentPercept(this.getX(), this.getY());
			break;
		case REFUEL:
			try {
				this.refuel();
			} catch (Exception ex) {
				LOGGER.info("not at fuel station");
			}
			break;
		case PUTDOWN:
			try {
				this.putTileInHole((TWHole) e);
			} catch (Exception ex) {
				LOGGER.info("no hole on the ground");
			}
			this.getMemory().removeAgentPercept(this.getX(), this.getY());
			break;
		case MOVE:
			try {
				this.move(thought.getDirection());
			} catch (CellBlockedException e1) {
				LOGGER.info("cell blocked");
				this.path = null;
			} catch (InsufficientFuelException e2) {
				LOGGER.info("agent: " + id);
			}
			this.getMemory().removeAgentPercept(this.getX(), this.getY());
			break;
		default:
			break;
		}
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Reactive Agent " + this.id;
	}

	private boolean atFuelStation() {
		return this.getX() == this.getEnvironment().getFuelingStation().getX()
				&& this.getY() == this.getEnvironment().getFuelingStation()
						.getY();
	}

	private boolean fuelLow() {
		return this.getFuelLevel() <= this.getX() + this.getY() + FUEL_LOW;
	}

	private boolean fuelCritical() {
		return this.getFuelLevel() <= this.getX() + this.getY() + FUEL_CRITICAL;
	}

	private TWThought react() {
		LOGGER.info("agent " + id);
		TWAction act;
		TWDirection dir = null;
		ObjectGrid2D objectGrid = this.getMemory().getMemoryGrid();
		TWEntity e = (TWEntity) objectGrid.get(this.getX(), this.getY());
		// LOGGER.info("object on current location: " + e.getClass());
		// LOGGER.info("at fuel station:" + this.atFuelStation());
		if (this.canCarry() && e != null && (e instanceof TWTile)) {
			act = TWAction.PICKUP;
			LOGGER.info("Action: PICKUP");
		} else if (e != null && (e instanceof TWHole) && this.hasTile()) {
			act = TWAction.PUTDOWN;
			LOGGER.info("Action: PUTDOWN");
		} else if (this.atFuelStation() && this.fuelLow()) {
			act = TWAction.REFUEL;
			LOGGER.info("Action: REFUEL");
		} else {
			LOGGER.info("Action: MOVE");
			act = TWAction.MOVE;
			dir = this.toFuel();
		}
		LOGGER.info(" ");
		return new TWThought(act, dir);
	}

	private TWDirection toFuel() {
		LOGGER.info("to fuel");
		TWDirection dir = null;
		if (this.fuelLow()) {
			if (this.path != null && this.path.hasNext()
					&& pathFor == PathFor.FUEL) {
				LOGGER.info("	current path: " + this.path + " heading to: "
						+ this.path.getpath().getLast().getX() + " "
						+ this.path.getpath().getLast().getY());
				dir = path.popNext().getDirection();
			} else {
				TWFuelStation tf = this.getEnvironment().getFuelingStation();
				LOGGER.info("	generating path from: "
						+ this.getX()
						+ " "
						+ this.getY()
						+ " to "
						+ java.lang.Math.max(tf.getX(), this.getX()
								- this.sensor.sensorRange)
						+ " "
						+ java.lang.Math.max(tf.getY(), this.getY()
								- this.sensor.sensorRange));
				TWPath lpath = new AstarPathGenerator(this.getEnvironment(),
						this, SEARCH_DEPTH).findPath(this.getX(), this.getY(),
						0, 0);
				if (lpath != null && lpath.hasNext()) {
					this.path = lpath;
					this.pathFor = PathFor.FUEL;
					dir = path.popNext().getDirection();
				} else {
					if (fuelCritical()) {
						LOGGER.info("	cannot find a path, waiting");
						dir = TWDirection.Z;
					} else {
						dir = this.find();
					}
				}
			}
		} else {
			dir = this.find();
		}
		return dir;
	}

	private TWDirection find() {
		LOGGER.info("find");
		TWEntity goal = null;
		TWDirection dir = null;
		if (this.path != null && this.path.hasNext()
				&& (pathFor == PathFor.TILE_HOLE)) {
			LOGGER.info("	current path: " + this.path + " heading to: "
					+ this.path.getpath().getLast().getX() + " "
					+ this.path.getpath().getLast().getX());
			dir = path.popNext().getDirection();
		} else {
			if (!this.hasTile() && this.canCarry()) {
				LOGGER.info("	looking for tile");
				goal = this.getMemory().getNearbyTile(this.getX(), this.getY(),
						TIME_THRES);
			} else {
				LOGGER.info("	looking for hole");
				goal = this.getMemory().getNearbyHole(this.getX(), this.getY(),
						TIME_THRES);
				if (goal == null && this.canCarry()) {
					LOGGER.info("	looking for tile");
					goal = this.getMemory().getNearbyTile(this.getX(),
							this.getY(), TIME_THRES);
				}
			}
			if (goal != null) {
				LOGGER.info("	generating path from: " + this.getX() + " "
						+ this.getY() + " to " + goal.getX() + " "
						+ goal.getY());
				TWPath lpath = pathGenerator.findPath(this.getX(), this.getY(),
						goal.getX(), goal.getY());
				if (lpath != null && lpath.hasNext()) {
					this.path = lpath;
					pathFor = PathFor.TILE_HOLE;
					dir = path.popNext().getDirection();
				} else {
					LOGGER.info("	no path available to" + goal.getX() + " "
							+ goal.getY());
					dir = this.patrol();
				}
			} else {
				LOGGER.info("	no tile or hole nearby");
				// dir = this.wander();
				dir = this.patrol();
			}
		}
		return dir;
	}

	private TWDirection patrol() {
		TWDirection dir = null;
		LOGGER.info("patroling");
		if (this.path != null && this.path.hasNext()
				&& pathFor == PathFor.PATROL) {
			LOGGER.info("	current path: " + this.path + " heading to: "
					+ this.path.getpath().getLast().getX() + " "
					+ this.path.getpath().getLast().getX());
			dir = path.popNext().getDirection();
		} else {
			if (java.lang.Math.abs(this.getX() - nextp.getX()) < DIST_THRES
					&& java.lang.Math.abs(this.getY() - nextp.getY()) < DIST_THRES) {
				nextp = this.pp.nextPoint();
			}
			TWPath lpath = pathGenerator.findPath(this.getX(), this.getY(),
					nextp.getX(), nextp.getY());
			if (lpath != null && lpath.hasNext()) {
				this.path = lpath;
				this.pathFor = PathFor.PATROL;
				dir = path.popNext().getDirection();
			} else {
				LOGGER.info("	no path available to " + nextp.getX() + " "
						+ nextp.getY());
				dir = this.wander();
			}
		}
		return dir;
	}

	private TWDirection wander() {
		LOGGER.info("wander");
		Int2D goal = null;
		TWDirection dir = null;
		LOGGER.info("	" + this.path);
		if (this.path != null && this.path.hasNext()
				&& pathFor == PathFor.RANDOM) {
			LOGGER.info("	current path: " + this.path + " heading to: "
					+ this.path.getpath().getLast().getX() + " "
					+ this.path.getpath().getLast().getX());
			dir = path.popNext().getDirection();
		} else {
			goal = this.getRandomLocation();
			TWPath lpath = pathGenerator.findPath(this.getX(), this.getY(),
					goal.getX(), goal.getY());
			if (lpath != null && lpath.hasNext()) {
				LOGGER.info("	random walking towards: " + goal.getX() + " "
						+ goal.getY());
				this.path = lpath;
				this.pathFor = PathFor.RANDOM;
				dir = path.popNext().getDirection();
				LOGGER.info(this.path + " " + this.path.hasNext() + " "
						+ this.pathFor);
			} else {
				LOGGER.info("	no path available");
				dir = this.getRandomDirection();
				LOGGER.info("	random walking: " + dir);
				int count = 0;
				while (this.getMemory().isCellBlocked(this.getX() + dir.dx,
						this.getY() + dir.dy)) {
					LOGGER.info("	random walking: " + dir);
					dir = this.getRandomDirection();
					if (count > 10) {
						dir = TWDirection.Z;
						break;
					}
					count++;
				}
			}
		}
		return dir;
	}

	private TWDirection getRandomDirection() {

		TWDirection randomDir = TWDirection.values()[this.getEnvironment().random
				.nextInt(4)];
		while ((this.getX() + randomDir.dx) < 0
				|| (this.getX() + randomDir.dx) > (this.getEnvironment()
						.getxDimension() - 1)
				|| (this.getY() + randomDir.dy) < 0
				|| (this.getY() + randomDir.dy) > (this.getEnvironment()
						.getyDimension() - 1)) {
			randomDir = TWDirection.values()[this.getEnvironment().random
					.nextInt(4)];
		}
		// if (this.getX() >= this.getEnvironment().getxDimension() - 1) {
		// randomDir = TWDirection.W;
		// } else if (this.getX() <= 0) {
		// randomDir = TWDirection.E;
		// } else if (this.getY() <= 0) {
		// randomDir = TWDirection.S;
		// } else if (this.getY() >= this.getEnvironment().getxDimension() - 1)
		// {
		// randomDir = TWDirection.N;
		// }

		return randomDir;

	}

	private Int2D getRandomLocation() {
		int gx = 1, gy = 1;
		int xthres = (int) (0 * this.getEnvironment().getxDimension());

		gx = xthres
				+ randomGenerator.nextInt(this.getEnvironment().getxDimension()
						- xthres);
		int ythres = (int) ((double) gx / this.getEnvironment().getxDimension() * this
				.getEnvironment().getyDimension());
		if (id % 2 == 1) {
			gy = ythres
					+ randomGenerator.nextInt(this.getEnvironment()
							.getyDimension() - ythres);
		} else {
			gy = randomGenerator.nextInt(this.getEnvironment().getyDimension()
					- ythres);
		}
		return new Int2D(gx, gy);
	}
}
