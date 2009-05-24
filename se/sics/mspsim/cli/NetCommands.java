/**
 * 
 */
package se.sics.mspsim.cli;

import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.net.CC2420PacketHandler;
import se.sics.mspsim.net.IEEE802154Handler;
import se.sics.mspsim.net.IPStack;
import se.sics.mspsim.net.LoWPANHandler;
import se.sics.mspsim.util.ComponentRegistry;
import se.sics.mspsim.util.Utils;

/**
 * @author joakim
 *
 */
public class NetCommands implements CommandBundle {

  private IPStack ipStack;
  public void setupCommands(final ComponentRegistry registry, CommandHandler handler) {

    handler.registerCommand("ipstack", new BasicLineCommand("setup 802.15.4/IP stack", "") {
      CC2420PacketHandler listener;
      CommandContext context;
      public int executeCommand(CommandContext context) {
        this.context = context;
        MSP430 cpu = (MSP430) registry.getComponent(MSP430.class);
        listener = new CC2420PacketHandler(cpu);
        listener.setOutput(context.out);
        IEEE802154Handler ieeeHandler = new IEEE802154Handler();
        listener.addUpperLayerHandler(0, ieeeHandler);
        ieeeHandler.setLowerLayerHandler(listener);
        ipStack = new IPStack();
        LoWPANHandler lowpanHandler = new LoWPANHandler(ipStack);
        ieeeHandler.addUpperLayerHandler(0, lowpanHandler);
        lowpanHandler.setLowerLayerHandler(ieeeHandler);
        ipStack.setLinkLayerHandler(lowpanHandler);
        context.err.print("IP Stack started");
        return 0;
      }
      
      public void lineRead(String line) {
        if (listener != null) {
          byte[] data = Utils.hexconv(line);
          for (int i = 0; i < data.length; i++) {
            //context.out.println("Byte " + i + " = " + ((int) data[i] & 0xff));
            // Currently it will autoprint when packet is ready...
            listener.receivedByte(data[i]);
          }
        }
      }
    });

    handler.registerCommand("tspstart", new BasicCommand("starts a TSP tunnel", "<server> <user> <password>") {
      public int executeCommand(CommandContext context) {
        if (ipStack.startTSPTunnel(context.getArgument(0),
            context.getArgument(1), context.getArgument(2))) {
          context.out.print("TSP Tunnel started");
          return 0;
        } else {
          context.out.print("TSP Tunnel failed");
          return 1;
        }
      }
    });
  }
}
