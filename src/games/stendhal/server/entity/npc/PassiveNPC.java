/***************************************************************************
 *                   (C) Copyright 2003-2013 - Stendhal                    *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.server.entity.npc;

import org.apache.log4j.Logger;

/**
 * A stripped down SpeakerNPC that does not interact with players
 * 
 * @author AntumDeluge
 *
 */
public class PassiveNPC extends NPC {
	/** the logger instance. */
	private static final Logger logger = Logger.getLogger(PassiveNPC.class);
	
    protected int pauseValue = 0;
	protected int pauseProbability = 0;
	
	/**
	 * Creates a new PassiveNPC.
	 *
	 */
	public PassiveNPC() {
		baseSpeed = 0.2;
		createPath();
		
		put("title_type", "npc");
		
		// Entity name is not drawn because of "unnamed" attribute
        setName("PassiveNPC");
		put("unnamed", "");
		
		// Health bar drawing is supressed
		put("no_hpbar", "");
		
		setSize(1, 1);
		
		updateModifiedAttributes();
	}
	
	protected void createPath() {
		// sub classes can implement this method
	}

	@Override
	public void logic() {
	    super.logic();
	    
		if (hasPath()) {
			setSpeed(getBaseSpeed());
		}
		
		applyMovement();
	}
	
	@Override
	public void onFinishedPath() {
	    super.onFinishedPath();
	    
	    if (isMovingEntity() && usesRandomPath()) {
	        // FIXME: There is a pause when renewing path
            setRandomPathFrom(getX(), getY(), getMovementRange() / 2);
	    }
	}
	
	public void setPathCompletePause(int pause, int probability) {
        this.pauseValue = pause;
	    this.pauseProbability = probability;
	}
}
