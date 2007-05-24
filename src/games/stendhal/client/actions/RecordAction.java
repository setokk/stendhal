package games.stendhal.client.actions;

import games.stendhal.client.scripting.ScriptRecorder;

import java.io.FileNotFoundException;

import marauroa.common.Logger;

/**
 * Enable/disable input recording.
 */
public class RecordAction implements SlashAction  {

	private static Logger logger = Logger.getLogger(RecordAction.class);

	private ScriptRecorder recorder = null;

	/**
	 * Execute a chat command.
	 *
	 * @param	params		The formal parameters.
	 * @param	remainder	Line content after parameters.
	 *
	 * @return	<code>true</code> if  was handled.
	 */
	public boolean execute(String[] params, String remainder) {
		if (recorder != null) {
			recorder.end();
			recorder = null;
		}

		String name = params[0];

		if (!name.equals("stop")) {
			try {
				recorder = new ScriptRecorder(name);
				recorder.start();
			} catch (FileNotFoundException e) {
				logger.error(e, e);
			}
		}

		return true;
	}

	/**
	 * Get the maximum number of formal parameters.
	 *
	 * @return	The parameter count.
	 */
	public int getMaximumParameters() {
		return 1;
	}

	/**
	 * Get the minimum number of formal parameters.
	 *
	 * @return	The parameter count.
	 */
	public int getMinimumParameters() {
		return 1;
	}

	/**
	 * get the script recorder
	 *
	 * @return ScriptRecorder
	 */
	public ScriptRecorder getRecorder() {
		return recorder;
	}
}