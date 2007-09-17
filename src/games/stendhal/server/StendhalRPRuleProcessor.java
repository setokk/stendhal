/* $Id$ */
/***************************************************************************
 *                      (C) Copyright 2003 - Marauroa                      *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.server;

import games.stendhal.common.Debug;
import marauroa.common.Pair;
import games.stendhal.server.actions.ActionListener;
import games.stendhal.server.actions.AdministrationAction;
import games.stendhal.server.actions.AttackAction;
import games.stendhal.server.actions.AwayAction;
import games.stendhal.server.actions.BuddyAction;
import games.stendhal.server.actions.ChatAction;
import games.stendhal.server.actions.CreateGuildAction;
import games.stendhal.server.actions.DisplaceAction;
import games.stendhal.server.actions.FaceAction;
import games.stendhal.server.actions.LookAction;
import games.stendhal.server.actions.MoveAction;
import games.stendhal.server.actions.OutfitAction;
import games.stendhal.server.actions.OwnAction;
import games.stendhal.server.actions.PlayersQuery;
import games.stendhal.server.actions.QuestListAction;
import games.stendhal.server.actions.StopAction;
import games.stendhal.server.actions.UseAction;
import games.stendhal.server.actions.equip.EquipmentAction;
import games.stendhal.server.entity.Entity;
import games.stendhal.server.entity.Outfit;
import games.stendhal.server.entity.RPEntity;
import games.stendhal.server.entity.npc.NPC;
import games.stendhal.server.entity.player.Player;
import games.stendhal.server.entity.spawner.CreatureRespawnPoint;
import games.stendhal.server.entity.spawner.PassiveEntityRespawnPoint;
import games.stendhal.server.events.LoginNotifier;
import games.stendhal.server.events.TurnNotifier;
import games.stendhal.server.events.TutorialNotifier;
import games.stendhal.server.rule.RuleManager;
import games.stendhal.server.rule.RuleSetFactory;
import games.stendhal.server.scripting.ScriptRunner;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import marauroa.common.Configuration;
import marauroa.common.Log4J;
import marauroa.common.Logger;
import marauroa.common.crypto.Hash;
import marauroa.common.game.AccountResult;
import marauroa.common.game.CharacterResult;
import marauroa.common.game.IRPZone;
import marauroa.common.game.RPAction;
import marauroa.common.game.RPObject;
import marauroa.common.game.RPSlot;
import marauroa.common.game.Result;
import marauroa.server.game.Statistics;
import marauroa.server.game.container.PlayerEntry;
import marauroa.server.game.container.PlayerEntryContainer;
import marauroa.server.game.db.DatabaseFactory;
import marauroa.server.game.db.JDBCDatabase;
import marauroa.server.game.db.Transaction;
import marauroa.server.game.rp.IRPRuleProcessor;
import marauroa.server.game.rp.RPServerManager;
import marauroa.test.TestHelper;

public class StendhalRPRuleProcessor implements IRPRuleProcessor {

	/** the logger instance. */
	private static final Logger logger = Log4J.getLogger(StendhalRPRuleProcessor.class);

	/** The Singleton instance */
	protected static StendhalRPRuleProcessor instance;

	private StendhalPlayerDatabase database;

	private static Map<String, ActionListener> actionsMap;
	static {
		actionsMap = new HashMap<String, ActionListener>();
	}

	private RPServerManager rpman;

	/**
	 * A list of all players who are currently logged in.
	 */
	private List<Player> players;

	/**
	 * ???
	 */
	private List<Player> playersRmText;
	private List<Player> playersRmPrivateText;

	private List<NPC> npcs;

	private List<NPC> npcsToAdd;

	private List<NPC> npcsToRemove;

	/**
	 * A list of RPEntities that were killed in the current turn, together with
	 * the Entity that killed it.
	 */
	private List<Pair<RPEntity, Entity>> entityToKill;

	private List<CreatureRespawnPoint> respawnPoints;

	private List<PassiveEntityRespawnPoint> plantGrowers;

	public static void register(String action, ActionListener actionClass) {
		if (actionsMap.get(action) != null) {
			logger.error("Registering twice (previous was "
					+ actionsMap.get(action).getClass()
					+ ") the same action handler: "
					+ action
					+ " with "
					+ actionClass.getClass());
		}
		actionsMap.put(action, actionClass);
	}

	private void registerActions() {
		/*
		 * TODO: Refactor
		 * Autoregister?
		 */ 
		AdministrationAction.register();
		AttackAction.register();
		AwayAction.register();
		BuddyAction.register();
		ChatAction.register();
		DisplaceAction.register();
		EquipmentAction.register();
		FaceAction.register();
		LookAction.register();
		MoveAction.register();
		OutfitAction.register();
		OwnAction.register();
		PlayersQuery.register();
		QuestListAction.register();
		StopAction.register();
		UseAction.register();
		CreateGuildAction.register();

	}

	protected StendhalRPRuleProcessor() {
		players = new LinkedList<Player>();
		playersRmText = new LinkedList<Player>();
		playersRmPrivateText = new LinkedList<Player>();
		npcs = new LinkedList<NPC>();
		respawnPoints = new LinkedList<CreatureRespawnPoint>();
		plantGrowers = new LinkedList<PassiveEntityRespawnPoint>();
		npcsToAdd = new LinkedList<NPC>();
		npcsToRemove = new LinkedList<NPC>();
		entityToKill = new LinkedList<Pair<RPEntity, Entity>>();

	}

	private void init() {
		database = StendhalPlayerDatabase.getDatabase();
		registerActions();
		instance = this;
		addGameEvent("server system", "startup");
	}

	public static StendhalRPRuleProcessor get() {
		if (instance == null) {
			instance = new StendhalRPRuleProcessor();
			instance.init();
		}
		return instance;
	}

	public void addGameEvent(String source, String event, String... params) {
		try {
			Transaction transaction = database.getTransaction();
			database.addGameEvent(transaction, source, event, params);
			transaction.commit();
		} catch (Exception e) {
			logger.warn("Can't store game event", e);
		}
	}

	/**
	 * Gets the points of named player in the specified hall of fame
	 *
	 * @param playername name of the player
	 * @param fametype   type of the hall of fame
	 * @return points     points to add
	 */
	public int getHallOfFamePoints(String playername, String fametype) {
		int res = 0;
		try {
			Transaction transaction = database.getTransaction();
			res = database.getHallOfFamePoints(transaction, playername, fametype);
			transaction.commit();
		} catch (Exception e) {
			logger.warn("Can't store game event", e);
		}
		return res;
	}

	/**
	 * Add points to the named player in the specified hall of fame
	 *
	 * @param playername name of the player
	 * @param fametype   type of the hall of fame
	 * @param points     points to add
	 */
	public void addHallOfFamePoints(String playername, String fametype, int points) {
		try {
			Transaction transaction = database.getTransaction();
			int oldPoints = database.getHallOfFamePoints(transaction, playername, fametype);
			int totalPoints = oldPoints + points;
			database.setHallOfFamePoints(transaction, playername, fametype, totalPoints);
			transaction.commit();
		} catch (Exception e) {
			logger.warn("Can't store game event", e);
		}
	}

	/**
	 *
	 * Set the context where the actions are executed.
	 * Load/Run optional StendhalServerExtension(s) as defined in marauroa.ini file
	 * example:
	 *  groovy=games.stendhal.server.scripting.StendhalGroovyRunner
	 *  myservice=games.stendhal.server.MyService
	 *  server_extension=groovy,myservice
	 * if no server_extension property is found, only the groovy extension is loaded
	 * to surpress loading groovy extension use
	 *  server_extension=
	 * in the properties file.
	 *
	 * @param rpman
	 */
	public void setContext(RPServerManager rpman) {
		try {
			this.rpman = rpman;
			StendhalRPAction.initialize(rpman);
			/* Initialize quests */
			StendhalQuestSystem.get().init();
			
			for (IRPZone zone : StendhalRPWorld.get()) {
				StendhalRPZone szone = (StendhalRPZone) zone;
				npcs.addAll(szone.getNPCList());
				respawnPoints.addAll(szone.getRespawnPointList());
				plantGrowers.addAll(szone.getPlantGrowers());
			}
			new ScriptRunner();

			Configuration config = Configuration.getConfiguration();
			try {
				String[] extensionsToLoad = config.get("server_extension").split(",");
				for (int i = 0; i < extensionsToLoad.length; i++) {
					String extension = null;
					try {
						extension = extensionsToLoad[i];
						if (extension.length() > 0) {
							StendhalServerExtension.getInstance(config.get(extension)).init();
						}
					} catch (Exception ex) {
						logger.error("Error while loading extension: " + extension, ex);
					}
				}
			} catch (Exception ep) {
				logger.info("No server extensions configured in ini file.");
			}

		} catch (Exception e) {
			logger.error("cannot set Context. exiting", e);
			System.exit(-1);
		}
	}

	public boolean checkGameVersion(String game, String version) {
		if (game.equals("stendhal")) {
			return true;
		}
		return false;
	}

	private boolean isValidUsername(String username) {
		/** TODO: Complete this. Should read the list from XML file */
		if (username.indexOf(' ') != -1) {
			return false;
		}
		// TODO: Fix bug [ 1672627 ] 'admin' not allowed in username but GM_ and _GM are
		if (username.toLowerCase().contains("admin")) {
			return false;
		}
		return true;
	}

	public void addNPC(NPC npc) {
		npcsToAdd.add(npc);
	}

	public void killRPEntity(RPEntity entity, Entity killer) {
		entityToKill.add(new Pair<RPEntity, Entity>(entity, killer));
	}

	/**
	 * Checks whether the given RPEntity has been killed this turn.
	 * @param entity The entity to check.
	 * @return true if the given entity has been killed this turn.
	 */
	private boolean wasKilled(RPEntity entity) {
		for (Pair<RPEntity, Entity> entry : entityToKill) {
			if (entity.equals(entry.first())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the entity which has killed the given RPEntity this
	 * turn.
	 * @param entity The killed RPEntity.
	 * @return The killer, or null if the given RPEntity hasn't been
	 *         killed this turn.
	 */
	private Entity killerOf(RPEntity entity) {
		for (Pair<RPEntity, Entity> entry : entityToKill) {
			if (entity.equals(entry.first())) {
				return entry.second();
			}
		}
		return null;
	}

	public void removePlayerText(Player player) {
		playersRmText.add(player);
	}

	public void removePlayerPrivateText(Player player) {
		playersRmPrivateText.add(player);
	}

	/**
	 * Gets all players who are currently online.
	 * @return A list of all online players
	 */
	public List<Player> getPlayers() {
		return players;
	}

	/**
	 * Finds an online player with a specific name.
	 * @param name The player's name
	 * @return The player, or null if no player with the given name is
	 *         currently online.
	 */
	public Player getPlayer(String name) {
		for (Player player : getPlayers()) {
			/*
			 * If player has the title attribute, it replaces name.
			 */
			String playername = player.getName();
			if (player.has("title")) {
				playername = player.get("title");
			}

			if (playername.equals(name)) {
				return player;
			}
		}
		return null;
	}

	public List<PassiveEntityRespawnPoint> getPlantGrowers() {
		return plantGrowers;
	}

	public List<NPC> getNPCs() {
		return npcs;
	}

	public boolean removeNPC(NPC npc) {
		return npcsToRemove.add(npc);
	}

	public boolean onActionAdd(RPObject caster, RPAction action, List<RPAction> actionList) {
		return true;
	}

	public void execute(RPObject caster, RPAction action) {
		try {
			Player player = (Player) caster;
			String type = action.get("type");
			ActionListener actionListener = actionsMap.get(type);
			if (actionListener == null) {
				logger.error("\"" + type + "\" not in " + actionsMap.get(type));
				player.sendPrivateText("Unknown Command " + type);
			} else {
				actionListener.onAction(player, action);
			}
		} catch (Exception e) {
			logger.error("cannot execute action " + action, e);
		}
	}

	public int getTurn() {
		return rpman.getTurn();
	}

	/** Notify it when a new turn happens */
	public synchronized void beginTurn() {
		long start = System.nanoTime();

		/*
		 * Debug statement for inspecting list of things.
		 * Most of our memories leaks came from list keep adding and adding elements.
		 */
		if (Debug.SHOW_LIST_SIZES && rpman.getTurn() % 1000 == 0) {
			int creatures = 0;
			for (CreatureRespawnPoint point : respawnPoints) {
				creatures += point.size();
			}

			int objects = 0;

			for (IRPZone zone : StendhalRPWorld.get()) {
				objects += zone.size();
			}

			StringBuffer os = new StringBuffer();
			os.append("entityToKill: " + entityToKill.size() + "\n");
			os.append("npcs: " + npcs.size() + "\n");
			os.append("npcsToAdd: " + npcsToAdd.size() + "\n");
			os.append("npcsToRemove: " + npcsToRemove.size() + "\n");
			os.append("plantGrowers: " + plantGrowers.size() + "\n");
			os.append("players: " + players.size() + "\n");
			os.append("playersRmText: " + playersRmText.size() + "\n");
			os.append("playersRmPrivateText: " + playersRmPrivateText.size() + "\n");
			os.append("respawnPoints: " + respawnPoints.size() + "\n");
			os.append("creatures: " + creatures + "\n");
			os.append("objects: " + objects + "\n");
			logger.info(os);
		}

		try {
			// We keep the number of players logged.
			Statistics.getStatistics().set("Players logged", players.size());
			
			/*
			 * TODO: Refactor.
			 * Entities should care about really dying themselves.
			 * This is here because there is a split between last hit and the moment a entity die so
			 * that the last hit is visible on client. 
			 */
			// In order for the last hit to be visible dead happens at two
			// steps.
			for (Pair<RPEntity, Entity> entry : entityToKill) {
				try {
					entry.first().onDead(entry.second());
				} catch (Exception e) {
					logger.error("Player has logout before dead", e);
				}
			}
			entityToKill.clear();
			
			/*
			 * TODO: Refactor
			 * NPC should be stored on zones instead of duplicating that info.
			 */
			// Done this way because a problem with comodification... :(
			npcs.removeAll(npcsToRemove);
			npcs.addAll(npcsToAdd);
			npcsToAdd.clear();
			npcsToRemove.clear();

			/*
			 * TODO: Refactor
			 * Use RPEvent that is the correct way and it is handled by marauroa itself.
			 */
			for (Player player : playersRmPrivateText) {
				if (player.has("private_text")) {
					player.remove("private_text");
					player.notifyWorldAboutChanges();
				}
			}

			/*
			 * TODO: Refactor
			 * May be done by the zone itself.
			 */
			for (Player player : players) {
				try {
					player.logic();
				} catch (Exception e) {
					logger.error("Error in player logic", e);
				}
			}

			/*
			 * TODO: Refactor
			 * Definitively must be done by the zone itself.
			 */
			for (NPC npc : npcs) {
				try {
					npc.logic();
				} catch (Exception e) {
					logger.error("Error in npc logic", e);
				}
			}

			/*
			 * TODO: Refactor
			 * Removable once RPEvent for chat is added.
			 */
			for (Player player : playersRmText) {
				if (player.has("text")) {
					player.remove("text");
					player.notifyWorldAboutChanges();
				}
			}
			playersRmText.clear();
			playersRmPrivateText.clear();
		} catch (Exception e) {
			logger.error("error in beginTurn", e);
		} finally {
			logger.debug("Begin turn: " + (System.nanoTime() - start) / 1000000.0);
		}
	}

	public synchronized void endTurn() {
		long start = System.nanoTime();
		int currentTurn = getTurn();
		try {
			/*
			 * TODO: Refactor
			 * Should be done by the zone itself.
			 */
			// Creatures
			for (CreatureRespawnPoint point : respawnPoints) {
				point.logic();
			}
	
			// Registeres classes for this turn
			TurnNotifier.get().logic(currentTurn);

		} catch (Exception e) {
			logger.error("error in endTurn", e);
		} finally {
			logger.debug("End turn: " + (System.nanoTime() - start) / 1000000.0 + " (" + (currentTurn % 5) + ")");
		}
	}

	public synchronized boolean onInit(RPObject object) {
		try {
			PlayerEntry entry = PlayerEntryContainer.getContainer().get(object);

			/*
			 * TODO: Refactor
			 * This is a hack, it should use instead RPObjectFactory.
			 */
			Player player = Player.create(object);
			entry.object = player;

			/*
			 * TODO: Refactor
			 * Removable once RPEvent chat is added.
			 */ 
			playersRmText.add(player);
			playersRmPrivateText.add(player);
			
			players.add(player);

			/*
			 * TODO: Refactor
			 * Hide implementation
			 */ 
			if (!player.isGhost()) {
				// Notify other players about this event
				for (Player p : getPlayers()) {
					p.notifyOnline(player.getName());
				}
			}
			addGameEvent(player.getName(), "login");
			LoginNotifier.get().onPlayerLoggedIn(player);
			TutorialNotifier.login(player);

			database.setOnlineStatus(player, true);

			return true;
		} catch (Exception e) {
			logger.error("There has been a severe problem loading player "
					+ object.get("#db_id"), e);
			return false;
		}
	}

	public synchronized boolean onExit(RPObject object) {
		try {
			Player player = (Player) object;
			if (wasKilled(player)) {
				logger.info("Logged out shortly before death: Killing it now :)");
				player.onDead(killerOf(player));
			}

			/*
			 * TODO: Refactor
			 * Hide implementation.
			 */ 
			// Notify other players about this event
			for (Player p : getPlayers()) {
				p.notifyOffline(player.getName());
			}

			Player.destroy(player);
			players.remove(player);
			
			addGameEvent(player.getName(), "logout");
			logger.debug("removed player " + player);

			return true;
		} catch (Exception e) {
			logger.error("error in onExit", e);
			return true;
		}
	}

	public synchronized void onTimeout(RPObject object) {
		/*
		 * TODO: Refactor
		 *  Check new syntax of onTimeout.
		 *  It is expected to kickout the player, it can't fail.
		 */
		onExit(object);
	}

	public AccountResult createAccount(String username, String password, String email) {
		/*
		 * TODO: Refactor
		 * Invalid patterns for username should be stored in a text file or XML file.
		 */ 
		if (!isValidUsername(username)) {
			return new AccountResult(Result.FAILED_EXCEPTION, username);
		}

		JDBCDatabase database = (JDBCDatabase) DatabaseFactory.getDatabase();
		Transaction trans = database.getTransaction();
		try {
			trans.begin();

			if (database.hasPlayer(trans, username)) {
				logger.warn("Account already exist: " + username);
				return new AccountResult(Result.FAILED_PLAYER_EXISTS, username);
			}

			database.addPlayer(trans, username, Hash.hash(password), email);

			trans.commit();
			return new AccountResult(Result.OK_CREATED, username);
		} catch (SQLException e) {
			try {
				trans.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			TestHelper.fail();
			return new AccountResult(Result.FAILED_EXCEPTION, username);
		}
    }

	public CharacterResult createCharacter(String username, String character, RPObject template) {
		JDBCDatabase database = (JDBCDatabase) DatabaseFactory.getDatabase();
		Transaction trans = database.getTransaction();

		try {
			if (database.hasCharacter(trans, username, character)) {
				logger.warn("Character already exist: " + character);
				return new CharacterResult(Result.FAILED_PLAYER_EXISTS, character, template);
		}

			/*
			 * TODO: Refactor
			 * OMG! Hide in a method.
			 * Even better, move it to player class as it is its duty to provide a empty level 0 player.
			 */ 
			/*
			 * Create the player character object
			 */
			Player object = new Player(new RPObject());
			object.setID(RPObject.INVALID_ID);

			object.put("type", "player");
			object.put("name", character);
			object.put("outfit", new Outfit().getCode());
			object.put("base_hp", 100);
			object.put("hp", 100);
			object.put("atk", 10);
			object.put("atk_xp", 0);
			object.put("def", 10);
			object.put("def_xp", 0);
			object.put("xp", 0);

			/*
			 * TODO: Update the above to use Player and RPEntity methods.
			 */
			object.update();

			RuleManager manager = RuleSetFactory.getRuleSet("default");

			object.addSlot("armor");
			Entity entity = manager.getEntityManager().getItem("leather_armor");
			RPSlot slot = object.getSlot("armor");
			slot.add(entity);

			object.addSlot("rhand");
			entity = manager.getEntityManager().getItem("club");
			slot = object.getSlot("rhand");
			slot.add(entity);

			/*
			 * Finally we add it to database.
			 */
			database.addCharacter(trans, username, character, object);
			return new CharacterResult(Result.OK_CREATED, character, object);
		} catch (Exception e) {
			try {
				trans.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			logger.error("Can't create character", e);
			TestHelper.fail();
			return new CharacterResult(Result.FAILED_EXCEPTION, character, template);
		}
    }

	public RPServerManager getRPManager() {
	    return rpman;
	}
}
