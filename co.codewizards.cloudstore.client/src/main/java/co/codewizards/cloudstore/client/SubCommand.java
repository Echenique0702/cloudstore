package co.codewizards.cloudstore.client;

import java.io.Console;

/**
 * <p>
 * Sub-command for a certain CLI operation.
 * <p>
 * The CloudStore-command-line-interface uses a syntax similar to the svn command and the logic of the
 * command 'java -jar co.codewizards.cloudstore.client-VERSION.jar SUBCOMMAND -arg1 val1 -arg2 val2 ...'
 * is thus actually implemented by a class extending this class and {@link #getSubCommandName() registering}
 * for a certain 'SUBCOMMAND'.
 * <p>
 * Every subclass of this class can declare its arguments using annotations like {@link Option}.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public abstract class SubCommand
{
	private String subCommandName;

	/**
	 * Get the name of the sub-command, i.e. what the user has to write in the command line.
	 * @return the name of the sub-command.
	 */
	public String getSubCommandName() {
		if (subCommandName == null) {
			final String suffix = "SubCommand";
			String simpleName = this.getClass().getSimpleName();
			if (!simpleName.endsWith(suffix))
				throw new IllegalStateException(
						String.format("Class name '%s' does not end with suffix '%s'! Rename the class or override the 'getSubCommand()' method!",
								simpleName, suffix));

			StringBuilder sb = new StringBuilder();
			sb.append(simpleName.substring(0, simpleName.length() - suffix.length()));
			if (sb.length() == 0)
				throw new IllegalStateException(
						String.format("Class name '%s' equals suffix '%s'! There should be sth. before the suffix! Rename the class or override the 'getSubCommand()' method!",
								simpleName, suffix));

			sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
			subCommandName = sb.toString();
		}
		return subCommandName;
	}

	/**
	 * Get the description for this sub-command.
	 * @return the description.
	 */
	public abstract String getSubCommandDescription();

	public void prepare()
	throws Exception
	{
	}

	public abstract void run()
	throws Exception;

	protected String promptPassword(String fmt, Object ... args) {
		Console console = System.console();
		if (console == null)
			throw new IllegalStateException("There is no system console! Cannot prompt \"" + String.format(fmt, args) + "\"!!!");

		char[] pw = console.readPassword(fmt, args);
		if (pw == null)
			return null;
		else
			return new String(pw);
	}

	protected String prompt(String fmt, Object ... args) {
		Console console = System.console();
		if (console == null)
			throw new IllegalStateException("There is no system console! Cannot prompt \"" + String.format(fmt, args) + "\"!!!");

		String result = console.readLine(fmt, args);
		return result;
	}

	public boolean isVisibleInHelp() {
		return true;
	}
}
