package tileworld.agent;

import sim.field.grid.ObjectGrid2D;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEntity;
import tileworld.environment.TWEnvironment;
import tileworld.environment.TWFuelStation;
import tileworld.environment.TWHole;
import tileworld.environment.TWTile;
import tileworld.exceptions.CellBlockedException;
import tileworld.planners.AstarPathGenerator;
import tileworld.planners.TWPath;
import tileworld.planners.TWPathGenerator;

public class ReactiveAgent extends TWAgent {
	private TWPathGenerator pathGenerator = new AstarPathGenerator(
			this.getEnvironment(), this, 100);
	private TWPath path;

	public ReactiveAgent(int xpos, int ypos, TWEnvironment env, double fuelLevel) {
		super(xpos, ypos, env, fuelLevel);
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
			break;
		case REFUEL:
			try {
				this.refuel();
			} catch (Exception ex) {
			}
			break;
		case PUTDOWN:
			try {
				this.putTileInHole((TWHole) e);
			} catch (Exception ex) {

			}
			this.getMemory().removeAgentPercept(this.getX(), this.getY());
			break;
		case MOVE:
			try {
				this.move(thought.getDirection());
			} catch (CellBlockedException e1) {
				// TODO Auto-generated catch block
				// e1.printStackTrace();
			}
			this.getMemory().removeAgentPercept(this.getX(),this.getY());
			break;
		default:
			break;
		}
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Reactive Agent";
	}

	private boolean atFuelStation() {
		return this.getX() == this.getEnvironment().getFuelingStation().getX()
				&& this.getY() == this.getEnvironment().getFuelingStation()
						.getY();
	}

	private boolean fuelLow() {
		return this.getFuelLevel() <= 200;
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
			TWFuelStation tf = this.getEnvironment().getFuelingStation();
			System.out.println("Generating path from: " + this.getX()+" "+ this.getY()+" to "
					+ tf.getX()+" "+ tf.getY());
			this.path = pathGenerator.findPath(this.getX(), this.getY(),
					tf.getX(), tf.getY());
			System.out.println("Path: " + path);
			if (this.path != null) {
				dir = path.popNext().getDirection();
			} else {
				dir = this.find();
			}
		} else {
			dir = this.find();
		}
		return dir;
	}

	private TWDirection find() {
		TWEntity goal = null;
		TWDirection dir = null;
		if (!this.hasTile()) {
			System.out.println("look for tile");
			goal = this.getMemory().getNearbyTile(this.getX(), this.getY(), 20);
		} else {
			System.out.println("look for hole");
			goal = this.getMemory().getNearbyHole(this.getX(), this.getY(), 20);
		}
		if (goal != null) {
			System.out.println("Generating path from: " + this.getX()+" "+ this.getY()+" to "
					+ goal.getX()+" "+ goal.getY());
			this.path = pathGenerator.findPath(this.getX(), this.getY(),
					goal.getX(), goal.getY());
			System.out.println("Path: " + path);
			if (this.path != null && this.path.hasNext()) {
				dir = path.popNext().getDirection();
			} else {
				dir = this.wander();
			}
		} else {
			dir = this.wander();
		}
		return dir;
	}

	private TWDirection wander() {
		TWDirection dir = this.getRandomDirection();
		System.out.println("Wandering");
		while (this.getMemory().isCellBlocked(this.getX() + dir.dx,
				this.getY() + dir.dy)) {
			System.out.println("Wandering");
			dir = this.getRandomDirection();
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

}
