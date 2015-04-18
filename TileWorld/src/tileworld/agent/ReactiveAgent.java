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

	/**
	 * @param xpos
	 *            initial position of the agent
	 * @param ypos
	 * @param id
	 *            identifier of the agent
	 * @param env
	 *            environment
	 * @param fuelLevel
	 *            fuel level
	 */
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

	@Override
	protected TWThought think() {
		if (this.getX() > this.getEnvironment().getxDimension()
				|| this.getY() > this.getEnvironment().getyDimension()) {
			LOGGER.warning("out of border");
		}
		LOGGER.info("agent " + id + " score: " + this.score + " at "
				+ this.getX() + " " + this.getY());
		LOGGER.info("runtime: "
				+ (int) ((System.nanoTime() - startTime) / 1000000));
		return this.handleObjects();
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

	/**
	 * handle object at current location
	 * 
	 * @return action to act
	 */
	private TWThought handleObjects() {
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

	/**
	 * check fuel level and try to get a path back to fuel station when fuel is
	 * low
	 * 
	 * @return direction for next movement
	 */
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
				LOGGER.info("	generating path from: " + this.getX() + " "
						+ this.getY() + " to  fuel station");
				TWPath lpath = pathGenerator.findPath(this.getX(), this.getY(),
						this.getEnvironment().getFuelingStation().getX(), this
								.getEnvironment().getFuelingStation().getY());
				if (lpath == null) {
					lpath = pathGenerator.findPath(
							this.getX(),
							this.getY(),
							java.lang.Math.max(tf.getX(), this.getX()
									- this.sensor.sensorRange),
							java.lang.Math.max(tf.getY(), this.getY()
									- this.sensor.sensorRange));
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
				}
				if (lpath != null && lpath.hasNext()) {
					this.path = lpath;
					this.pathFor = PathFor.FUEL;
					dir = path.popNext().getDirection();
				} else {
					if (fuelCritical()) {
						LOGGER.info("	cannot find a path, waiting");
						dir = TWDirection.Z;
					} else {
						dir = this.lookForObjects();
					}
				}
			}
		} else {
			dir = this.lookForObjects();
		}
		return dir;
	}

	/**
	 * look for tiles and holes in the neighborhood
	 * 
	 * @return direction for next movement
	 */
	private TWDirection lookForObjects() {
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

	/**
	 * move the agent according to the patrol path
	 * 
	 * @return direction for next movement
	 */
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

	/**
	 * move randomly
	 * 
	 * @return direction for next movement
	 */
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

	/**
	 * set patrol points for the agent
	 */
	private void setPatrol() {
		TWEnvironment env = this.getEnvironment();
		int dev = this.sensor.sensorRange, xmax = env.getxDimension(), ymax = env
				.getyDimension();

		PatrolPoint p1 = new PatrolPoint(dev, ymax - dev);
		PatrolPoint p2 = new PatrolPoint(xmax - 3 * dev, ymax - dev);
		PatrolPoint p3 = new PatrolPoint(dev, dev);

		PatrolPoint pp1 = new PatrolPoint(xmax - dev, dev);
		PatrolPoint pp2 = new PatrolPoint(xmax - dev, ymax - dev);
		PatrolPoint pp3 = new PatrolPoint(3 * dev, dev);

		if (id % 2 == 1) {
			this.pp = new PatrolPath(p3, p1, p3, p2);
			this.pp.autoPath(3, Shape.CUSTOM);
		} else {
			this.pp = new PatrolPath(pp3, pp1, pp3, pp2);
			this.pp.autoPath(3, Shape.CUSTOM);
		}
		for (PatrolPoint p : pp.getPps()) {
			LOGGER.warning(p.getX() + " " + p.getY());
		}
		this.nextp = pp.nextPoint();
	}

	/**
	 * get random location in own half of the world
	 * 
	 * @return
	 */
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

	/**
	 * get random direction for next movement
	 * 
	 * @return one of the directions N, W, E, S
	 */
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
		return randomDir;
	}

	/**
	 * check whether at fuel station
	 * 
	 * @return true or false
	 */
	private boolean atFuelStation() {
		return this.getX() == this.getEnvironment().getFuelingStation().getX()
				&& this.getY() == this.getEnvironment().getFuelingStation()
						.getY();
	}

	/**
	 * check whether fuel is low
	 * @return true or false
	 */
	private boolean fuelLow() {
		return this.getFuelLevel() <= this.getX() + this.getY() + FUEL_LOW;
	}
	
	/**
	 * check whether fuel is critical
	 * @return true or false
	 */
	private boolean fuelCritical() {
		return this.getFuelLevel() <= this.getX() + this.getY() + FUEL_CRITICAL;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Reactive Agent " + this.id;
	}
}
