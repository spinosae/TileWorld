package tileworld.agent;

import sim.field.grid.ObjectGrid2D;
import sim.util.Int2D;
import tileworld.Parameters;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEntity;
import tileworld.environment.TWEnvironment;
import tileworld.environment.TWFuelStation;
import tileworld.environment.TWHole;
import tileworld.environment.TWTile;
import tileworld.exceptions.CellBlockedException;
import tileworld.planners.AstarPathGenerator;
import tileworld.planners.PatrolPath;
import tileworld.planners.PatrolPoint;
import tileworld.planners.TWPath;
import tileworld.planners.TWPathGenerator;
import tileworld.planners.TWPathStep;

import java.lang.Math;
import java.util.NoSuchElementException;
import java.util.Random;

public class ReactiveAgent extends TWAgent {
	private TWPathGenerator pathGenerator = new AstarPathGenerator(
			this.getEnvironment(), this, 100);
	private TWPath path = null;
	private final int id;
	private Random randomGenerator = new Random();
	private PatrolPath pp;
	private PatrolPoint nextp;

	public enum PathFor {
		FUEL, TILE, HOLE, NONE, RANDOM, PATROL
	}

	private PathFor pathFor = PathFor.NONE;

	public ReactiveAgent(int xpos, int ypos, int id, TWEnvironment env,
			double fuelLevel) {
		super(xpos, ypos, env, fuelLevel);
		this.id = id;
		if (id == 1) {
			pp = new PatrolPath(new PatrolPoint(Parameters.defaultSensorRange,
					env.getyDimension() - Parameters.defaultSensorRange));
		} else {
			pp = new PatrolPath(new PatrolPoint(env.getxDimension()
					- Parameters.defaultSensorRange,
					Parameters.defaultSensorRange));
		}
		pp.addPoint(new PatrolPoint(env.getxDimension()
				- Parameters.defaultSensorRange, env.getyDimension()
				- Parameters.defaultSensorRange));
		pp.addPoint(new PatrolPoint(Parameters.defaultSensorRange,
				Parameters.defaultSensorRange));
		this.nextp = pp.nextPoint();
	}

	@Override
	protected TWThought think() {
		// TODO Auto-generated method stub
		// TWAction act;
		// TWDirection direc;
		// int xpos = this.getX();
		// int ypos = this.getY();
		return this.react();
	}

	@Override
	protected void act(TWThought thought) {
		// TODO Auto-generated method stub
		// System.out.println(thought.getAction());
		// System.out.println(thought.getDirection());
		TWEntity e = (TWEntity) this.getEnvironment().getObjectGrid()
				.get(this.getX(), this.getY());
		switch (thought.getAction()) {
		case PICKUP:
			try {
				this.pickUpTile((TWTile) e);
			} catch (Exception ex) {
			}
			this.getMemory().removeAgentPercept(this.getX(), this.getY());
			this.path = null;
			this.pathFor = PathFor.NONE;
			break;
		case REFUEL:
			try {
				this.refuel();
			} catch (Exception ex) {
			}
			this.path = null;
			this.pathFor = PathFor.NONE;
			break;
		case PUTDOWN:
			try {
				this.putTileInHole((TWHole) e);
			} catch (Exception ex) {

			}
			this.getMemory().removeAgentPercept(this.getX(), this.getY());
			this.path = null;
			this.pathFor = PathFor.NONE;
			break;
		case MOVE:
			try {
				this.move(thought.getDirection());
			} catch (CellBlockedException e1) {
				// TODO Auto-generated catch block
				// e1.printStackTrace();
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
		return this.getFuelLevel() <= this.getX() + this.getY() + 20;
	}

	private TWThought react() {
		TWAction act;
		TWDirection dir = null;

		ObjectGrid2D objectGrid = this.getMemory().getMemoryGrid();
		TWEntity e = (TWEntity) objectGrid.get(this.getX(), this.getY());
		System.out.println("object on current location: " + e);
		System.out.println("at fuel station:" + this.atFuelStation());
		if (e != null && (e instanceof TWTile)) {
			act = TWAction.PICKUP;
			System.out.println("PICKUP");
		} else if (e != null && (e instanceof TWHole) && this.hasTile()) {
			act = TWAction.PUTDOWN;
			System.out.println("PUTDOWN");
		} else if (this.atFuelStation() && this.fuelLow()) {
			act = TWAction.REFUEL;
			System.out.println("REFUEL");
		} else {
			System.out.println("MOVE");
			act = TWAction.MOVE;
			dir = this.toFuel();
		}
		return new TWThought(act, dir);
	}

	private TWDirection toFuel() {
		TWDirection dir = null;
		if (this.fuelLow()) {
			System.out.println("To refuel");
			if (this.path != null && this.path.hasNext()
					&& pathFor == PathFor.FUEL) {
				System.out.println("current path: " + this.path);
				try {
					dir = path.popNext().getDirection();
				} catch (NoSuchElementException e) {
					System.out.println("Path exception");
				}
			} else {
				TWFuelStation tf = this.getEnvironment().getFuelingStation();
				System.out.println("Generating path from: "
						+ this.getX()
						+ " "
						+ this.getY()
						+ " to "
						+ java.lang.Math.max(tf.getX(), this.getX()
								- Parameters.defaultSensorRange)
						+ " "
						+ java.lang.Math.max(tf.getY(), this.getY()
								- Parameters.defaultSensorRange));
				this.path = pathGenerator.findPath(
						this.getX(),
						this.getY(),
						java.lang.Math.max(tf.getX(), this.getX()
								- Parameters.defaultSensorRange),
						java.lang.Math.max(tf.getY(), this.getY()
								- Parameters.defaultSensorRange));
				this.pathFor = PathFor.FUEL;
				System.out.println("Path: " + path);
				if (this.path != null) {
					dir = path.popNext().getDirection();
				} else {
					dir = TWDirection.Z;
				}
			}
		} else {
			dir = this.find();
		}
		return dir;
	}

	private TWDirection find() {
		TWEntity goal = null;
		TWDirection dir = null;
		if (this.path != null && this.path.hasNext()
				&& (pathFor == PathFor.TILE || pathFor == PathFor.HOLE)) {
			System.out.println("current path: " + this.path + " " + pathFor);
			try {
				dir = path.popNext().getDirection();
			} catch (NoSuchElementException e) {
				System.out.println("Path exception");
			}

		} else {
			if (!this.hasTile()) {
				System.out.println("look for tile");
				goal = this.getMemory().getNearbyTile(this.getX(), this.getY(),
						20);
				pathFor = PathFor.TILE;
			} else {
				System.out.println("look for hole");
				goal = this.getMemory().getNearbyHole(this.getX(), this.getY(),
						20);
				pathFor = PathFor.HOLE;
			}
			if (goal != null) {
				System.out.println("Generating path from: " + this.getX() + " "
						+ this.getY() + " to " + goal.getX() + " "
						+ goal.getY());
				this.path = pathGenerator.findPath(this.getX(), this.getY(),
						goal.getX(), goal.getY());
				System.out.println("Path: " + path);
				if (this.path != null && this.path.hasNext()) {
					dir = path.popNext().getDirection();
				} else {
					 dir = this.wander();
//					dir = this.patrol();
				}
			} else {
				 dir = this.wander();
//				dir = this.patrol();
			}
		}
		return dir;
	}

	private TWDirection wander() {
		Int2D goal = null;
		TWDirection dir = null;
		// TWDirection dir = this.getRandomDirection();
		System.out.println("Wandering");
		if (this.path != null && pathFor == PathFor.RANDOM) {
			dir = path.popNext().getDirection();
		} else {
			goal = this.getRandomLocation();
			this.pathFor = PathFor.RANDOM;
			this.path = pathGenerator.findPath(this.getX(), this.getY(),
					goal.getX(), goal.getY());
			if (this.path != null) {
				dir = path.popNext().getDirection();
			} else {
				dir = this.getRandomDirection();
				while (this.getMemory().isCellBlocked(this.getX() + dir.dx,
						this.getY() + dir.dy)) {
					System.out.println("Wandering");
					dir = this.getRandomDirection();
				}
			}
		}
		return dir;
	}

	private TWDirection patrol() {
		TWDirection dir = null;
		// TWDirection dir = this.getRandomDirection();
		System.out.println("Patroling");
		if (java.lang.Math.abs(this.getX()-nextp.getX()) < 3 && java.lang.Math.abs(this.getY()-nextp.getY()) < 3){
			nextp = this.pp.nextPoint();
		}
		if (this.path != null && pathFor == PathFor.PATROL) {
			dir = path.popNext().getDirection();
		} else {
			
//			nextp = this.pp.getPps().get(1);
			this.pathFor = PathFor.PATROL;
			this.path = pathGenerator.findPath(this.getX(), this.getY(),
					nextp.getX(), nextp.getY());
			if (this.path != null) {
				dir = path.popNext().getDirection();
			} else {
				dir = this.getRandomDirection();
				while (this.getMemory().isCellBlocked(this.getX() + dir.dx,
						this.getY() + dir.dy)) {
					System.out.println("Wandering");
					dir = this.getRandomDirection();
				}
			}
		}
		return dir;
	}

	// private TWDirection explorer() {
	// TWDirection dir = this.find();
	// return dir;
	// }

	private TWDirection getRandomDirection() {

		TWDirection randomDir = TWDirection.values()[this.getEnvironment().random
				.nextInt(5)];

		if (this.getX() >= this.getEnvironment().getxDimension() - 1) {
			randomDir = TWDirection.W;
		} else if (this.getX() <= 0) {
			randomDir = TWDirection.E;
		} else if (this.getY() <= 0) {
			randomDir = TWDirection.S;
		} else if (this.getY() >= this.getEnvironment().getxDimension() - 1) {
			randomDir = TWDirection.N;
		}

		return randomDir;

	}

	private Int2D getRandomLocation() {
		int gx = 1, gy = 1;
		int xthres = (int) (0.5 * this.getEnvironment().getxDimension());

		gx = xthres
				+ randomGenerator.nextInt(this.getEnvironment().getxDimension()
						- xthres);
		// for (int i=1; i<10; i++){
		// System.out.println(randomGenerator.nextInt())
		//
		// }
		int ythres = (int) ((double) gx / this.getEnvironment().getxDimension() * this
				.getEnvironment().getyDimension());
		if (id % 2 == 1) {
			gy = randomGenerator.nextInt(this.getEnvironment().getyDimension()
					- ythres);
		} else {
			gy = ythres
					+ randomGenerator.nextInt(this.getEnvironment()
							.getyDimension() - ythres);
		}
		return new Int2D(gx, gy);
		// return
		// this.memory.getMemoryGrid().get(this.getEnvironment().random.nextInt(),
		// this.getEnvironment().random.nextInt());
	}
}
