package se.sics.mspsim.cli;


public abstract class Command implements Cloneable {
  public abstract int executeCommand(CommandContext context);
  public abstract String getCommandHelp(CommandContext context);
  public abstract String getArgumentHelp(CommandContext context);

  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }
}
