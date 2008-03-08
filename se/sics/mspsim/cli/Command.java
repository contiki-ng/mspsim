package se.sics.mspsim.cli;


public interface Command {
  public int executeCommand(CommandContext context);
  public String getCommandHelp(CommandContext context);
  public String getArgumentHelp(CommandContext context);
}
